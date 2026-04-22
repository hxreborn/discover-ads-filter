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
                        "ads" -> adsHidden.set(parts[1].toLongOrNull() ?: 0)
                        "hook_status" -> lastHookStatus = parts[1]
                        "hook_process" -> lastHookProcess = parts[1]
                    }
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

    fun reportHookStatus(
        status: String,
        process: String,
    ) {
        lastHookStatus = status
        lastHookProcess = process
        writeMetricsFile()
        // TODO: Remove when i decide if i use root or another way to write counter
        Thread {
            runCatching { Thread.sleep(3_000) }
            callProvider(
                MetricsProvider.METHOD_REPORT_HOOK_STATUS,
                Bundle().apply {
                    putString(MetricsProvider.KEY_STATUS, status)
                    putString(MetricsProvider.KEY_PROCESS, process)
                },
            )
        }.start()
    }

    fun addAdsHidden(count: Int) {
        if (count <= 0) return
        val total = adsHidden.addAndGet(count.toLong())
        writeMetricsFile()
        Logger.v { "ads filtered: +$count (total=$total)" }
        callProvider(
            MetricsProvider.METHOD_INCREMENT,
            Bundle().apply { putInt(MetricsProvider.KEY_COUNT, count) },
        )
    }

    private fun writeMetricsFile() {
        runCatching {
            metricsFile?.writeText(
                buildString {
                    append("ads=${adsHidden.get()}\n")
                    lastHookStatus?.let { append("hook_status=$it\n") }
                    lastHookProcess?.let { append("hook_process=$it\n") }
                },
            )
        }.onFailure {
            Logger.w("metrics file write failed: ${it.message}")
        }
    }

    private fun callProvider(
        method: String,
        extras: Bundle,
    ) {
        val ctx =
            context() ?: run {
                Logger.w("callProvider($method): no app context")
                return
            }
        runCatching {
            ctx.contentResolver.call(PROVIDER_URI, method, null, extras)
        }.onFailure {
            Logger.w("callProvider($method) failed: ${it.message}")
        }
    }
}
