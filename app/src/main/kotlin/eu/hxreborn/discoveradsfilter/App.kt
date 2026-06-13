package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.Context
import android.util.Log
import eu.hxreborn.discoveradsfilter.prefs.PrefsRepository
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.recovery.AutoRecovery
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class App :
    Application(),
    XposedServiceHelper.OnServiceListener {
    @Volatile
    private var mService: XposedService? = null

    lateinit var prefsRepository: PrefsRepository
        private set

    private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recoveryLock = Any()
    private var recoveryInFlight = false
    private var lastRecoveryVersion = 0L

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

    fun onRecoveryRequested(agsaVersionCode: Long): Boolean {
        if (!::prefsRepository.isInitialized) return false
        if (!prefsRepository.read(SettingsPrefs.autoRecoveryOnUpdate)) {
            Log.d(TAG, "recovery ignored (disabled) v=$agsaVersionCode")
            return false
        }
        synchronized(recoveryLock) {
            if (recoveryInFlight) {
                Log.d(TAG, "recovery already running v=$agsaVersionCode")
                return false
            }
            if (agsaVersionCode != 0L && agsaVersionCode == lastRecoveryVersion) {
                Log.d(TAG, "recovery already attempted v=$agsaVersionCode")
                return false
            }
            recoveryInFlight = true
        }
        appScope.launch {
            try {
                AutoRecovery.run(this@App, prefsRepository, agsaVersionCode)
            } catch (t: Throwable) {
                Log.e(TAG, "recovery crashed v=$agsaVersionCode", t)
            } finally {
                synchronized(recoveryLock) {
                    lastRecoveryVersion = agsaVersionCode
                    recoveryInFlight = false
                }
            }
        }
        return true
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter/App"

        fun from(context: Context): App = context.applicationContext as App
    }
}
