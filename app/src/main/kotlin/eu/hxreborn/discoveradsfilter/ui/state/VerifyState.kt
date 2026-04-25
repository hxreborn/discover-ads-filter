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
    val hookStatus: HookStatus? = null,
    val hookProcess: String? = null,
    val adsHidden: Long = 0,
    val moduleStatus: ModuleStatus = ModuleStatus.Unknown,
    val scanOrigin: ScanOrigin? = null,
    val scanProgress: List<ScanStep> = emptyList(),
    val scanDurationMs: Long = 0,
    val lastRefreshError: String? = null,
) {
    val hookInstalled: Int get() = hookStatus?.installed ?: 0
    val hookTotal: Int get() = hookStatus?.total ?: 0

    val resolvedTargetCount: Int
        get() = (lastResult as? VerifyResult.Success)?.targets?.resolvedCount() ?: 0

    val totalTargetCount: Int get() = TOTAL_TARGETS

    companion object {
        const val TOTAL_TARGETS = 7

        fun parseHookStatus(raw: String?): HookStatus? {
            val parts = raw?.substringBefore(' ')?.split('/') ?: return null
            if (parts.size != 2) return null
            val installed = parts[0].toIntOrNull() ?: return null
            val total = parts[1].toIntOrNull() ?: return null
            return HookStatus(installed, total)
        }
    }
}

internal fun ResolvedTargets.Resolved.resolvedCount(): Int =
    listOf(adMetadataClass, feedCardClass, adFlagFieldName, adLabelFieldName, adMetadataFieldName)
        .count { !it.isNullOrBlank() } +
        listOf(cardProcessorMethods.isNotEmpty(), streamRenderableListMethod != null)
            .count { it }

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
