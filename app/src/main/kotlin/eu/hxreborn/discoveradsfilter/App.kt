package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.Context
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.PrefsRepository
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

class App :
    Application(),
    XposedServiceHelper.OnServiceListener {
    @Volatile
    private var mService: XposedService? = null

    lateinit var prefsRepository: PrefsRepository
        private set

    private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

    override fun onCreate() {
        super.onCreate()
        prefsRepository =
            PrefsRepository(
                local = getSharedPreferences(SettingsPrefs.GROUP, Context.MODE_PRIVATE),
                remoteProvider = { mService?.getRemotePreferences(SettingsPrefs.GROUP) },
            )
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        Log.i(TAG, "service bound: ${service.frameworkName} v${service.frameworkVersion}")
        mService = service
        prefsRepository.syncToRemote()
        listeners.forEach { it.onServiceBind(service) }
    }

    override fun onServiceDied(service: XposedService) {
        Log.w(TAG, "service died")
        mService = null
        listeners.forEach { it.onServiceDied(service) }
    }

    fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.add(listener)
        mService?.let { listener.onServiceBind(it) }
    }

    fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter/App"

        fun from(context: Context): App = context.applicationContext as App
    }
}
