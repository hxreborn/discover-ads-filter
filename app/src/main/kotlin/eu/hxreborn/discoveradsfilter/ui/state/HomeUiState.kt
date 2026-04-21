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
    val onFilterEnabledChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
)

@Immutable
data class VerifyUiState(
    val phase: VerifyPhase = VerifyPhase.Idle,
    val lastResult: VerifyResult? = null,
    val installedAgsaVersion: Long? = null,
    val installedAgsaVersionName: String? = null,
    val installedAgsaLastUpdateTime: Long = 0,
    val xposedServiceBound: Boolean = false,
    val scanModuleVersion: Int = 0,
    val hookInstallStatus: String? = null,
    val hookProcess: String? = null,
    val adsHidden: Long = 0,
    val filterEnabled: Boolean = true,
) {
    val hookInstalled: Int get() = parseSlash(hookInstallStatus).first
    val hookTotal: Int get() = parseSlash(hookInstallStatus).second

    val resolvedTargetCount: Int
        get() = (lastResult as? VerifyResult.Success)?.targets?.resolvedFieldCount() ?: 0

    val totalTargetCount: Int get() = TOTAL_TARGETS

    companion object {
        const val TOTAL_TARGETS = 7

        private fun parseSlash(status: String?): Pair<Int, Int> {
            val raw = status ?: return 0 to 0
            val head = raw.substringBefore(' ')
            val parts = head.split('/')
            if (parts.size != 2) return 0 to 0
            val a = parts[0].toIntOrNull() ?: return 0 to 0
            val b = parts[1].toIntOrNull() ?: return 0 to 0
            return a to b
        }
    }
}

private fun ResolvedTargets.Resolved.resolvedFieldCount(): Int {
    var n = 0
    if (!adMetadataClass.isNullOrBlank()) n++
    if (!feedCardClass.isNullOrBlank()) n++
    if (!adFlagFieldName.isNullOrBlank()) n++
    if (!adLabelFieldName.isNullOrBlank()) n++
    if (!adMetadataFieldName.isNullOrBlank()) n++
    if (cardProcessorMethods.isNotEmpty()) n++
    if (streamRenderableListMethod != null) n++
    return n
}

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
