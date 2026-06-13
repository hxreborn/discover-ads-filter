package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.os.Bundle
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.CurrentApp

object HookMetrics {
    @Volatile
    private var appContext: Context? = null

    fun addAdsHidden(count: Int) {
        if (count <= 0) return
        val ctx = context() ?: return
        MetricsClient.call(
            ctx,
            MetricsProvider.METHOD_INCREMENT,
            Bundle().apply { putInt(MetricsProvider.KEY_COUNT, count) },
        )
    }

    private fun context(): Context? {
        appContext?.let { return it }
        return CurrentApp.get()?.let { it.applicationContext ?: it }?.also { appContext = it }
    }
}
