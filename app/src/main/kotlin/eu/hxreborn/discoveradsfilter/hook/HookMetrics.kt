package eu.hxreborn.discoveradsfilter.hook

import android.annotation.SuppressLint
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

    private const val KEY_HOOK_STATUS = "hook_status"
    private const val KEY_HOOK_PROCESS = "hook_process"

    @SuppressLint("UseKtx")
    private val providerUri = Uri.parse("content://${MetricsProvider.AUTHORITY}")
    private val metricsFile = File(AGSA_CACHE, METRICS_FILENAME)

    @Volatile
    private var lastHookStatus: String? = null

    @Volatile
    private var lastHookProcess: String? = null

    @Volatile
    private var appContext: Context? = null

    fun init() {
        metricsFile.parentFile?.mkdirs()

        runCatching {
            if (!metricsFile.exists()) return

            metricsFile.forEachLine { line ->
                val key = line.substringBefore("=", missingDelimiterValue = "")
                val value = line.substringAfter("=", missingDelimiterValue = "")

                when (key) {
                    KEY_HOOK_STATUS -> lastHookStatus = value
                    KEY_HOOK_PROCESS -> lastHookProcess = value
                }
            }
        }.onFailure {
            Logger.w("metrics file read failed: ${it.message}")
        }
    }

    fun reportHookStatus(
        status: String,
        process: String,
    ) {
        lastHookStatus = status
        lastHookProcess = process
        writeMetricsFile(status, process)
    }

    fun addAdsHidden(count: Int) {
        if (count <= 0) return

        val ctx = context() ?: return

        runCatching {
            ctx.contentResolver.call(
                providerUri,
                MetricsProvider.METHOD_INCREMENT,
                null,
                Bundle().apply {
                    putInt(MetricsProvider.KEY_COUNT, count)
                },
            )
        }.onFailure {
            Logger.w("provider increment failed: ${it.message}")
        }
    }

    private fun context(): Context? {
        appContext?.let { return it }

        @Suppress("PrivateApi")
        return runCatching {
            Class
                .forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Context
        }.getOrNull()?.let { it.applicationContext ?: it }?.also { appContext = it }
    }

    private fun writeMetricsFile(
        status: String,
        process: String,
    ) {
        runCatching {
            metricsFile.writeText(
                buildString {
                    append(KEY_HOOK_STATUS).append('=').append(status).append('\n')
                    append(KEY_HOOK_PROCESS).append('=').append(process).append('\n')
                },
            )
        }.onFailure {
            Logger.w("metrics file write failed: ${it.message}")
        }
    }
}
