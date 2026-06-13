package eu.hxreborn.discoveradsfilter.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import eu.hxreborn.discoveradsfilter.util.Logger

object HookToast {
    fun show(
        ctx: Context,
        message: String,
    ) {
        runCatching {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
            }
        }.onFailure { Logger.log(Log.WARN, "toast failed: ${it.message}") }
    }
}
