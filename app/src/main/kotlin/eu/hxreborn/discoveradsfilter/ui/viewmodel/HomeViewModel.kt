package eu.hxreborn.discoveradsfilter.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.hxreborn.discoveradsfilter.App
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.discovery.DexKitResolver
import eu.hxreborn.discoveradsfilter.discovery.KnownGoodEntry
import eu.hxreborn.discoveradsfilter.discovery.KnownGoodRegistry
import eu.hxreborn.discoveradsfilter.discovery.RegistryStatus
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.prefs.SettingsRepository
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.HookCoverage
import eu.hxreborn.discoveradsfilter.ui.state.KnownGoodUiState
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.hookCoverage
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
    application: Application,
) : AndroidViewModel(application) {
    private val repo =
        SettingsRepository(
            context = application,
            remotePrefsProvider = { App.remotePrefs },
        )

    private val verboseFlow = MutableStateFlow(false)
    private val verifyFlow = MutableStateFlow<VerifyUiState?>(null)
    private val knownGoodFlow = MutableStateFlow(KnownGoodUiState())

    val uiState: StateFlow<HomeUiState> =
        combine(verboseFlow, verifyFlow, knownGoodFlow) { verbose, verify, kg ->
            if (verify == null) {
                HomeUiState.Loading
            } else {
                HomeUiState.Ready(verbose = verbose, verify = verify, knownGood = kg)
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
            onForgetKnownGood = ::forgetKnownGood,
            onOpenSource = ::openSource,
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = repo.snapshot()
            verboseFlow.value = snapshot.verbose
            val lastScan = repo.readLastScan()
            val agsaPkg = currentAgsaPackageInfo()
            val hookStatus = repo.readHookStatus()
            val hookProcess = repo.readHookProcess()
            val adsHidden = repo.readAdsHidden()
            val kg = repo.readKnownGood()
            knownGoodFlow.value = KnownGoodUiState(bundled = kg.bundled, local = kg.local)

            val result = lastScan?.let { VerifyResult.Success(it.versionCode, it.targets) }
            val registryStatus =
                KnownGoodRegistry.status(
                    agsaVersionCode = agsaPkg?.versionCode ?: 0L,
                    agsaLastUpdateTime = agsaPkg?.lastUpdateTime ?: 0L,
                    moduleVersionCode = BuildConfig.VERSION_CODE,
                    targets = (result as? VerifyResult.Success)?.targets,
                    entries = kg.all(),
                )
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
                    registryStatus = registryStatus,
                    filterEnabled = snapshot.filterEnabled,
                )
            maybeAutoVerify()
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
            maybeAutoVerify()
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
            val entries = knownGoodFlow.value.bundled + knownGoodFlow.value.local
            verifyFlow.update { current ->
                val preserved =
                    when (result) {
                        is VerifyResult.Success -> result
                        is VerifyResult.Failure -> current?.lastResult ?: result
                    }
                val targets = (preserved as? VerifyResult.Success)?.targets
                val registryStatus =
                    KnownGoodRegistry.status(
                        agsaVersionCode = installed?.versionCode ?: 0L,
                        agsaLastUpdateTime = installed?.lastUpdateTime ?: 0L,
                        moduleVersionCode = BuildConfig.VERSION_CODE,
                        targets = targets,
                        entries = entries,
                    )
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
                    registryStatus = registryStatus,
                )
            }
            maybeAutoVerify()
        }
    }

    // Auto-mark known-good when the hook proved it actually works:
    // coverage is Full, at least one ad was filtered in the current install.
    private fun maybeAutoVerify() {
        val v = verifyFlow.value ?: return
        if (v.registryStatus != RegistryStatus.Unverified) return
        if (v.adsHidden <= 0L) return
        if (v.hookCoverage() != HookCoverage.Full) return
        val result = v.lastResult as? VerifyResult.Success ?: return
        val agsaVersion = v.installedAgsaVersion ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val entry =
                KnownGoodEntry(
                    agsaVersionCode = agsaVersion,
                    agsaVersionName = v.installedAgsaVersionName,
                    agsaLastUpdateTime = v.installedAgsaLastUpdateTime,
                    moduleVersionCode = BuildConfig.VERSION_CODE,
                    moduleVersionName = BuildConfig.VERSION_NAME,
                    targetsHash = KnownGoodRegistry.hash(result.targets),
                    verifiedAt = System.currentTimeMillis(),
                    source = KnownGoodEntry.Source.Local,
                )
            val updated = repo.addKnownGood(entry)
            knownGoodFlow.update { it.copy(bundled = updated.bundled, local = updated.local) }
            verifyFlow.update { it?.copy(registryStatus = RegistryStatus.Verified) }
        }
    }

    private fun openSource() {
        val app = getApplication<Application>()
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { app.startActivity(intent) }.onFailure {
            Log.w(TAG, "failed to open source", it)
        }
    }

    private fun forgetKnownGood(entry: KnownGoodEntry) {
        if (entry.source != KnownGoodEntry.Source.Local) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = repo.removeKnownGood(entry.agsaVersionCode, entry.moduleVersionCode, entry.targetsHash)
            knownGoodFlow.update { it.copy(bundled = updated.bundled, local = updated.local) }
            verifyFlow.update { current ->
                current?.let {
                    val targets = (it.lastResult as? VerifyResult.Success)?.targets
                    val status =
                        KnownGoodRegistry.status(
                            agsaVersionCode = it.installedAgsaVersion ?: 0L,
                            agsaLastUpdateTime = it.installedAgsaLastUpdateTime,
                            moduleVersionCode = BuildConfig.VERSION_CODE,
                            targets = targets,
                            entries = updated.all(),
                        )
                    it.copy(registryStatus = status)
                }
            }
        }
    }

    private suspend fun scan(): VerifyResult =
        withContext(Dispatchers.IO) {
            val app = getApplication<Application>()
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

            var resolvedTargets: ResolvedTargets? = null
            val elapsedMs =
                measureTimeMillis {
                    resolvedTargets = DexKitResolver.resolveAll(apkPath)
                }
            if (verboseFlow.value && resolvedTargets != null) {
                Log.d(TAG, "DexKit finished in ${elapsedMs}ms: ${resolvedTargets!!.summary()}")
            }

            when (val resolved = resolvedTargets!!) {
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
        val pm = getApplication<Application>().packageManager
        return runCatching {
            val info = pm.getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0)
            AgsaPackageInfo(
                versionCode = info.longVersionCode,
                versionName = info.versionName,
                lastUpdateTime = info.lastUpdateTime,
            )
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "DiscoverAdsFilter"
        private const val SOURCE_URL = "https://github.com/hxreborn/discover-ads-filter"
    }
}
