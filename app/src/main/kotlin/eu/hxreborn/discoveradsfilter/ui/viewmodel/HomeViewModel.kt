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
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.ScanOrigin
import eu.hxreborn.discoveradsfilter.ui.state.ScanStep
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.util.isLauncherIconVisible
import eu.hxreborn.discoveradsfilter.util.setLauncherIconVisible
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val app: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val appInstance = App.from(app)
    private val repo = appInstance.prefsRepository

    private val verboseFlow = MutableStateFlow(false)
    private val filterEnabledFlow = MutableStateFlow(true)
    private val launcherIconHiddenFlow = MutableStateFlow(false)
    private val verifyFlow = MutableStateFlow<VerifyUiState?>(null)
    private val moduleStatusFlow = MutableStateFlow(ModuleStatus.Inactive)

    private val serviceListener =
        object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                moduleStatusFlow.value = ModuleStatus.Active
            }

            override fun onServiceDied(service: XposedService) {
                moduleStatusFlow.value = ModuleStatus.Inactive
            }
        }

    // A decorative counter must never be able to freeze the whole screen, so a
    // throw in the cross-process prefs flow falls back to 0 instead of killing combine.
    private val adsHiddenFlow = repo.adsHiddenFlow().catch { emit(0L) }

    private val verifyWithStatusFlow =
        combine(verifyFlow, moduleStatusFlow) { verify, status ->
            verify?.copy(moduleStatus = status)
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
            verboseFlow,
            filterEnabledFlow,
            launcherIconHiddenFlow,
            verifyWithStatusFlow,
            adsHiddenFlow,
        ) { verbose, filterEnabled, launcherIconHidden, verify, adsHidden ->
            if (verify == null) {
                HomeUiState.Loading
            } else {
                HomeUiState.Ready(
                    verbose = verbose,
                    filterEnabled = filterEnabled,
                    isLauncherIconHidden = launcherIconHidden,
                    verify = verify.copy(adsHidden = adsHidden),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    val actions: HomeActions =
        HomeActions(
            onVerboseChange = { value ->
                repo.save(SettingsPrefs.verbose, value)
                verboseFlow.value = value
            },
            onFilterEnabledChange = { value ->
                repo.save(SettingsPrefs.filterEnabled, value)
                filterEnabledFlow.value = value
            },
            onLauncherIconHiddenChange = { hidden ->
                setLauncherIconVisible(app, !hidden)
                launcherIconHiddenFlow.value = hidden
            },
            onVerify = ::verify,
            onClearCacheOnly = ::clearCacheOnly,
            onResetAdsCounter = ::resetAdsCounter,
        )

    init {
        appInstance.addServiceListener(serviceListener)
        viewModelScope.launch(ioDispatcher) {
            runCatching { launcherIconHiddenFlow.value = !isLauncherIconVisible(app) }
                .onFailure { Log.w(TAG, "launcher icon state read failed", it) }
            runCatching { initialize() }.onFailure { t ->
                Log.e(TAG, "initialization failed", t)
                verifyFlow.update { it ?: VerifyUiState(phase = VerifyPhase.Idle) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appInstance.removeServiceListener(serviceListener)
    }

    private suspend fun initialize() {
        verboseFlow.value = repo.read(SettingsPrefs.verbose)
        filterEnabledFlow.value = repo.read(SettingsPrefs.filterEnabled)

        val lastScan = repo.readLastScan()
        val result = lastScan?.let { VerifyResult.Success(it.versionCode, it.targets) }

        // Publish a usable state from the fast local reads before the PackageManager
        // lookup below, which can stall while the system finishes an AGSA update.
        // This is what keeps the screen from sitting on the bare Loading card.
        verifyFlow.value =
            VerifyUiState(
                phase = VerifyPhase.Idle,
                lastResult = result,
                scanModuleVersion = lastScan?.moduleVersionCode ?: 0,
            )

        val agsaPkg = currentAgsaPackageInfo()
        val agsaUpdated = result != null && agsaPkg != null && agsaPkg.versionCode != result.versionCode
        val moduleUpdated = lastScan != null && lastScan.moduleVersionCode != BuildConfig.VERSION_CODE
        val needsScan = result == null || agsaUpdated || moduleUpdated
        val origin = if (result != null) ScanOrigin.Background else ScanOrigin.Startup

        verifyFlow.update { current ->
            current?.copy(
                phase = if (needsScan) VerifyPhase.Running else VerifyPhase.Idle,
                scanOrigin = if (needsScan) origin else null,
                installedAgsaVersion = agsaPkg?.versionCode,
                installedAgsaVersionName = agsaPkg?.versionName,
                installedAgsaLastUpdateTime = agsaPkg?.lastUpdateTime ?: 0L,
            )
        }

        if (needsScan) runScanAndUpdate()
    }

    private fun resetAdsCounter() {
        viewModelScope.launch(ioDispatcher) {
            repo.resetAdsCounter()
        }
    }

    private fun clearCacheOnly() {
        viewModelScope.launch(ioDispatcher) {
            repo.clearScanCache()
            verifyFlow.update {
                it?.copy(
                    lastResult = null,
                    scanProgress = emptyList(),
                    scanModuleVersion = 0,
                )
            }
        }
    }

    private fun verify() {
        if (!transitionToManualScan()) return
        viewModelScope.launch { runScanAndUpdate() }
    }

    private fun transitionToManualScan(): Boolean {
        val current = verifyFlow.value ?: return false
        if (current.phase == VerifyPhase.Running) return false
        verifyFlow.update {
            it?.copy(
                phase = VerifyPhase.Running,
                scanOrigin = ScanOrigin.Manual,
                scanProgress = emptyList(),
            )
        }
        return true
    }

    private suspend fun runScanAndUpdate() {
        if (verboseFlow.value) Log.d(TAG, "scan started")
        val steps = mutableListOf<ScanStep>()
        val startTime = System.currentTimeMillis()
        val result =
            runCatching { scan(steps) }.getOrElse { t ->
                Log.e(TAG, "scan threw", t)
                VerifyResult.Failure("Unexpected exception", t.message)
            }
        val elapsed = System.currentTimeMillis() - startTime
        val installed = withContext(ioDispatcher) { currentAgsaPackageInfo() }
        val origin = verifyFlow.value?.scanOrigin
        verifyFlow.update { current ->
            val preserved =
                when (result) {
                    is VerifyResult.Success -> result
                    is VerifyResult.Failure -> current?.lastResult ?: result
                }
            val refreshError =
                if (origin == ScanOrigin.Background && result is VerifyResult.Failure) {
                    result.reason
                } else {
                    null
                }
            current?.copy(
                phase = VerifyPhase.Idle,
                lastResult = preserved,
                scanDurationMs = elapsed,
                lastRefreshError = refreshError,
                installedAgsaVersion = installed?.versionCode,
                installedAgsaVersionName = installed?.versionName,
                installedAgsaLastUpdateTime = installed?.lastUpdateTime ?: 0L,
                scanModuleVersion = if (result is VerifyResult.Success) BuildConfig.VERSION_CODE else current.scanModuleVersion,
            )
        }
    }

    private suspend fun scan(steps: MutableList<ScanStep>): VerifyResult =
        withContext(ioDispatcher) {
            val agsaInfo =
                runCatching {
                    app.packageManager.getApplicationInfo(
                        DiscoverAdsFilterModule.AGSA_PKG,
                        0,
                    )
                }.getOrElse { t ->
                    if (verboseFlow.value) {
                        Log.d(
                            TAG,
                            "AGSA lookup failed: ${t.javaClass.simpleName}: ${t.message}",
                        )
                    }
                    return@withContext VerifyResult.Failure(
                        reason = "AGSA not installed on this device",
                        detail = t.message,
                    )
                }
            val apkPath =
                agsaInfo.sourceDir ?: return@withContext VerifyResult.Failure(
                    "AGSA ApplicationInfo.sourceDir was null",
                )

            val versionCode =
                runCatching {
                    app.packageManager.getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0).longVersionCode
                }.getOrNull() ?: 0L

            if (verboseFlow.value) {
                Log.d(TAG, "DexKit scan: agsaV=$versionCode apk=$apkPath")
            }

            val resolved =
                DexKitResolver.resolveAll(apkPath) { name, value ->
                    steps.add(ScanStep(name, value, value != null))
                    verifyFlow.update {
                        it?.copy(scanProgress = steps.toList())
                    }
                }

            if (verboseFlow.value) {
                Log.d(TAG, "DexKit finished: ${resolved.summary()}")
            }

            when (resolved) {
                is ResolvedTargets.Resolved -> {
                    repo.writeResolvedTargets(versionCode, resolved)
                    VerifyResult.Success(versionCode, resolved)
                }

                is ResolvedTargets.Missing -> {
                    VerifyResult.Failure(
                        reason = "Signatures not resolved",
                        detail = resolved.reason,
                    )
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
