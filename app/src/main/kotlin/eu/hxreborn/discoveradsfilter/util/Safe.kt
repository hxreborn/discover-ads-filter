package eu.hxreborn.discoveradsfilter.util

import android.util.Log
import eu.hxreborn.discoveradsfilter.module

object Safe {
    inline fun run(
        tag: String,
        what: String,
        block: () -> Unit,
    ) {
        runCatching(block).onFailure { t -> logFailure(tag, what, t) }
    }

    inline fun <T> compute(
        tag: String,
        what: String,
        block: () -> T,
    ): T? =
        runCatching(block).getOrElse { t ->
            logFailure(tag, what, t)
            null
        }

    @PublishedApi
    internal fun logFailure(
        tag: String,
        what: String,
        t: Throwable,
    ) {
        try {
            module.log(Log.ERROR, tag, "failed: $what", t)
        } catch (_: UninitializedPropertyAccessException) {
            Log.e(tag, "failed before module init: $what", t)
        }
    }
}
