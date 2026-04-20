package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
)

@Immutable
data class VerifyUiState(
    val phase: VerifyPhase = VerifyPhase.Idle,
    val lastResult: VerifyResult? = null,
    val installedAgsaVersion: Long? = null,
    val xposedServiceBound: Boolean = false,
    val scanModuleVersion: Int = 0,
    val hookInstallStatus: String? = null,
    val hookProcess: String? = null,
    val adsHidden: Long = 0,
)

enum class VerifyPhase { Idle, Running }

enum class HookCoverage { Full, FallbackOnly, None, NotScanned, ScanFailed }

fun VerifyUiState.hookCoverage(): HookCoverage =
    when (val r = lastResult) {
        null -> {
            HookCoverage.NotScanned
        }

        is VerifyResult.Failure -> {
            HookCoverage.ScanFailed
        }

        is VerifyResult.Success -> {
            val t = r.targets
            val hasProto = t.adMetadataClass != null && t.feedCardClass != null
            val hasProcessors = t.cardProcessorMethods.isNotEmpty()
            when {
                hasProto && hasProcessors -> HookCoverage.Full
                hasProto -> HookCoverage.FallbackOnly
                else -> HookCoverage.None
            }
        }
    }

fun VerifyUiState.moduleUpdatedSinceScan(currentModuleVersion: Int): Boolean {
    if (scanModuleVersion == 0) return false
    return scanModuleVersion != currentModuleVersion
}

fun VerifyUiState.agsaUpdatedSinceScan(): Boolean {
    val result = lastResult as? VerifyResult.Success ?: return false
    val installed = installedAgsaVersion ?: return false
    return installed != result.versionCode
}

@Immutable
sealed interface VerifyResult {
    @Immutable
    data class Success(
        val versionCode: Long,
        val targets: ResolvedTargets.Resolved,
    ) : VerifyResult

    @Immutable
    data class Failure(
        val reason: String,
        val detail: String? = null,
    ) : VerifyResult
}
