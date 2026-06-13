package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val autoRecoveryOnUpdate: Boolean = false,
        val isLauncherIconHidden: Boolean = false,
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onAutoRecoveryChange: (Boolean) -> Unit,
    val onLauncherIconHiddenChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onResetAdsCounter: () -> Unit,
)

enum class ModuleStatus { Active, Inactive }
