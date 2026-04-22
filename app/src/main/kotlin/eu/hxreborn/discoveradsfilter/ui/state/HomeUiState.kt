package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets

@Immutable
enum class SymbolStatus {
    Mapped,
    Partial,
    NotFound,
    NotMapped,
}

@Immutable
data class SymbolRow(
    val name: String,
    val value: String?,
    val status: SymbolStatus,
)

@Immutable
data class SymbolSection(
    val title: String,
    val rows: List<SymbolRow>,
) {
    val resolvedCount: Int get() = rows.count { it.status == SymbolStatus.Mapped }

    val totalCount: Int get() = rows.size
}

fun VerifyUiState.toSymbolSections(): List<SymbolSection> {
    val targets = (lastResult as? VerifyResult.Success)?.targets
    return listOf(
        SymbolSection(
            "Classes",
            listOf(
                SymbolRow(
                    name = "Ad metadata",
                    value = targets?.adMetadataClass?.substringAfterLast('.'),
                    status = targets?.adMetadataClass.toSymbolStatus(),
                ),
                SymbolRow(
                    name = "Feed card",
                    value = targets?.feedCardClass?.substringAfterLast('.'),
                    status = targets?.feedCardClass.toSymbolStatus(),
                ),
            ),
        ),
        SymbolSection(
            "Fields",
            listOf(
                SymbolRow("Ad flag", targets?.adFlagFieldName, targets?.adFlagFieldName.toSymbolStatus()),
                SymbolRow("Ad label", targets?.adLabelFieldName, targets?.adLabelFieldName.toSymbolStatus()),
                SymbolRow("Ad metadata ref", targets?.adMetadataFieldName, targets?.adMetadataFieldName.toSymbolStatus()),
            ),
        ),
        SymbolSection(
            "Methods",
            listOf(
                SymbolRow(
                    name = "Card processors",
                    value = targets?.cardProcessorMethods?.takeIf { it.isNotEmpty() }?.let { "${it.size} methods" },
                    status =
                        if (!targets?.cardProcessorMethods.isNullOrEmpty()) {
                            SymbolStatus.Mapped
                        } else {
                            SymbolStatus.NotFound
                        },
                ),
                SymbolRow(
                    name = "Stream list",
                    value = targets?.streamRenderableListMethod?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" },
                    status =
                        if (targets?.streamRenderableListMethod != null) {
                            SymbolStatus.Mapped
                        } else {
                            SymbolStatus.NotFound
                        },
                ),
            ),
        ),
    )
}

private fun String?.toSymbolStatus(): SymbolStatus =
    if (this.isNullOrBlank()) {
        SymbolStatus.NotFound
    } else {
        SymbolStatus.Mapped
    }

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val filterEnabled: Boolean = true,
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onFilterEnabledChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onResetAdsCounter: () -> Unit,
    val onDismissStartupScan: () -> Unit,
)

enum class ModuleStatus { Unknown, Active, Inactive }

@Immutable
data class HookStatus(
    val installed: Int,
    val total: Int,
)

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

    val showStartupOverlay: Boolean
        get() =
            scanOrigin == ScanOrigin.Startup &&
                (phase == VerifyPhase.Running || (phase == VerifyPhase.Idle && scanProgress.isNotEmpty()))

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

internal fun ResolvedTargets.Resolved.resolvedCount(): Int {
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
