package eu.hxreborn.discoveradsfilter.util

import android.util.Log
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.hook.verbose
import eu.hxreborn.discoveradsfilter.module

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
    internal fun verboseEnabled(): Boolean = verbose
}
