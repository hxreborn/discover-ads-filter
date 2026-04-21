package eu.hxreborn.discoveradsfilter.util

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs

object Logger {
    @PublishedApi
    internal const val TAG = DiscoverAdsFilterModule.TAG

    inline fun v(msg: () -> String) {
        if (!verboseEnabled()) return
        module.log(Log.DEBUG, TAG, msg())
    }

    fun i(msg: String) {
        module.log(Log.INFO, TAG, msg)
    }

    fun w(msg: String) {
        module.log(Log.WARN, TAG, msg)
    }

    @PublishedApi
    internal fun verboseEnabled(): Boolean =
        cachedPrefs()?.let { SettingsPrefs.verbose.read(it) } == true

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun cachedPrefs(): SharedPreferences? {
        prefs?.let { return it }
        return runCatching { module.getRemotePreferences(SettingsPrefs.GROUP) }
            .getOrNull()
            ?.also { prefs = it }
    }
}
