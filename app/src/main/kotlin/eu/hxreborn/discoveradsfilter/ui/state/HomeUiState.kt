package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val autoRecoveryOnUpdate: Boolean = false,
        val shareOriginalLink: Boolean = false,
        val shareStripSourceLine: Boolean = false,
        val shareCustomLine: String? = null,
        val isLauncherIconHidden: Boolean = false,
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onAutoRecoveryChange: (Boolean) -> Unit,
    val onShareOriginalLinkChange: (Boolean) -> Unit,
    val onShareStripSourceLineChange: (Boolean) -> Unit,
    val onShareCustomLineChange: (String?) -> Unit,
    val onLauncherIconHiddenChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onResetAdsCounter: () -> Unit,
)

enum class ModuleStatus { Active, Inactive }
