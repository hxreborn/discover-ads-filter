package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger

@Volatile
internal var verbose: Boolean = false

private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

internal fun loadHookPrefs(prefs: SharedPreferences) {
    verbose = SettingsPrefs.verbose.read(prefs)
    if (prefListener == null) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { src, key ->
                when (key) {
                    null -> loadHookPrefs(src)
                    SettingsPrefs.verbose.key -> verbose = SettingsPrefs.verbose.read(src)
                }
            }
        runCatching { prefs.registerOnSharedPreferenceChangeListener(listener) }
            .onSuccess { prefListener = listener }
            .onFailure { Logger.log(Log.WARN, "failed to register pref listener: ${it.message}") }
    }
}
