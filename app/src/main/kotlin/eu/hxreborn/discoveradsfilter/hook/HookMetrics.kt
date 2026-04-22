package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.net.Uri
import android.os.Bundle
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.Logger
import java.io.File
import java.util.concurrent.atomic.AtomicLong

@Suppress("SdCardPath")
object HookMetrics {
    private const val AGSA_CACHE = "/data/data/com.google.android.googlequicksearchbox/cache"
    private const val METRICS_FILENAME = "discover_adsfilter_metrics.txt"
    private val PROVIDER_URI = Uri.parse("content://${MetricsProvider.AUTHORITY}")

    private val adsHidden = AtomicLong(0)

    @Volatile
    private var metricsFile: File? = null

    @Volatile
    private var appContext: Context? = null

    fun init() {
        val dir = File(AGSA_CACHE)
        dir.mkdirs()
        metricsFile = File(dir, METRICS_FILENAME)

        runCatching {
            metricsFile?.takeIf { it.exists() }?.readLines()?.forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == "ads") {
                    adsHidden.set(parts[1].toLongOrNull() ?: 0)
                }
            }
        }
    }

    private fun context(): Context? {
        appContext?.let { return it }
        @Suppress("PrivateApi")
        val ctx =
            runCatching {
                Class
                    .forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null) as? Context
            }.getOrNull()
        ctx?.let { appContext = it }
        return ctx
    }

    fun addAdsHidden(count: Int) {
        if (count <= 0) return
        val total = adsHidden.addAndGet(count.toLong())
        runCatching {
            metricsFile?.writeText("ads=$total\n")
        }.onFailure {
            Logger.w("metrics file write failed: ${it.message}")
        }
        Logger.i("ads filtered: +$count (total=$total)")
        reportToProvider(count)
    }

    private fun reportToProvider(delta: Int) {
        val ctx =
            context() ?: run {
                Logger.w("reportToProvider: no app context")
                return
            }
        runCatching {
            ctx.contentResolver.call(
                PROVIDER_URI,
                MetricsProvider.METHOD_INCREMENT,
                null,
                Bundle().apply { putInt(MetricsProvider.KEY_COUNT, delta) },
            )
            Logger.i("metrics provider incremented by $delta")
        }.onFailure {
            Logger.w("metrics provider call failed: ${it.message}")
        }
    }
}
