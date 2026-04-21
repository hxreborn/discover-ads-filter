package eu.hxreborn.discoveradsfilter.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.hxreborn.discoveradsfilter.App
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.discovery.DexKitResolver
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.prefs.SettingsRepository
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class HomeViewModel(
    private val app: Application,
) : ViewModel() {
    private val repo =
        SettingsRepository(
            context = app,
            remotePrefsProvider = { App.remotePrefs },
        )

    private val verboseFlow = MutableStateFlow(false)
    private val verifyFlow = MutableStateFlow<VerifyUiState?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(verboseFlow, verifyFlow) { verbose, verify ->
            if (verify == null) {
                HomeUiState.Loading
            } else {
                HomeUiState.Ready(verbose = verbose, verify = verify)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    val actions: HomeActions =
        HomeActions(
            onVerboseChange = { value ->
                repo.setVerbose(value)
                verboseFlow.value = value
            },
            onFilterEnabledChange = { value ->
                repo.setFilterEnabled(value)
                verifyFlow.update { it?.copy(filterEnabled = value) }
            },
            onVerify = ::verify,
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = repo.snapshot()
            verboseFlow.value = snapshot.verbose
            val agsaPkg = currentAgsaPackageInfo()
            val lastScan = repo.readLastScan(agsaPkg?.versionCode ?: 0L)
            val hookStatus = repo.readHookStatus()
            val hookProcess = repo.readHookProcess()
            val adsHidden = repo.readAdsHidden()

            val result = lastScan?.let { VerifyResult.Success(it.versionCode, it.targets) }
            verifyFlow.value =
                VerifyUiState(
                    lastResult = result,
                    installedAgsaVersion = agsaPkg?.versionCode,
                    installedAgsaVersionName = agsaPkg?.versionName,
                    installedAgsaLastUpdateTime = agsaPkg?.lastUpdateTime ?: 0L,
                    scanModuleVersion = lastScan?.moduleVersionCode ?: 0,
                    hookInstallStatus = hookStatus,
                    hookProcess = hookProcess,
                    adsHidden = adsHidden,
                    filterEnabled = snapshot.filterEnabled,
                )
        }
    }

    fun onServiceBound() {
        verifyFlow.update { it?.copy(xposedServiceBound = true) }
        viewModelScope.launch(Dispatchers.IO) {
            repo.syncLocalToRemote()
            val hookStatus = repo.readHookStatus()
            val hookProcess = repo.readHookProcess()
            val adsHidden = repo.readAdsHidden()
            verifyFlow.update { current ->
                current?.copy(
                    hookInstallStatus = hookStatus ?: current.hookInstallStatus,
                    hookProcess = hookProcess ?: current.hookProcess,
                    adsHidden = maxOf(adsHidden, current.adsHidden),
                )
            }
        }
    }

    fun onServiceDied() {
        verifyFlow.update { it?.copy(xposedServiceBound = false) }
    }

    private fun verify() {
        val current = verifyFlow.value ?: return
        if (current.phase == VerifyPhase.Running) return
        verifyFlow.update { it?.copy(phase = VerifyPhase.Running) }
        viewModelScope.launch {
            if (verboseFlow.value) {
                Log.d(TAG, "scan started")
            }
            val result =
                runCatching { scan() }.getOrElse { t ->
                    Log.e(TAG, "scan threw", t)
                    VerifyResult.Failure("Unexpected exception", t.message)
                }
            val installed = withContext(Dispatchers.IO) { currentAgsaPackageInfo() }
            verifyFlow.update { current ->
                val preserved =
                    when (result) {
                        is VerifyResult.Success -> result
                        is VerifyResult.Failure -> current?.lastResult ?: result
                    }
                current?.copy(
                    phase = VerifyPhase.Idle,
                    lastResult = preserved,
                    installedAgsaVersion = installed?.versionCode,
                    installedAgsaVersionName = installed?.versionName,
                    installedAgsaLastUpdateTime = installed?.lastUpdateTime ?: 0L,
                    scanModuleVersion =
                        if (result is VerifyResult.Success) {
                            BuildConfig.VERSION_CODE
                        } else {
                            current.scanModuleVersion
                        },
                )
            }
        }
    }

    private suspend fun scan(): VerifyResult =
        withContext(Dispatchers.IO) {
            val app = app
            val agsaInfo =
                runCatching {
                    app.packageManager.getApplicationInfo(DiscoverAdsFilterModule.AGSA_PKG, 0)
                }.getOrElse { t ->
                    if (verboseFlow.value) {
                        Log.d(TAG, "AGSA ApplicationInfo lookup failed: ${t.javaClass.simpleName}: ${t.message}")
                    }
                    return@withContext VerifyResult.Failure(
                        reason = "AGSA not installed on this device",
                        detail = t.message,
                    )
                }
            val apkPath = agsaInfo.sourceDir ?: return@withContext VerifyResult.Failure("AGSA ApplicationInfo.sourceDir was null")

            val versionCode =
                runCatching {
                    app.packageManager.getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0).longVersionCode
                }.getOrNull() ?: 0L

            if (verboseFlow.value) {
                Log.d(TAG, "DexKit scan: agsaV=$versionCode apk=$apkPath")
            }

            var resolved: ResolvedTargets
            val elapsedMs =
                measureTimeMillis {
                    resolved = DexKitResolver.resolveAll(apkPath)
                }
            if (verboseFlow.value) {
                Log.d(TAG, "DexKit finished in ${elapsedMs}ms: ${resolved.summary()}")
            }

            when (resolved) {
                is ResolvedTargets.Resolved -> {
                    repo.writeResolvedTargets(versionCode, resolved)
                    VerifyResult.Success(versionCode, resolved)
                }

                is ResolvedTargets.Missing -> {
                    VerifyResult.Failure(reason = "Signatures not resolved", detail = resolved.reason)
                }
            }
        }

    private data class AgsaPackageInfo(
        val versionCode: Long,
        val versionName: String?,
        val lastUpdateTime: Long,
    )

    private fun currentAgsaPackageInfo(): AgsaPackageInfo? {
        val pm = app.packageManager
        return runCatching {
            val info = pm.getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0)
            AgsaPackageInfo(
                versionCode = info.longVersionCode,
                versionName = info.versionName,
                lastUpdateTime = info.lastUpdateTime,
            )
        }.getOrNull()
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter"

        val Factory =
            viewModelFactory {
                initializer { HomeViewModel(this[APPLICATION_KEY] as Application) }
            }
    }
}
