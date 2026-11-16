package eu.hxreborn.discoveradsfilter.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.util.Logger
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

object ShareLinkHook {
    private const val WRAPPER_HOST = "share.google/"
    private const val WRAPPER_DOMAIN = "share.google"
    private const val MAX_NODES = 600
    private const val MAX_DEPTH = 5

    private val wrapperUrl = Regex("""https?://share\.google/\S+""")
    private val trackingParams = setOf("shem")

    private val articleUrl = ThreadLocal<String?>()

    fun install(
        loader: ClassLoader,
        targets: ResolvedTargets,
        processName: String,
    ): Boolean {
        val resolved = targets as? ResolvedTargets.Resolved
        val feedShareHooked = installFeedShare(loader, resolved, processName)
        val webPageShareHooked = installWebPageShare(loader, resolved, processName)
        return feedShareHooked || webPageShareHooked
    }

    private fun installFeedShare(
        loader: ClassLoader,
        targets: ResolvedTargets.Resolved?,
        processName: String,
    ): Boolean {
        val ref = targets?.shareIntentMethod
        if (ref == null) {
            Logger.log(
                Log.WARN,
                "no share method in cache proc=$processName, share links unchanged",
            )
            return false
        }
        val builder =
            try {
                ref.resolve(loader)
            } catch (e: ReflectiveOperationException) {
                Logger.log(Log.WARN, "failed to rehydrate $ref: ${e.message}")
                return false
            }
        val chooser =
            Intent::class.java.getDeclaredMethod(
                "createChooser",
                Intent::class.java,
                CharSequence::class.java,
            )
        module.deoptimize(builder)
        module.deoptimize(chooser)
        module.hook(builder).intercept(ShareBuilderHooker)
        module.hook(chooser).intercept(ChooserHooker)
        Logger.log(Log.INFO, "hooked $ref proc=$processName")
        return true
    }

    private fun installWebPageShare(
        loader: ClassLoader,
        targets: ResolvedTargets.Resolved?,
        processName: String,
    ): Boolean {
        val ref = targets?.customTabShareMethod
        val fieldName = targets?.customTabShortLinkFieldName
        if (ref == null || fieldName == null) {
            Logger.log(
                Log.WARN,
                "no web page share method in cache proc=$processName, share links unchanged",
            )
            return false
        }
        val (share, shortLinkSwitch) =
            try {
                val method = ref.resolve(loader)
                val field =
                    method.declaringClass.getDeclaredField(fieldName).also {
                        it.isAccessible = true
                    }
                method to field
            } catch (e: ReflectiveOperationException) {
                Logger.log(Log.WARN, "failed to rehydrate $ref $fieldName: ${e.message}")
                return false
            }
        val activityField =
            share.declaringClass.declaredFields
                .singleOrNull { Activity::class.java.isAssignableFrom(it.type) }
                ?.also { it.isAccessible = true }
        if (activityField == null) {
            Logger.log(Log.WARN, "no activity field on ${share.declaringClass.name}")
            return false
        }
        module.deoptimize(share)
        val callers = targets.customTabShareCallers.count { deoptimizeCaller(loader, it) }
        module.hook(share).intercept(WebPageShareHooker(shortLinkSwitch, activityField))
        Logger.log(
            Log.INFO,
            "hooked $ref switch=$fieldName callers=$callers proc=$processName",
        )
        return true
    }

    private fun deoptimizeCaller(
        loader: ClassLoader,
        ref: MethodRef,
    ): Boolean =
        try {
            module.deoptimize(ref.resolve(loader))
        } catch (e: ReflectiveOperationException) {
            Logger.log(Log.WARN, "failed to rehydrate caller $ref: ${e.message}")
            false
        }

    private class WebPageShareHooker(
        private val shortLinkSwitch: Field,
        private val activityField: Field,
    ) : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            if (!shareOriginalLink) return chain.proceed()
            val share = chain.thisObject ?: return chain.proceed()
            val pageUrl = (activityField.get(share) as? Activity)?.intent?.data
            val args = webPageShareArgs(chain.args, pageUrl)
            if (!shortLinkSwitch.getBoolean(share)) return chain.proceed(args)
            shortLinkSwitch.setBoolean(share, false)
            try {
                return chain.proceed(args)
            } finally {
                shortLinkSwitch.setBoolean(share, true)
            }
        }
    }

    private fun webPageShareArgs(
        args: List<Any?>,
        pageUrl: Uri?,
    ): Array<Any?> {
        val out = args.toTypedArray()
        val shared = out[0] as? Uri
        if (shared?.host == WRAPPER_DOMAIN && pageUrl != null) {
            out[0] = pageUrl
            Logger.debug { "web page share $shared -> $pageUrl" }
        }
        val custom = shareCustomLine.trim()
        out[1] =
            when {
                shareStripSourceLine -> null
                custom.isNotEmpty() -> custom
                else -> out[1]
            }
        return out
    }

    private object ShareBuilderHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            if (!shareOriginalLink) return chain.proceed()
            articleUrl.set(runCatching { findArticleUrl(chain.args) }.getOrNull())
            try {
                return chain.proceed()
            } finally {
                articleUrl.remove()
            }
        }
    }

    private object ChooserHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val url = articleUrl.get()
            if (url != null) {
                runCatching { rewrite(chain.args.getOrNull(0) as? Intent, url) }
                    .onFailure { Logger.log(Log.WARN, "share rewrite failed: ${it.message}") }
            }
            return chain.proceed()
        }
    }

    private fun rewrite(
        target: Intent?,
        url: String,
    ) {
        val text = target?.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val wrapper = wrapperUrl.find(text)?.value ?: return
        val custom = shareCustomLine.trim()
        target.putExtra(
            Intent.EXTRA_TEXT,
            when {
                shareStripSourceLine -> url
                custom.isNotEmpty() -> "$custom $url"
                else -> text.replace(wrapper, url)
            },
        )
        Logger.debug { "share link $wrapper -> $url" }
    }

    private fun findArticleUrl(args: List<Any?>): String? {
        val found = LinkedHashSet<String>()
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val queue = ArrayDeque<Pair<Any, Int>>()
        args.filterNotNull().forEach { queue += it to 0 }
        var nodes = 0
        while (queue.isNotEmpty() && nodes < MAX_NODES) {
            val (value, depth) = queue.removeFirst()
            if (!seen.add(value)) continue
            nodes++
            if (value is String) {
                if (value.startsWith("http") && !value.contains(WRAPPER_HOST)) found += value
                continue
            }
            if (depth >= MAX_DEPTH || value is Number || value is Boolean || value is Char) continue
            val cls = value.javaClass
            if (cls.name.startsWith("java.") || cls.name.startsWith("android.")) continue
            generateSequence(cls) { it.superclass.takeIf { c -> c != Any::class.java } }
                .flatMap { it.declaredFields.asSequence() }
                .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                .forEach { field ->
                    runCatching {
                        field.isAccessible = true
                        field.get(value)?.let { queue += it to depth + 1 }
                    }
                }
        }
        return found.firstOrNull { '?' !in it } ?: found.firstOrNull()?.let(::stripTracking)
    }

    private fun stripTracking(url: String): String {
        val uri = Uri.parse(url)
        val names = runCatching { uri.queryParameterNames }.getOrNull() ?: return url
        if (names.none { it in trackingParams }) return url
        val builder = uri.buildUpon().clearQuery()
        names
            .filterNot { it in trackingParams }
            .forEach { name ->
                uri.getQueryParameters(name).forEach { builder.appendQueryParameter(name, it) }
            }
        return builder.build().toString()
    }
}
