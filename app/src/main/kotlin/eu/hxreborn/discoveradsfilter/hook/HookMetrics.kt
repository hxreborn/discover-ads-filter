package eu.hxreborn.discoveradsfilter.hook

import eu.hxreborn.discoveradsfilter.util.Logger
import java.io.File
import java.util.concurrent.atomic.AtomicLong

object HookMetrics {
    // Known AGSA data dir — hook process runs as this UID so we can write here.
    // Avoids depending on ActivityThread.currentApplication(), which is null in onPackageReady.
    private const val AGSA_CACHE = "/data/data/com.google.android.googlequicksearchbox/cache"
    private const val METRICS_FILENAME = "discover_adsfilter_metrics.txt"

    private val adsHidden = AtomicLong(0)

    @Volatile
    private var metricsFile: File? = null

    fun init() {
        val dir = File(AGSA_CACHE)
        runCatching { dir.mkdirs() }
        val file = File(dir, METRICS_FILENAME)
        metricsFile = file
        runCatching {
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2 && parts[0] == "ads") {
                        adsHidden.set(parts[1].toLongOrNull() ?: 0)
                    }
                }
            }
        }
    }

    fun addAdsHidden(count: Int) {
        val total = adsHidden.addAndGet(count.toLong())
        runCatching {
            metricsFile?.writeText("ads=$total\n")
        }.onFailure {
            Logger.w("metrics file write failed: ${it.message}")
        }
    }

    fun adsCount(): Long = adsHidden.get()
}
