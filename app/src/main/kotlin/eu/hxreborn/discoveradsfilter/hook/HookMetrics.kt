package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.CurrentApp
import eu.hxreborn.discoveradsfilter.util.Logger

object HookMetrics {
    private val providerUri = "content://${MetricsProvider.AUTHORITY}".toUri()

    @Volatile
    private var appContext: Context? = null

    fun addAdsHidden(count: Int) {
        if (count <= 0) return
        val ctx = context() ?: return

        runCatching {
            ctx.contentResolver.call(
                providerUri,
                MetricsProvider.METHOD_INCREMENT,
                null,
                Bundle().apply {
                    putInt(MetricsProvider.KEY_COUNT, count)
                },
            )
        }.onFailure {
            Logger.log(Log.WARN, "provider increment failed: ${it.message}")
        }
    }

    private fun context(): Context? {
        appContext?.let { return it }
        return CurrentApp.get()?.let { it.applicationContext ?: it }?.also { appContext = it }
    }
}
