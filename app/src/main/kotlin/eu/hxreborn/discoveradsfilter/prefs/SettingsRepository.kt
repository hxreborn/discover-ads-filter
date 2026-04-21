package eu.hxreborn.discoveradsfilter.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.discovery.DexKitCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import java.io.File

class SettingsRepository(
    private val context: Context,
    private val remotePrefsProvider: () -> SharedPreferences?,
) {
    private val localPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(SettingsPrefs.GROUP, Context.MODE_PRIVATE)

    fun snapshot(): PersistedSettings =
        PersistedSettings(
            verbose = SettingsPrefs.verbose.read(localPrefs),
            filterEnabled = SettingsPrefs.filterEnabled.read(localPrefs),
        )

    fun setVerbose(value: Boolean) = save { SettingsPrefs.verbose.write(this, value) }

    fun setFilterEnabled(value: Boolean) = save { SettingsPrefs.filterEnabled.write(this, value) }

    fun writeResolvedTargets(
        agsaVersionCode: Long,
        targets: ResolvedTargets,
    ) {
        val resolvedKey = SettingsPrefs.fingerprintKey(agsaVersionCode, BuildConfig.VERSION_CODE)
        val encoded = DexKitCache.encode(targets)

        val keysToPrune =
            localPrefs.all.keys.filterTo(mutableSetOf()) { key ->
                key.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) && key != resolvedKey
            }
        save {
            putString(resolvedKey, encoded)
            // Prefix bump (fp_v4_ → fp_v5_) invalidates old entries without touching new ones.
            keysToPrune.forEach { remove(it) }
        }
    }

    fun readLastScan(agsaVersionCode: Long): CachedScan? {
        if (agsaVersionCode == 0L) return null
        val key = SettingsPrefs.fingerprintKey(agsaVersionCode, BuildConfig.VERSION_CODE)
        val raw = localPrefs.getString(key, null) ?: return null
        val resolved =
            runCatching { DexKitCache.decode(raw) }.getOrNull() as? ResolvedTargets.Resolved
                ?: return null
        return CachedScan(agsaVersionCode, resolved, BuildConfig.VERSION_CODE)
    }

    fun readHookStatus(): String? =
        remotePrefsProvider()?.let { SettingsPrefs.hookStatus.read(it) }
            ?: SettingsPrefs.hookStatus.read(localPrefs)

    fun readHookProcess(): String? =
        remotePrefsProvider()?.let { SettingsPrefs.hookProcess.read(it) }
            ?: SettingsPrefs.hookProcess.read(localPrefs)

    fun readAdsHidden(): Long {
        val localAds = SettingsPrefs.adsHidden.read(localPrefs)
        val remote = remotePrefsProvider()
        val remoteAds = remote?.let { SettingsPrefs.adsHidden.read(it) } ?: 0L
        val ads = maxOf(localAds, remoteAds)
        if (ads > localAds) {
            localPrefs.edit { SettingsPrefs.adsHidden.write(this, ads) }
        }
        if (ads > 0L) return ads
        return readAdsFromAgsa()
    }

    private fun readAdsFromAgsa(): Long {
        var ads = 0L
        runCatching {
            val agsaInfo =
                context.packageManager.getApplicationInfo(
                    "com.google.android.googlequicksearchbox",
                    0,
                )
            val cacheDir = File(agsaInfo.dataDir, "cache")
            val metricsFile = File(cacheDir, "discover_adsfilter_metrics.txt")
            if (metricsFile.exists()) {
                metricsFile.readLines().forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2 && parts[0] == "ads") {
                        ads = parts[1].toLongOrNull() ?: 0
                    }
                }
            }
        }
        return ads
    }

    fun syncLocalToRemote() {
        val remote = remotePrefsProvider() ?: return
        remote.edit(commit = true) {
            SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
            localPrefs.all.forEach { (key, value) ->
                if (key in HOOK_OWNED_KEYS) return@forEach
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                }
            }
        }
    }

    private inline fun save(crossinline block: SharedPreferences.Editor.() -> Unit) {
        localPrefs.edit { block() }
        remotePrefsProvider()?.edit(commit = true) {
            block()
            SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
        }
    }

    private companion object {
        private val HOOK_OWNED_KEYS: Set<String> =
            setOf(
                SettingsPrefs.hookStatus.key,
                SettingsPrefs.hookProcess.key,
                SettingsPrefs.adsHidden.key,
            )
    }
}

data class PersistedSettings(
    val verbose: Boolean,
    val filterEnabled: Boolean = true,
)

data class CachedScan(
    val versionCode: Long,
    val targets: ResolvedTargets.Resolved,
    val moduleVersionCode: Int = 0,
)
