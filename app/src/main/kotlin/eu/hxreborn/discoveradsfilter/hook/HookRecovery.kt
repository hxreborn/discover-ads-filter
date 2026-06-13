package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.os.Bundle
import eu.hxreborn.discoveradsfilter.provider.MetricsProvider

object HookRecovery {
    fun request(
        ctx: Context,
        versionCode: Long,
    ): Boolean {
        val result =
            MetricsClient.call(
                ctx,
                MetricsProvider.METHOD_REQUEST_RECOVERY,
                Bundle().apply { putLong(MetricsProvider.KEY_VERSION, versionCode) },
            )
        return result?.getBoolean(MetricsProvider.KEY_FIRST, false) ?: false
    }
}
