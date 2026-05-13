package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val filterEnabled: Boolean = true,
        val isLauncherIconHidden: Boolean = false,
        val verify: VerifyUiState = VerifyUiState(),
    ) : HomeUiState
}

@Immutable
data class HomeActions(
    val onVerboseChange: (Boolean) -> Unit,
    val onFilterEnabledChange: (Boolean) -> Unit,
    val onLauncherIconHiddenChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onResetAdsCounter: () -> Unit,
)

enum class ModuleStatus { Active, Inactive }
