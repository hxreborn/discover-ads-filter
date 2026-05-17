package eu.hxreborn.discoveradsfilter.util

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs

object Logger {
    @PublishedApi
    internal const val TAG = DiscoverAdsFilterModule.TAG

    fun log(
        level: Int,
        msg: String,
        t: Throwable? = null,
    ) = if (t != null) module.log(level, TAG, msg, t) else module.log(level, TAG, msg)

    inline fun debug(msg: () -> String) {
        if (verboseEnabled()) module.log(Log.DEBUG, TAG, msg())
    }

    @PublishedApi
    internal fun verboseEnabled(): Boolean =
        cachedPrefs()?.let { SettingsPrefs.verbose.read(it) } == true

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun cachedPrefs(): SharedPreferences? =
        prefs ?: runCatching { module.getRemotePreferences(SettingsPrefs.GROUP) }
            .getOrNull()
            ?.also { prefs = it }
}
