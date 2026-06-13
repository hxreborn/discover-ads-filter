package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.Logger

internal object MetricsClient {
    private val uri = "content://${MetricsProvider.AUTHORITY}".toUri()

    fun call(
        ctx: Context,
        method: String,
        extras: Bundle? = null,
    ): Bundle? =
        runCatching { ctx.contentResolver.call(uri, method, null, extras) }
            .onFailure { Logger.log(Log.WARN, "metrics $method failed: ${it.message}") }
            .getOrNull()
}
