@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.NavigationRow
import eu.hxreborn.discoveradsfilter.ui.components.SectionHeader
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.components.SwitchRow
import eu.hxreborn.discoveradsfilter.ui.components.centeredContentWidth
import eu.hxreborn.discoveradsfilter.ui.components.listBottomPadding
import eu.hxreborn.discoveradsfilter.ui.components.rowSurface
import eu.hxreborn.discoveradsfilter.ui.navigation.Destination
import eu.hxreborn.discoveradsfilter.ui.screen.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Tokens
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel
import me.zhanghai.compose.preference.TextFieldPreference

private const val FEED_ROW_COUNT = 3
private const val SHARING_ROW_COUNT = 3
private const val APP_ROW_COUNT = 4

@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreenContent(
        modifier = modifier,
        state = state,
        actions = viewModel.actions,
        onNavigate = onNavigate,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreenContent(
    state: HomeUiState,
    actions: HomeActions,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? HomeUiState.Ready
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomPadding = listBottomPadding()
    val direction = LocalLayoutDirection.current

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.centeredContentWidth(),
            contentPadding =
                PaddingValues(
                    start = innerPadding.calculateStartPadding(direction),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(direction),
                    bottom = bottomPadding,
                ),
        ) {
            if (ready != null) {
                dashboardReadyItems(ready = ready, actions = actions, onNavigate = onNavigate)
            } else {
                dashboardLoadingCard()
            }
        }
    }
}

private fun Modifier.groupRow(index: Int): Modifier =
    padding(horizontal = Tokens.ScreenMargin).padding(top = if (index == 0) 0.dp else Tokens.RowGap)

private fun LazyListScope.dashboardReadyItems(
    ready: HomeUiState.Ready,
    actions: HomeActions,
    onNavigate: (Destination) -> Unit,
) {
    item(key = "status", contentType = "status") {
        StatusCard(
            state = ready.verify,
            onOpenDiagnostics = { onNavigate(Destination.Diagnostics) },
            modifier = Modifier.padding(vertical = Tokens.ScreenMargin),
        )
    }

    item(key = "feed_header", contentType = "header") {
        SectionHeader(stringResource(R.string.pref_category_feed))
    }
    item(key = "filter_ads", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(FEED_ROW_COUNT, 0),
            icon = Icons.Outlined.Block,
            title = stringResource(R.string.pref_filter_ads_title),
            summary = stringResource(R.string.pref_filter_ads_summary),
            checked = ready.filterAds,
            onCheckedChange = actions.onFilterAdsChange,
            modifier = Modifier.groupRow(0),
        )
    }
    item(key = "news_rules", contentType = "navigation") {
        NavigationRow(
            shape = shapeForPosition(FEED_ROW_COUNT, 1),
            icon = Icons.Outlined.FilterAlt,
            title = stringResource(R.string.pref_news_rules_title),
            summary =
                if (ready.newsRules.isEmpty()) {
                    stringResource(R.string.pref_news_rules_summary)
                } else {
                    stringResource(
                        R.string.pref_news_rules_summary_count,
                        ready.newsRules.count { it.enabled },
                        ready.newsRules.size,
                    )
                },
            onClick = { onNavigate(Destination.NewsRules) },
            modifier = Modifier.groupRow(1),
        )
    }
    item(key = "expressive_loading_indicator", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(FEED_ROW_COUNT, 2),
            icon = Icons.Outlined.Refresh,
            title = stringResource(R.string.pref_expressive_loading_indicator_title),
            summary = stringResource(R.string.pref_expressive_loading_indicator_summary),
            checked = ready.expressiveLoadingIndicator,
            onCheckedChange = actions.onExpressiveLoadingIndicatorChange,
            modifier = Modifier.groupRow(2),
        )
    }

    item(key = "sharing_header", contentType = "header") {
        SectionHeader(stringResource(R.string.pref_category_sharing))
    }
    item(key = "share_original_link", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(SHARING_ROW_COUNT, 0),
            icon = Icons.Outlined.Link,
            title = stringResource(R.string.pref_share_original_link_title),
            summary = stringResource(R.string.pref_share_original_link_summary),
            checked = ready.shareOriginalLink,
            onCheckedChange = actions.onShareOriginalLinkChange,
            modifier = Modifier.groupRow(0),
        )
    }
    item(key = "share_strip_source_line", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(SHARING_ROW_COUNT, 1),
            icon = Icons.AutoMirrored.Outlined.ShortText,
            title = stringResource(R.string.pref_share_strip_source_title),
            summary = stringResource(R.string.pref_share_strip_source_summary),
            checked = ready.shareStripSourceLine,
            onCheckedChange = actions.onShareStripSourceLineChange,
            enabled = ready.shareOriginalLink,
            modifier = Modifier.groupRow(1),
        )
    }
    item(key = "share_custom_line", contentType = "text_field") {
        ShareCustomLineRow(
            ready = ready,
            actions = actions,
            modifier = Modifier.groupRow(2),
        )
    }

    item(key = "app_header", contentType = "header") {
        SectionHeader(stringResource(R.string.pref_category_app))
    }
    item(key = "auto_recovery", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(APP_ROW_COUNT, 0),
            icon = Icons.Outlined.Build,
            title = stringResource(R.string.pref_auto_recovery_title),
            summary =
                stringResource(
                    if (ready.autoRecoveryOnUpdate) {
                        R.string.pref_auto_recovery_summary
                    } else {
                        R.string.pref_auto_recovery_summary_root
                    },
                ),
            checked = ready.autoRecoveryOnUpdate,
            onCheckedChange = actions.onAutoRecoveryChange,
            modifier = Modifier.groupRow(0),
        )
    }
    item(key = "hide_launcher_icon", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(APP_ROW_COUNT, 1),
            icon = Icons.Outlined.PhonelinkErase,
            title = stringResource(R.string.pref_hide_launcher_icon_title),
            summary = stringResource(R.string.pref_hide_launcher_icon_summary),
            checked = ready.isLauncherIconHidden,
            onCheckedChange = actions.onLauncherIconHiddenChange,
            modifier = Modifier.groupRow(1),
        )
    }
    item(key = "verbose", contentType = "switch") {
        SwitchRow(
            shape = shapeForPosition(APP_ROW_COUNT, 2),
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.toggle_verbose),
            summary = stringResource(R.string.toggle_verbose_summary),
            checked = ready.verbose,
            onCheckedChange = actions.onVerboseChange,
            modifier = Modifier.groupRow(2),
        )
    }
    item(key = "about", contentType = "navigation") {
        NavigationRow(
            shape = shapeForPosition(APP_ROW_COUNT, 3),
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.pref_category_about),
            summary = stringResource(R.string.pref_about_summary),
            onClick = { onNavigate(Destination.About) },
            modifier = Modifier.groupRow(3),
        )
    }
}

@Composable
private fun ShareCustomLineRow(
    ready: HomeUiState.Ready,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    TextFieldPreference(
        value = ready.shareCustomLine,
        onValueChange = actions.onShareCustomLineChange,
        textToValue = { text -> text.trim().takeIf { it.isNotEmpty() } },
        valueToText = { it.orEmpty() },
        modifier = modifier.rowSurface(shapeForPosition(SHARING_ROW_COUNT, 2)),
        enabled = ready.shareOriginalLink && !ready.shareStripSourceLine,
        icon = { Icon(imageVector = Icons.Outlined.EditNote, contentDescription = null) },
        title = { Text(stringResource(R.string.pref_share_custom_line_title)) },
        summary = {
            Text(ready.shareCustomLine ?: stringResource(R.string.pref_share_custom_line_summary))
        },
        textField = { value, onValueChange, onOk ->
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.pref_share_custom_line_hint)) },
                keyboardActions = KeyboardActions { onOk() },
                singleLine = true,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.dashboardLoadingCard() {
    item(key = "loading", contentType = "loading") {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(Tokens.ScreenMargin),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = shapeForPosition(1, 0),
        ) {
            val loadingDesc = stringResource(R.string.loading)
            Box(
                modifier =
                    Modifier.fillMaxWidth().padding(Tokens.RowPadding * 2).semantics {
                        contentDescription = loadingDesc
                    },
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        }
    }
}

private val NoOpActions =
    HomeActions(
        onVerboseChange = {},
        onFilterAdsChange = {},
        onExpressiveLoadingIndicatorChange = {},
        onAutoRecoveryChange = {},
        onShareOriginalLinkChange = {},
        onShareStripSourceLineChange = {},
        onShareCustomLineChange = {},
        onNewsRuleSaved = {},
        onNewsRuleDeleted = {},
        onLoadPresets = {},
        onImportRules = {},
        onExportRules = {},
        onSetAllRulesEnabled = {},
        onDeleteAllRules = {},
        onLauncherIconHiddenChange = {},
        onVerify = {},
        onClearCacheOnly = {},
        onRestartGoogleApp = {},
        onResetAdsCounter = {},
    )

private class DashboardStateProvider : PreviewParameterProvider<HomeUiState> {
    override val values: Sequence<HomeUiState> =
        sequenceOf(
            HomeUiState.Ready(verify = PreviewFixtures.verifySuccessFull()),
            HomeUiState.Ready(verify = PreviewFixtures.verifyNeedsScan()),
            HomeUiState.Ready(verify = PreviewFixtures.verifyFailureDexKitNoMatches()),
        )
}

@Preview(name = "Dashboard", showSystemUi = true)
@Composable
private fun DashboardPreview(
    @PreviewParameter(DashboardStateProvider::class) state: HomeUiState,
) {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        DashboardScreenContent(
            state = state,
            actions = NoOpActions,
            onNavigate = {},
        )
    }
}
