@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.IconSwitch
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.navigation.Destination
import eu.hxreborn.discoveradsfilter.ui.screen.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.preferenceCard
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.TextFieldPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val isExpanded by remember {
                        derivedStateOf { scrollBehavior.state.collapsedFraction < 0.5f }
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style =
                            if (isExpanded) {
                                MaterialTheme.typography.headlineLarge.copy(lineHeight = 36.sp)
                            } else {
                                LocalTextStyle.current
                            },
                        maxLines = if (isExpanded) 2 else 1,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ProvidePreferenceLocals {
            val surface = MaterialTheme.colorScheme.surfaceVariant
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                if (ready != null) {
                    dashboardReadyItems(
                        ready = ready,
                        actions = actions,
                        onNavigate = onNavigate,
                        surface = surface,
                    )
                } else {
                    dashboardLoadingCard(surface = surface)
                }
            }
        }
    }
}

private fun LazyListScope.dashboardReadyItems(
    ready: HomeUiState.Ready,
    actions: HomeActions,
    onNavigate: (Destination) -> Unit,
    surface: Color,
) {
    item(key = "status", contentType = "StatusCard") {
        StatusCard(
            state = ready.verify,
            onOpenDiagnostics = { onNavigate(Destination.Diagnostics) },
        )
    }

    preferenceCategory(
        key = "cat_feed",
        title = { Text(stringResource(R.string.pref_category_feed)) },
    )

    switchRow(
        key = "filter_ads",
        value = ready.filterAds,
        onValueChange = actions.onFilterAdsChange,
        shapeCount = 2,
        shapeIndex = 0,
        surface = surface,
        icon = Icons.Outlined.Block,
        titleRes = R.string.pref_filter_ads_title,
        summaryRes = R.string.pref_filter_ads_summary,
    )

    item(key = "spacer_ads_rules", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    preference(
        key = "news_rules",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(2, 1), surface = surface),
        icon = {
            Icon(imageVector = Icons.Outlined.FilterAlt, contentDescription = null)
        },
        title = {
            Text(
                stringResource(R.string.pref_news_rules_title),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = {
            Text(
                if (ready.newsRules.isEmpty()) {
                    stringResource(R.string.pref_news_rules_summary)
                } else {
                    stringResource(
                        R.string.pref_news_rules_summary_count,
                        ready.newsRules.count { it.enabled },
                        ready.newsRules.size,
                    )
                },
            )
        },
        widgetContainer = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = { onNavigate(Destination.NewsRules) },
    )

    preferenceCategory(
        key = "cat_sharing",
        title = { Text(stringResource(R.string.pref_category_sharing)) },
    )

    switchRow(
        key = "share_original_link",
        value = ready.shareOriginalLink,
        onValueChange = actions.onShareOriginalLinkChange,
        shapeCount = 3,
        shapeIndex = 0,
        surface = surface,
        icon = Icons.Outlined.Link,
        titleRes = R.string.pref_share_original_link_title,
        summaryRes = R.string.pref_share_original_link_summary,
    )

    item(key = "spacer_share_strip", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    switchRow(
        key = "share_strip_source",
        value = ready.shareStripSourceLine,
        onValueChange = actions.onShareStripSourceLineChange,
        shapeCount = 3,
        shapeIndex = 1,
        surface = surface,
        enabled = ready.shareOriginalLink,
        icon = Icons.AutoMirrored.Outlined.ShortText,
        titleRes = R.string.pref_share_strip_source_title,
        summaryRes = R.string.pref_share_strip_source_summary,
    )

    item(key = "spacer_strip_custom", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    item(key = "share_custom_line", contentType = "TextFieldPreference") {
        TextFieldPreference(
            value = ready.shareCustomLine,
            onValueChange = actions.onShareCustomLineChange,
            textToValue = { text -> text.trim().takeIf { it.isNotEmpty() } },
            valueToText = { it.orEmpty() },
            modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 2), surface = surface),
            enabled = ready.shareOriginalLink && !ready.shareStripSourceLine,
            icon = {
                Icon(imageVector = Icons.Outlined.EditNote, contentDescription = null)
            },
            title = {
                Text(
                    text = stringResource(R.string.pref_share_custom_line_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            summary = {
                Text(
                    ready.shareCustomLine
                        ?: stringResource(R.string.pref_share_custom_line_summary),
                )
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

    preferenceCategory(
        key = "cat_app",
        title = { Text(stringResource(R.string.pref_category_app)) },
    )

    switchRow(
        key = "auto_recovery",
        value = ready.autoRecoveryOnUpdate,
        onValueChange = actions.onAutoRecoveryChange,
        shapeCount = 4,
        shapeIndex = 0,
        surface = surface,
        icon = Icons.Outlined.Build,
        titleRes = R.string.pref_auto_recovery_title,
        summaryRes =
            if (ready.autoRecoveryOnUpdate) {
                R.string.pref_auto_recovery_summary
            } else {
                R.string.pref_auto_recovery_summary_root
            },
    )

    item(key = "spacer_recovery_hide", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    switchRow(
        key = "hide_launcher_icon",
        value = ready.isLauncherIconHidden,
        onValueChange = actions.onLauncherIconHiddenChange,
        shapeCount = 4,
        shapeIndex = 1,
        surface = surface,
        icon = Icons.Outlined.PhonelinkErase,
        titleRes = R.string.pref_hide_launcher_icon_title,
        summaryRes = R.string.pref_hide_launcher_icon_summary,
    )

    item(key = "spacer_hide_verbose", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    switchRow(
        key = "verbose",
        value = ready.verbose,
        onValueChange = actions.onVerboseChange,
        shapeCount = 4,
        shapeIndex = 2,
        surface = surface,
        icon = Icons.Outlined.BugReport,
        titleRes = R.string.toggle_verbose,
        summaryRes = R.string.toggle_verbose_summary,
    )

    item(key = "spacer_verbose_about", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    preference(
        key = "about",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(4, 3), surface = surface),
        icon = {
            Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
        },
        title = {
            Text(
                stringResource(R.string.pref_category_about),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = { Text(stringResource(R.string.pref_about_summary)) },
        onClick = { onNavigate(Destination.About) },
    )

    item(key = "spacer_bottom", contentType = "Spacer") { Spacer(Modifier.height(16.dp)) }
}

private fun LazyListScope.switchRow(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    shapeCount: Int,
    shapeIndex: Int,
    surface: Color,
    icon: ImageVector,
    titleRes: Int,
    summaryRes: Int,
    enabled: Boolean = true,
) {
    preference(
        key = key,
        modifier =
            Modifier.preferenceCard(
                shape = shapeForPosition(shapeCount, shapeIndex),
                surface = surface,
            ),
        enabled = enabled,
        icon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        title = {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = {
            Text(stringResource(summaryRes))
        },
        widgetContainer = {
            IconSwitch(
                checked = value,
                onCheckedChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md),
            )
        },
        onClick = { onValueChange(!value) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.dashboardLoadingCard(surface: Color) {
    item(key = "loading", contentType = "loading") {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            color = surface,
            shape = MaterialTheme.shapes.large,
        ) {
            val loadingDesc = stringResource(R.string.loading)
            Box(
                modifier =
                    Modifier.fillMaxWidth().padding(Spacing.lg).semantics {
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
