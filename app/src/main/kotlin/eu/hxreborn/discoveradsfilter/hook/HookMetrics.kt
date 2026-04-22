package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.net.Uri
import android.os.Bundle
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.Logger
import java.io.File

@Suppress("SdCardPath")
object HookMetrics {
    private const val AGSA_CACHE = "/data/data/com.google.android.googlequicksearchbox/cache"
    private const val METRICS_FILENAME = "discover_adsfilter_metrics.txt"
    private val PROVIDER_URI = Uri.parse("content://${MetricsProvider.AUTHORITY}")

    @Volatile
    private var lastHookStatus: String? = null

    @Volatile
    private var lastHookProcess: String? = null

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
                if (parts.size == 2) {
                    when (parts[0]) {
                        "hook_status" -> lastHookStatus = parts[1]
                        "hook_process" -> lastHookProcess = parts[1]
                    }
                }
            }
        }
    }

    fun reportHookStatus(
        status: String,
        process: String,
    ) {
        lastHookStatus = status
        lastHookProcess = process
        writeMetricsFile()
    }

    fun addAdsHidden(count: Int) {
        if (count <= 0) return
        val ctx = context() ?: return
        runCatching {
            ctx.contentResolver.call(
                PROVIDER_URI,
                MetricsProvider.METHOD_INCREMENT,
                null,
                Bundle().apply { putInt(MetricsProvider.KEY_COUNT, count) },
            )
        }.onFailure {
            Logger.w("provider increment failed: ${it.message}")
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

    private fun writeMetricsFile() {
        runCatching {
            metricsFile?.writeText(
                buildString {
                    lastHookStatus?.let { append("hook_status=$it\n") }
                    lastHookProcess?.let { append("hook_process=$it\n") }
                },
            )
        }.onFailure {
            Logger.w("metrics file write failed: ${it.message}")
        }
    }
}
