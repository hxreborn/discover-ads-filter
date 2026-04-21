package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.Safe
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object StreamSliceFilterHook {
    private const val TAG = "DiscoverAdsFilter/StreamSlice"

    private val adClusterTokens = setOf("feedads")

    private val decisionCache = ConcurrentHashMap<String, Boolean>()
    private val fieldCache = ConcurrentHashMap<String, Field>()

    @Volatile
    private var hookPrefs: SharedPreferences? = null

    fun install(
        loader: ClassLoader,
        prefs: SharedPreferences,
        targets: ResolvedTargets,
    ) = Safe.run(TAG, "install") {
        hookPrefs = prefs
        val streamRef = (targets as? ResolvedTargets.Resolved)?.streamRenderableListMethod
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

            var removed = 0
            val filtered = ArrayList<Any?>(items.size)
            for (item in items) {
                val id = item?.let { contentId(it) }
                val drop = id != null && isAdItem(id)
                if (drop) removed++ else filtered += item
            }

            if (removed == 0) return result

            HookMetrics.addAdsHidden(removed)
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

    private fun contentId(item: Any): String? {
        // f122746b: obfuscated content-ID field on ContentRenderableSlice
        val cacheKey = "${classCacheKey(item.javaClass)}#f122746b"
        val field =
            fieldCache[cacheKey] ?: run {
                var c: Class<*>? = item.javaClass
                while (c != null && c != Any::class.java) {
                    val f = runCatching { c.getDeclaredField("f122746b") }.getOrNull()
                    if (f != null) {
                        f.isAccessible = true
                        fieldCache[cacheKey] = f
                        return@run f
                    }
                    c = c.superclass
                }
                null
            } ?: return null

        return (runCatching { field.get(item) }.getOrNull() as? String)?.takeIf { it.isNotBlank() }
    }
}
