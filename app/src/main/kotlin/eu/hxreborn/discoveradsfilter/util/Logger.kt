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

    fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            module.log(Log.ERROR, TAG, message, throwable)
        } else {
            module.log(Log.INFO, TAG, message)
        }
    }

    inline fun logDebug(message: () -> String) = v(message)

    fun i(msg: String) {
        module.log(Log.INFO, TAG, msg)
    }

    fun w(msg: String) {
        module.log(Log.WARN, TAG, msg)
    }

    fun e(
        msg: String,
        t: Throwable? = null,
    ) {
        if (t != null) {
            module.log(Log.ERROR, TAG, msg, t)
        } else {
            module.log(Log.ERROR, TAG, msg)
        }
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

fun log(
    message: String,
    throwable: Throwable? = null,
): Unit = Logger.log(message, throwable)

inline fun logDebug(message: () -> String): Unit = Logger.logDebug(message)
