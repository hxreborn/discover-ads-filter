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
                    name = "AdMetadata",
                    value = targets?.adMetadataClass?.substringAfterLast('.'),
                    status = targets?.adMetadataClass.toSymbolStatus(),
                ),
                SymbolRow(
                    name = "FeedCard",
                    value = targets?.feedCardClass?.substringAfterLast('.'),
                    status = targets?.feedCardClass.toSymbolStatus(),
                ),
            ),
        ),
        SymbolSection(
            "Fields",
            listOf(
                SymbolRow("isAd", targets?.adFlagFieldName, targets?.adFlagFieldName.toSymbolStatus()),
                SymbolRow("adLabel", targets?.adLabelFieldName, targets?.adLabelFieldName.toSymbolStatus()),
                SymbolRow("adMetadata", targets?.adMetadataFieldName, targets?.adMetadataFieldName.toSymbolStatus()),
            ),
        ),
        SymbolSection(
            "Methods",
            listOf(
                SymbolRow(
                    name = "Card processors",
                    value = targets?.cardProcessorMethods?.takeIf { it.isNotEmpty() }?.let { "${it.size} methods" },
                    status =
                        if (targets?.cardProcessorMethods?.isNotEmpty() == true) {
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
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onFilterEnabledChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCache: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onDismissStartupScan: () -> Unit,
)

@Immutable
data class VerifyUiState(
    val phase: VerifyPhase = VerifyPhase.Idle,
    val lastResult: VerifyResult? = null,
    val installedAgsaVersion: Long? = null,
    val installedAgsaVersionName: String? = null,
    val installedAgsaLastUpdateTime: Long = 0,
    val scanModuleVersion: Int = 0,
    val hookInstallStatus: String? = null,
    val hookProcess: String? = null,
    val adsHidden: Long = 0,
    val filterEnabled: Boolean = true,
    val moduleActive: Boolean = false,
    val moduleActiveChecked: Boolean = false,
    val scanOrigin: ScanOrigin? = null,
    val scanProgress: List<ScanStep> = emptyList(),
    val scanDurationMs: Long = 0,
    val lastRefreshError: String? = null,
    val startupScanDismissed: Boolean = false,
) {
    val hookInstalled: Int get() = parseSlash(hookInstallStatus).first

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

private fun ResolvedTargets.Resolved.resolvedFieldCount(): Int =
    listOf(
        !adMetadataClass.isNullOrBlank(),
        !feedCardClass.isNullOrBlank(),
        !adFlagFieldName.isNullOrBlank(),
        !adLabelFieldName.isNullOrBlank(),
        !adMetadataFieldName.isNullOrBlank(),
        cardProcessorMethods.isNotEmpty(),
        streamRenderableListMethod != null,
    ).count { it }

enum class VerifyPhase { Idle, Running }

enum class ScanOrigin { Startup, Background, Manual }

@Immutable
data class ScanStep(
    val label: String,
    val rawValue: String?,
    val resolved: Boolean,
)

enum class HookCoverage { Full, FallbackOnly, None, NotScanned, ScanFailed, ModuleNotActive }

fun VerifyUiState.hookCoverage(): HookCoverage {
    if (moduleActiveChecked && !moduleActive) return HookCoverage.ModuleNotActive

    return when (val r = lastResult) {
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
