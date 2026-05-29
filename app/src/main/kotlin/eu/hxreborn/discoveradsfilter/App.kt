package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.Context
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.prefs.SettingsRepository
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class App :
    Application(),
    XposedServiceHelper.OnServiceListener {
    @Volatile
    var boundService: XposedService? = null
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    private val listeners =
        java.util.concurrent.CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

    override fun onCreate() {
        super.onCreate()
        settingsRepository =
            SettingsRepository(
                local = getSharedPreferences(SettingsPrefs.GROUP, Context.MODE_PRIVATE),
                remoteProvider = { boundService?.getRemotePreferences(SettingsPrefs.GROUP) },
            )
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        Log.i(TAG, "service bound: ${service.frameworkName} v${service.frameworkVersion}")
        boundService = service
        settingsRepository.syncToRemote()
        listeners.forEach { it.onServiceBind(service) }
    }

    override fun onServiceDied(service: XposedService) {
        Log.w(TAG, "service died")
        boundService = null
        listeners.forEach { it.onServiceDied(service) }
    }

    fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.add(listener)
        boundService?.let { listener.onServiceBind(it) }
    }

    fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter/App"

        fun from(context: Context): App = context.applicationContext as App
    }
}
