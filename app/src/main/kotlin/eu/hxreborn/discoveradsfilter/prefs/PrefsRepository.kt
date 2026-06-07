package eu.hxreborn.discoveradsfilter.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.discovery.DexKitCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import android.content.SharedPreferences.OnSharedPreferenceChangeListener as OnChangeListener

class PrefsRepository(
    private val local: SharedPreferences,
    private val remoteProvider: () -> SharedPreferences?,
) {
    fun <T> read(spec: PrefSpec<T>): T = spec.read(local)

    fun <T> save(
        spec: PrefSpec<T>,
        value: T,
    ) {
        local.edit { spec.write(this, value) }
        remoteProvider()?.edit(commit = true) {
            spec.write(this, value)
            SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
        }
    }

    val state: Flow<AppPrefs> =
        callbackFlow {
            val listener = OnChangeListener { _, _ -> trySend(readAll()) }
            local.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { local.unregisterOnSharedPreferenceChangeListener(listener) }
        }.onStart { emit(readAll()) }
            .distinctUntilChanged()

    fun writeResolvedTargets(
        agsaVersionCode: Long,
        targets: ResolvedTargets,
    ) {
        val resolvedKey = SettingsPrefs.fingerprintKey(agsaVersionCode, BuildConfig.VERSION_CODE)
        val encoded = DexKitCache.encode(targets)

        val keysToPrune =
            local.all.keys.filter { key ->
                key.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) && key !in
                    setOf(
                        SettingsPrefs.fingerprintCurrent.key,
                        SettingsPrefs.fingerprintCurrentVersion.key,
                        SettingsPrefs.fingerprintCurrentModuleVersion.key,
                        resolvedKey,
                    )
            }
        edit {
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
        val raw = SettingsPrefs.fingerprintCurrent.read(local) ?: return null
        val resolved =
            runCatching { DexKitCache.decode(raw) }.getOrNull() as? ResolvedTargets.Resolved
                ?: return null
        val version = SettingsPrefs.fingerprintCurrentVersion.read(local)
        val moduleVersion = SettingsPrefs.fingerprintCurrentModuleVersion.read(local)
        return CachedScan(version, resolved, moduleVersion)
    }

    fun readAdsHidden(): Long = SettingsPrefs.adsHidden.read(local)

    // MetricsProvider writes the counter in this same process, so a local change
    // listener sees every increment without polling.
    fun adsHiddenFlow(): Flow<Long> =
        callbackFlow {
            trySend(readAdsHidden())
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == null || key == SettingsPrefs.adsHidden.key) {
                        trySend(readAdsHidden())
                    }
                }
            local.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { local.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    fun resetAdsCounter() {
        edit { SettingsPrefs.adsHidden.write(this, 0L) }
    }

    fun clearScanCache() {
        val keysToRemove =
            local.all.keys.filter { it.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) }
        edit { keysToRemove.forEach { remove(it) } }
    }

    fun syncToRemote() {
        val remote = remoteProvider() ?: return
        var changed = false
        remote.edit(commit = true) {
            SettingsPrefs.all.forEach { spec ->
                if (spec.copyIfChanged(local, remote, this)) changed = true
            }
            // Sync versioned fingerprint keys that aren't in the typed all list.
            local.all.keys
                .filter { it.startsWith(SettingsPrefs.KEY_FINGERPRINT_PREFIX) }
                .forEach { key ->
                    if (SettingsPrefs.all.none { it.key == key }) {
                        val localValue = local.getString(key, null) ?: return@forEach
                        if (localValue != remote.getString(key, null)) {
                            putString(key, localValue)
                            changed = true
                        }
                    }
                }
            if (changed) SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
        }
    }

    private fun readAll() =
        AppPrefs(
            verbose = read(SettingsPrefs.verbose),
            filterEnabled = read(SettingsPrefs.filterEnabled),
        )

    private inline fun edit(crossinline block: SharedPreferences.Editor.() -> Unit) {
        local.edit { block() }
        remoteProvider()?.edit(commit = true) {
            block()
            SettingsPrefs.lastRemoteWrite.write(this, System.currentTimeMillis())
        }
    }
}

data class AppPrefs(
    val verbose: Boolean,
    val filterEnabled: Boolean = true,
)

data class CachedScan(
    val versionCode: Long,
    val targets: ResolvedTargets.Resolved,
    val moduleVersionCode: Int = 0,
)
