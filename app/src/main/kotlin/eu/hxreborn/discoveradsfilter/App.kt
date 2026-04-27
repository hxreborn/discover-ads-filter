package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(
            object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    Log.i(
                        TAG,
                        "service bound: ${service.frameworkName} v${service.frameworkVersion}",
                    )
                    boundService = service
                    remotePrefs = service.getRemotePreferences(SettingsPrefs.GROUP)
                    listeners.forEach { it.onServiceBind(service) }
                }

                override fun onServiceDied(service: XposedService) {
                    Log.w(TAG, "service died")
                    boundService = null
                    remotePrefs = null
                    listeners.forEach { it.onServiceDied(service) }
                }
            },
        )
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter/App"

        @Volatile
        var boundService: XposedService? = null
            private set

        @Volatile
        var remotePrefs: SharedPreferences? = null
            private set

        private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

        fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.add(listener)
            boundService?.let { listener.onServiceBind(it) }
        }

        fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.remove(listener)
        }
    }
}
