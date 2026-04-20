package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import java.io.File
import java.util.concurrent.atomic.AtomicLong

object HookMetrics {
    private val adsHidden = AtomicLong(0)

    @Volatile
    private var prefs: SharedPreferences? = null
    private var metricsFile: File? = null

    fun init(prefs: SharedPreferences) {
        this.prefs = prefs
        adsHidden.set(SettingsPrefs.adsHidden.read(prefs))
    }

    fun setMetricsDir(dir: File) {
        metricsFile = File(dir, "discover_adsfilter_metrics.txt")
        runCatching {
            metricsFile?.readText()?.lines()?.forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    when (parts[0]) {
                        "ads" -> adsHidden.set(parts[1].toLongOrNull() ?: 0)
                    }
                }
            }
        }
    }

    fun addAdsHidden(count: Int) {
        adsHidden.addAndGet(count.toLong())
        writeMetrics()
    }

    fun adsCount(): Long = adsHidden.get()

    private fun writeMetrics() {
        val ads = adsHidden.get()
        runCatching {
            prefs?.edit {
                SettingsPrefs.adsHidden.write(this, ads)
                SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
            }
        }
        runCatching {
            metricsFile?.writeText("ads=$ads\n")
        }
    }
}
