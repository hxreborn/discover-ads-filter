package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger

@Volatile
internal var verbose: Boolean = false

@Volatile
internal var shareOriginalLink: Boolean = false

@Volatile
internal var shareStripSourceLine: Boolean = false

@Volatile
internal var shareCustomLine: String = ""

private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

internal fun loadHookPrefs(prefs: SharedPreferences) {
    verbose = SettingsPrefs.verbose.read(prefs)
    shareOriginalLink = SettingsPrefs.shareOriginalLink.read(prefs)
    shareStripSourceLine = SettingsPrefs.shareStripSourceLine.read(prefs)
    shareCustomLine = SettingsPrefs.shareCustomLine.read(prefs).orEmpty()
    if (prefListener == null) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { src, key ->
                when (key) {
                    null -> {
                        loadHookPrefs(src)
                    }

                    SettingsPrefs.verbose.key -> {
                        verbose = SettingsPrefs.verbose.read(src)
                    }

                    SettingsPrefs.shareOriginalLink.key -> {
                        shareOriginalLink = SettingsPrefs.shareOriginalLink.read(src)
                    }

                    SettingsPrefs.shareStripSourceLine.key -> {
                        shareStripSourceLine = SettingsPrefs.shareStripSourceLine.read(src)
                    }

                    SettingsPrefs.shareCustomLine.key -> {
                        shareCustomLine = SettingsPrefs.shareCustomLine.read(src).orEmpty()
                    }
                }
            }
        runCatching { prefs.registerOnSharedPreferenceChangeListener(listener) }
            .onSuccess { prefListener = listener }
            .onFailure { Logger.log(Log.WARN, "failed to register pref listener: ${it.message}") }
    }
}
