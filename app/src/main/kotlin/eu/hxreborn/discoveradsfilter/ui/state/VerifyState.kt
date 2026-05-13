package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets

@Immutable
data class VerifyUiState(
    val phase: VerifyPhase = VerifyPhase.Idle,
    val lastResult: VerifyResult? = null,
    val installedAgsaVersion: Long? = null,
    val installedAgsaVersionName: String? = null,
    val installedAgsaLastUpdateTime: Long = 0,
    val scanModuleVersion: Int = 0,
    val adsHidden: Long = 0,
    val moduleStatus: ModuleStatus = ModuleStatus.Inactive,
    val scanOrigin: ScanOrigin? = null,
    val scanProgress: List<ScanStep> = emptyList(),
    val scanDurationMs: Long = 0,
    val lastRefreshError: String? = null,
) {
    companion object {
        const val TOTAL_TARGETS = 7
    }
}

enum class VerifyPhase { Idle, Running }

enum class ScanOrigin { Startup, Background, Manual }

@Immutable
data class ScanStep(
    val label: String,
    val rawValue: String?,
    val resolved: Boolean,
)

fun VerifyUiState.moduleUpdatedSinceScan(currentModuleVersion: Int): Boolean =
    scanModuleVersion != 0 && scanModuleVersion != currentModuleVersion

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
