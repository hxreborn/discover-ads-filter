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
    private const val NULL_KEY_PREFIX = "__null__#"

    // DEBUG: "homestack" temporarily included so testing isn't gated on a real ad appearing.
    // Revert to setOf("feedads") before shipping.
    private val adClusterTokens = setOf("feedads", "homestack")

    private val decisionCache = ConcurrentHashMap<String, Boolean>()
    private val fieldCache = ConcurrentHashMap<String, Field>()
    private val classFieldCache = ConcurrentHashMap<String, List<Field>>()

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
        // only the main feed processes carry the Discover RecyclerView
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
        if (app?.applicationInfo?.sourceDir?.isNotBlank() == true) {
            return app.applicationInfo.sourceDir
        }
        return runCatching {
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

    private fun classCacheKey(c: Class<*>): String {
        val id = Integer.toHexString(System.identityHashCode(c))
        return "${c.simpleName}@$id"
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

            val keys =
                items.mapIndexed { i, item ->
                    if (item == null) {
                        "$NULL_KEY_PREFIX$i"
                    } else {
                        stableItemKey(item) ?: "$NULL_KEY_PREFIX$i"
                    }
                }
            var removed = 0
            val filtered = ArrayList<Any?>(items.size)
            items.forEachIndexed { i, item ->
                val key = keys[i].takeIf { !it.startsWith(NULL_KEY_PREFIX) }
                val drop = item != null && key != null && isAdItem(key)
                if (drop) removed++ else filtered += item
            }

            lastFingerprint = fp

            if (removed == 0) {
                lastFilteredSnapshot = null
                return result
            }

            HookMetrics.addAdsHidden(removed)
            lastFilteredSnapshot = filtered
            return filtered
        }
    }

    private fun isAdItem(key: String): Boolean {
        decisionCache[key]?.let { return it }
        val lower = key.lowercase(Locale.ROOT)
        val isAd = adClusterTokens.any { it in lower }
        decisionCache[key] = isAd
        return isAd
    }

    @Suppress("kotlin:S6518") // Field.get() is reflection, not a collection accessor
    private fun stableItemKey(item: Any): String? {
        contentId(item)?.let { return it }
        return instanceFields(item)
            .asSequence()
            .mapNotNull { f -> runCatching { f.get(item) as? String }.getOrNull() }
            .firstOrNull { it.isNotBlank() }
            // Avoid fully-qualified names in keys to keep logs/caches readable.
            ?.let { "${item.javaClass.simpleName}#$it" }
    }

    private fun contentId(item: Any): String? {
        // f122746b: obfuscated content-ID field on ContentRenderableSlice
        val value = readField(item, "f122746b") as? String
        return value?.takeIf { it.isNotBlank() }
    }

    @Suppress("kotlin:S6518") // Field.get() is reflection, not a collection accessor
    private fun readField(
        instance: Any,
        name: String,
    ): Any? {
        val cacheKey = "${classCacheKey(instance.javaClass)}#$name"
        val field =
            fieldCache[cacheKey] ?: run {
                var c: Class<*>? = instance.javaClass
                while (c != null && c != Any::class.java) {
                    val f = runCatching { c.getDeclaredField(name) }.getOrNull()
                    if (f != null) {
                        f.isAccessible = true
                        fieldCache[cacheKey] = f
                        return@run f
                    }
                    c = c.superclass
                }
                null
            } ?: return null

        return runCatching { field.get(instance) }.getOrNull()
    }

    private fun instanceFields(obj: Any): List<Field> =
        classFieldCache.computeIfAbsent(classCacheKey(obj.javaClass)) {
            buildList {
                var c: Class<*>? = obj.javaClass
                while (c != null && c != Any::class.java) {
                    c.declaredFields.forEach { f ->
                        if (!Modifier.isStatic(f.modifiers) && !f.isSynthetic) {
                            runCatching { f.isAccessible = true }
                            add(f)
                        }
                    }
                    c = c.superclass
                }
            }
        }

    private fun fastFingerprint(items: List<*>): Long {
        // skip re-filtering when list identity hasn't changed
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
