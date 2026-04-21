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
                key.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) && key !in
                    setOf(
                        SettingsPrefs.fingerprintCurrent.key,
                        SettingsPrefs.fingerprintCurrentVersion.key,
                        SettingsPrefs.fingerprintCurrentModuleVersion.key,
                        resolvedKey,
                    )
            }
        save {
            // Keep the versioned entry for AGSA+module lookups.
            putString(resolvedKey, encoded)

            // Drop entries from older schema, module, and AGSA builds.
            // A prefix bump invalidates old entries without touching new ones.
            keysToPrune.forEach { remove(it) }

            // Fall back to the latest scan when the hook side has no AGSA version code.
            SettingsPrefs.fingerprintCurrent.write(this, encoded)
            SettingsPrefs.fingerprintCurrentVersion.write(this, agsaVersionCode)
            SettingsPrefs.fingerprintCurrentModuleVersion.write(this, BuildConfig.VERSION_CODE)
        }
    }

    fun readLastScan(): CachedScan? {
        val raw = SettingsPrefs.fingerprintCurrent.read(localPrefs) ?: return null
        val resolved =
            runCatching { DexKitCache.decode(raw) }.getOrNull() as? ResolvedTargets.Resolved
                ?: return null
        val version = SettingsPrefs.fingerprintCurrentVersion.read(localPrefs)
        val moduleVersion = SettingsPrefs.fingerprintCurrentModuleVersion.read(localPrefs)
        return CachedScan(version, resolved, moduleVersion)
    }

    fun readHookStatus(): String? = readPrefRemoteFirst(SettingsPrefs.hookStatus)

    fun readHookProcess(): String? = readPrefRemoteFirst(SettingsPrefs.hookProcess)

    private fun readPrefRemoteFirst(spec: PrefSpec<String?>): String? =
        remotePrefsProvider()?.let { spec.read(it) } ?: spec.read(localPrefs)

    fun readAdsHidden(): Long {
        val localAds = SettingsPrefs.adsHidden.read(localPrefs)
        val remoteAds = remotePrefsProvider()?.let { SettingsPrefs.adsHidden.read(it) } ?: 0L
        val ads = maxOf(localAds, remoteAds)
        if (remoteAds > localAds) {
            localPrefs.edit { SettingsPrefs.adsHidden.write(this, ads) }
        }
        return if (ads > 0L) ads else readAdsFromAgsa()
    }

    private fun readAdsFromAgsa(): Long =
        runCatching {
            val agsaInfo =
                context.packageManager.getApplicationInfo(
                    "com.google.android.googlequicksearchbox",
                    0,
                )
            val metricsFile =
                File(File(agsaInfo.dataDir, "cache"), "discover_adsfilter_metrics.txt")
            if (!metricsFile.exists()) return@runCatching 0L
            metricsFile
                .readLines()
                .firstOrNull { it.startsWith("ads=") }
                ?.substringAfter('=')
                ?.toLongOrNull()
                ?: 0L
        }.getOrDefault(0L)

    fun clearScanCache() {
        val keysToRemove =
            localPrefs.all.keys.filter { it.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) }
        save { keysToRemove.forEach { remove(it) } }
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
