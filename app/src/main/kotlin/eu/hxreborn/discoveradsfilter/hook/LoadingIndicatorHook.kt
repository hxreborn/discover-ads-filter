package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.util.Logger
import java.lang.reflect.Method
import java.util.WeakHashMap

object LoadingIndicatorHook {
    fun install(
        loader: ClassLoader,
        targets: ResolvedTargets,
        context: Context,
    ): Boolean {
        val ref = (targets as? ResolvedTargets.Resolved)?.refreshProgressMethod
        if (ref == null) {
            Logger.log(Log.WARN, "no refresh indicator in cache, run Verify")
            return false
        }
        val progressMethod: Method
        val drawMethod: Method
        val layoutMethods: Array<Method>
        try {
            progressMethod = ref.resolve(loader)
            drawMethod = progressMethod.declaringClass.getDeclaredMethod("draw", Canvas::class.java)
            layoutMethods =
                loader
                    .loadClass("androidx.swiperefreshlayout.widget.SwipeRefreshLayout")
                    .declaredMethods
        } catch (e: ReflectiveOperationException) {
            Logger.log(Log.WARN, "refresh indicator target missing: ${e.message}")
            return false
        } catch (e: LinkageError) {
            Logger.log(Log.WARN, "refresh indicator target missing: ${e.message}")
            return false
        }
        val indicators = WeakHashMap<Drawable, LoadingIndicatorDrawing>()

        fun indicator(drawable: Drawable): LoadingIndicatorDrawing =
            synchronized(indicators) {
                indicators.getOrPut(drawable) { LoadingIndicatorDrawing(context) }
            }

        layoutMethods.forEach { module.deoptimize(it) }
        module.hook(progressMethod).intercept { chain ->
            val result = chain.proceed()
            if (expressiveLoadingIndicator) {
                indicator(chain.thisObject as Drawable).progress =
                    ((chain.getArg(0) as Float) / 0.8f).coerceIn(0f, 1f)
            }
            result
        }
        module.hook(drawMethod).intercept { chain ->
            if (!expressiveLoadingIndicator) {
                chain.proceed()
            } else {
                val drawable = chain.thisObject as Drawable
                indicator(drawable).draw(drawable, chain.getArg(0) as Canvas)
                null
            }
        }
        Logger.log(Log.INFO, "hooked refresh indicator $ref")
        return true
    }
}
