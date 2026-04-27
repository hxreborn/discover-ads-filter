package eu.hxreborn.discoveradsfilter.hook

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider
import eu.hxreborn.discoveradsfilter.util.Logger

object HookMetrics {
    @SuppressLint("UseKtx")
    private val providerUri = Uri.parse("content://${MetricsProvider.AUTHORITY}")

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
            Logger.w("provider increment failed: ${it.message}")
        }
    }

    private fun context(): Context? {
        appContext?.let { return it }

        @Suppress("PrivateApi")
        return runCatching {
            Class
                .forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Context
        }.getOrNull()?.let { it.applicationContext ?: it }?.also { appContext = it }
    }
}
