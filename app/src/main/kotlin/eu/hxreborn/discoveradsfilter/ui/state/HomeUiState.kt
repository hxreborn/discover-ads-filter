package eu.hxreborn.discoveradsfilter.ui.state

import android.net.Uri
import androidx.compose.runtime.Immutable
import eu.hxreborn.discoveradsfilter.filter.NewsRule

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Ready(
        val verbose: Boolean = false,
        val autoRecoveryOnUpdate: Boolean = false,
        val shareOriginalLink: Boolean = false,
        val shareStripSourceLine: Boolean = false,
        val shareCustomLine: String? = null,
        val newsRules: List<NewsRule> = emptyList(),
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
    val onNewsRuleSaved: (NewsRule) -> Unit,
    val onNewsRuleDeleted: (String) -> Unit,
    val onLoadPresets: () -> Unit,
    val onImportRules: (Uri) -> Unit,
    val onExportRules: (Uri) -> Unit,
    val onLauncherIconHiddenChange: (Boolean) -> Unit,
    val onVerify: () -> Unit,
    val onClearCacheOnly: () -> Unit,
    val onRestartGoogleApp: () -> Unit,
    val onResetAdsCounter: () -> Unit,
)

enum class ModuleStatus { Active, Inactive }
