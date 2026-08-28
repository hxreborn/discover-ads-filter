package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.filter.CompiledRule
import eu.hxreborn.discoveradsfilter.filter.NewsFilter
import eu.hxreborn.discoveradsfilter.filter.NewsRules
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger

@Volatile
internal var verbose: Boolean = false

@Volatile
internal var filterAds: Boolean = true

@Volatile
internal var shareOriginalLink: Boolean = false

@Volatile
internal var shareStripSourceLine: Boolean = false

@Volatile
internal var shareCustomLine: String = ""

@Volatile
internal var newsRules: List<CompiledRule> = emptyList()

private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

internal fun loadHookPrefs(prefs: SharedPreferences) {
    verbose = SettingsPrefs.verbose.read(prefs)
    filterAds = SettingsPrefs.filterAds.read(prefs)
    shareOriginalLink = SettingsPrefs.shareOriginalLink.read(prefs)
    shareStripSourceLine = SettingsPrefs.shareStripSourceLine.read(prefs)
    shareCustomLine = SettingsPrefs.shareCustomLine.read(prefs).orEmpty()
    newsRules = compileNewsRules(prefs)
    if (prefListener == null) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { src, key ->
                when (key) {
                    null -> {
                        loadHookPrefs(src)
                    }

                    SettingsPrefs.verbose.key -> {
                        verbose = SettingsPrefs.verbose.read(src)
                        if (verbose) StreamSliceFilterHook.resetKeyDump()
                    }

                    SettingsPrefs.filterAds.key -> {
                        filterAds = SettingsPrefs.filterAds.read(src)
                        StreamSliceFilterHook.resetFilterState()
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

                    SettingsPrefs.newsRules.key -> {
                        newsRules = compileNewsRules(src)
                        StreamSliceFilterHook.resetFilterState()
                    }
                }
            }
        runCatching { prefs.registerOnSharedPreferenceChangeListener(listener) }
            .onSuccess { prefListener = listener }
            .onFailure { Logger.log(Log.WARN, "failed to register pref listener: ${it.message}") }
    }
}

private fun compileNewsRules(prefs: SharedPreferences): List<CompiledRule> {
    val raw = SettingsPrefs.newsRules.read(prefs)
    if (raw.isNullOrBlank()) return emptyList()
    val rules = NewsRules.decode(raw)
    if (rules.isEmpty() && raw.trim() != "[]") {
        Logger.log(Log.WARN, "news rules could not be read, nothing will be filtered")
    }
    return NewsFilter.compile(rules)
}
