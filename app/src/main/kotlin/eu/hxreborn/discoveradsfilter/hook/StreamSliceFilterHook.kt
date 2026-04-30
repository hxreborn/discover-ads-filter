package eu.hxreborn.discoveradsfilter.hook

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import eu.hxreborn.discoveradsfilter.discovery.DexKitResolver
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.Safe
import io.github.libxposed.api.XposedInterface
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object StreamSliceFilterHook {
    private const val TAG = "DiscoverAdsFilter/StreamSlice"
    private const val TARGET_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val CONTENT_ID_FIELD = "f122746b"

    private val adClusterTokens = setOf("feedads")

    private val decisionCache = ConcurrentHashMap<String, Boolean>()
    private val countedAdKeys = ConcurrentHashMap.newKeySet<String>()

    private val contentIdFieldCache = ConcurrentHashMap<Class<*>, Field>()
    private val noContentIdClasses = ConcurrentHashMap.newKeySet<Class<*>>()
    private val stringFieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()
    private val nonSliceClasses = ConcurrentHashMap.newKeySet<Class<*>>()

    @Volatile
    private var resolvedStreamMethod: MethodRef? = null

    @Volatile
    private var fallbackTargets: ResolvedTargets.Resolved? = null

    @Volatile
    private var lastFingerprint: Long = Long.MIN_VALUE

    @Volatile
    private var lastFilteredSnapshot: List<Any?>? = null

    @Volatile
    private var hookPrefs: SharedPreferences? = null

    fun install(
        loader: ClassLoader,
        prefs: SharedPreferences,
        targets: ResolvedTargets,
        processName: String,
    ) = Safe.run(TAG, "install") {
        hookPrefs = prefs
        val streamRef = resolveStreamMethod(targets, processName)
        if (streamRef == null) {
            Logger.w("stream method not resolved; skipping")
            return@run
        }

        val method =
            runCatching { streamRef.resolve(loader) }.getOrElse {
                Logger.w("failed to resolve $streamRef")
                return@run
            }

        module.hook(method).intercept(StreamListHook())
        Logger.i("hooked $streamRef")
    }

    private fun resolveStreamMethod(
        targets: ResolvedTargets,
        processName: String,
    ): MethodRef? {
        (targets as? ResolvedTargets.Resolved)?.streamRenderableListMethod?.let { return it }

        resolvedStreamMethod?.let { return it }

        val resolved = resolveFallbackTargets(processName) ?: return null
        val ref = resolved.streamRenderableListMethod ?: return null
        resolvedStreamMethod = ref
        Logger.i("DexKit fallback resolved $ref")
        return ref
    }

    private fun resolveFallbackTargets(processName: String): ResolvedTargets.Resolved? {
        fallbackTargets?.let { return it }
        if ("googleapp" !in processName && "search" !in processName) {
            return null
        }
        val apk = currentPackageSourceDir() ?: return null
        val resolved =
            runCatching {
                runBlocking { DexKitResolver.resolveAll(apk) }
            }.getOrNull() as? ResolvedTargets.Resolved
        if (resolved != null) fallbackTargets = resolved
        return resolved
    }

    @Suppress("PrivateApi")
    private fun currentPackageSourceDir(): String? {
        val app =
            runCatching {
                Class
                    .forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null) as? Application
            }.getOrNull()
        return app?.applicationInfo?.sourceDir?.takeIf { it.isNotBlank() } ?: runCatching {
            val at =
                Class
                    .forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread")
                    .invoke(null) ?: return@runCatching null
            val ctx =
                at.javaClass.getMethod("getSystemContext").invoke(at) as? Context
                    ?: return@runCatching null
            ctx.packageManager.getApplicationInfo(TARGET_PACKAGE, 0).sourceDir
        }.getOrNull()
    }

    @Volatile
    private var keysDumped = false

    private fun dumpKeysOnce(items: List<*>) {
        if (keysDumped) return
        keysDumped = true
        Logger.v {
            items
                .mapIndexed { i, item ->
                    val cls = item?.javaClass?.simpleName ?: "null"
                    val key = item?.let(::stableItemKey) ?: "<no-key>"
                    "  [$i] $cls → $key"
                }.joinToString("\n", prefix = "item key dump (${items.size} items):\n")
        }
    }

    private fun buildFilteredList(items: List<*>): List<Any?>? {
        var removed = 0
        var newAds = 0
        val filtered = ArrayList<Any?>(items.size)
        for (item in items) {
            if (item == null) {
                filtered += null
                continue
            }
            val key = stableItemKey(item)
            if (key != null && isAdItem(key)) {
                removed++
                if (countedAdKeys.add(key)) {
                    newAds++
                    Logger.v { "blocked ad key=$key" }
                }
            } else {
                filtered += item
            }
        }
        if (removed == 0) return null
        if (newAds > 0) HookMetrics.addAdsHidden(newAds)
        return filtered
    }

    private class StreamListHook : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val result = chain.proceed()
            val items = result as? List<*> ?: return result
            if (items.isEmpty()) return result

            val prefs = hookPrefs
            if (prefs != null && !SettingsPrefs.filterEnabled.read(prefs)) return result

            val fp = fastFingerprint(items)
            if (fp == lastFingerprint) return lastFilteredSnapshot ?: result

            dumpKeysOnce(items)
            lastFingerprint = fp

            val filtered = buildFilteredList(items)
            lastFilteredSnapshot = filtered
            return filtered ?: result
        }
    }

    private fun isAdItem(key: String): Boolean {
        decisionCache[key]?.let { return it }
        val lower = key.lowercase(Locale.ROOT)
        val isAd = adClusterTokens.any { it in lower }
        decisionCache[key] = isAd
        return isAd
    }

    private fun stableItemKey(item: Any): String? {
        val cls = item.javaClass

        // Fast path: known non-slice class
        if (cls in nonSliceClasses) return null

        contentId(item, cls)?.let { return it }

        // Fallback: first non-blank String field
        val fields = stringFieldsCache.computeIfAbsent(cls) { resolveStringFields(cls) }
        for (f in fields) {
            val value =
                try {
                    f.get(item) as? String
                } catch (_: Exception) {
                    null
                }
            if (!value.isNullOrBlank()) {
                return "${cls.simpleName}#$value"
            }
        }

        // No usable key — remember this class
        nonSliceClasses.add(cls)
        return null
    }

    private fun contentId(
        item: Any,
        cls: Class<*>,
    ): String? {
        if (cls in noContentIdClasses) return null

        val field =
            contentIdFieldCache[cls] ?: run {
                val found = findContentIdField(cls)
                if (found == null) {
                    noContentIdClasses.add(cls)
                    return null
                }
                contentIdFieldCache[cls] = found
                found
            }

        val value =
            try {
                field.get(item) as? String
            } catch (_: Exception) {
                null
            }
        return value?.takeIf { it.isNotBlank() }
    }

    private fun findContentIdField(start: Class<*>): Field? {
        var c: Class<*>? = start
        while (c != null && c != Any::class.java) {
            try {
                val f = c.getDeclaredField(CONTENT_ID_FIELD)
                f.isAccessible = true
                return f
            } catch (_: NoSuchFieldException) {
                // expected — walk up
            }
            c = c.superclass
        }
        return null
    }

    private fun collectAccessibleFields(
        cls: Class<*>,
        dest: MutableList<Field>,
    ) {
        for (f in cls.declaredFields) {
            if (Modifier.isStatic(f.modifiers) || f.isSynthetic) continue
            try {
                f.isAccessible = true
            } catch (_: Exception) {
                continue
            }
            dest.add(f)
        }
    }

    private fun resolveStringFields(cls: Class<*>): List<Field> =
        buildList {
            var c: Class<*>? = cls
            while (c != null && c != Any::class.java) {
                collectAccessibleFields(c, this)
                c = c.superclass
            }
        }

    private fun fastFingerprint(items: List<*>): Long {
        var hash = items.size.toLong()
        val step = (items.size / 3).coerceAtLeast(1)
        var i = 0
        var n = 0
        while (i < items.size && n < 4) {
            hash = hash * 31L + System.identityHashCode(items[i]).toLong()
            i += step
            n++
        }
        return hash
    }
}
