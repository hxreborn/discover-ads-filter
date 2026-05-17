package eu.hxreborn.discoveradsfilter.util

import android.util.Log

object Safe {
    inline fun run(
        op: String,
        block: () -> Unit,
    ) {
        runCatching(block).onFailure { t -> logFailure(op, t) }
    }

    @PublishedApi
    internal fun logFailure(
        op: String,
        t: Throwable,
    ) = Logger.log(Log.ERROR, "failed: $op", t)
}
