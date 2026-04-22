package eu.hxreborn.discoveradsfilter.hook

import eu.hxreborn.discoveradsfilter.util.Logger
import java.io.File
import java.util.concurrent.atomic.AtomicLong

@Suppress("SdCardPath")
object HookMetrics {
    private const val AGSA_CACHE = "/data/data/com.google.android.googlequicksearchbox/cache"
    private const val METRICS_FILENAME = "discover_adsfilter_metrics.txt"
    private val adsHidden = AtomicLong(0)

    @Volatile
    private var lastHookStatus: String? = null

    @Volatile
    private var lastHookProcess: String? = null

    @Volatile
    private var metricsFile: File? = null

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
        adsHidden.addAndGet(count.toLong())
        writeMetricsFile()
    }

    fun flush() {
        writeMetricsFile()
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
}
