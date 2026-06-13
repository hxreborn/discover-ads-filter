@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.navigation.Destination
import eu.hxreborn.discoveradsfilter.ui.screen.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { resId ->
            Toast.makeText(context.applicationContext, context.getString(resId), Toast.LENGTH_LONG).show()
        }
    }
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
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showResetCounterDialog by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val isExpanded = scrollBehavior.state.collapsedFraction < 0.5f
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
                        onClearCacheClick = { showClearCacheDialog = true },
                        onResetCounterClick = { showResetCounterDialog = true },
                    )
                } else {
                    dashboardLoadingCard(surface = surface)
                }
            }
        }
    }

    if (showClearCacheDialog) {
        ClearCacheDialog(
            onDismiss = { showClearCacheDialog = false },
            onConfirm = {
                showClearCacheDialog = false
                actions.onClearCacheOnly()
            },
        )
    }

    if (showResetCounterDialog) {
        ResetCounterDialog(
            onDismiss = { showResetCounterDialog = false },
            onConfirm = {
                showResetCounterDialog = false
                actions.onResetAdsCounter()
            },
        )
    }
}

private fun LazyListScope.dashboardReadyItems(
    ready: HomeUiState.Ready,
    actions: HomeActions,
    onNavigate: (Destination) -> Unit,
    surface: Color,
    onClearCacheClick: () -> Unit,
    onResetCounterClick: () -> Unit,
) {
    item(key = "status", contentType = "StatusCard") {
        StatusCard(state = ready.verify)
    }

    preferenceCategory(
        key = "cat_resolution",
        title = { Text(stringResource(R.string.pref_category_target_resolution)) },
    )

    val scanning = ready.verify.phase == VerifyPhase.Running
    preference(
        key = "diagnostics",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 0), surface = surface),
        icon = {
            Icon(imageVector = Icons.Outlined.Map, contentDescription = null)
        },
        title = {
            Text(
                stringResource(R.string.pref_dexkit_title),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = {
            Text(stringResource(R.string.pref_dexkit_summary_ready))
        },
        widgetContainer = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = { onNavigate(Destination.Diagnostics) },
    )

    item(key = "spacer_diagnostics_clear", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    preference(
        key = "clear_cache",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 1), surface = surface),
        icon = {
            Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
        },
        title = {
            Text(
                stringResource(R.string.pref_clear_cache),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = {
            Text(stringResource(R.string.pref_clear_cache_summary))
        },
        enabled = !scanning,
        onClick = onClearCacheClick,
    )

    item(key = "spacer_clear_recovery", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    item(key = "auto_recovery", contentType = "SwitchPreference") {
        SwitchPreference(
            value = ready.autoRecoveryOnUpdate,
            onValueChange = actions.onAutoRecoveryChange,
            modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 2), surface = surface),
            icon = {
                Icon(imageVector = Icons.Outlined.Autorenew, contentDescription = null)
            },
            title = {
                Text(
                    text = stringResource(R.string.pref_auto_recovery_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            summary = {
                Text(stringResource(R.string.pref_auto_recovery_summary))
            },
        )
    }

    preferenceCategory(
        key = "cat_metrics",
        title = { Text(stringResource(R.string.pref_category_metrics)) },
    )

    preference(
        key = "reset_counter",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(1, 0), surface = surface),
        icon = {
            Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = null)
        },
        title = {
            Text(
                stringResource(R.string.pref_reset_counter),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        summary = {
            Text(stringResource(R.string.pref_reset_counter_summary))
        },
        onClick = onResetCounterClick,
    )

    preferenceCategory(
        key = "cat_app",
        title = { Text(stringResource(R.string.pref_category_app)) },
    )

    item(key = "hide_launcher_icon", contentType = "SwitchPreference") {
        SwitchPreference(
            value = ready.isLauncherIconHidden,
            onValueChange = actions.onLauncherIconHiddenChange,
            modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 0), surface = surface),
            icon = {
                Icon(imageVector = Icons.Outlined.PhonelinkErase, contentDescription = null)
            },
            title = {
                Text(
                    text = stringResource(R.string.pref_hide_launcher_icon_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            summary = {
                Text(stringResource(R.string.pref_hide_launcher_icon_summary))
            },
        )
    }

    item(key = "spacer_hide_verbose", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    item(key = "verbose", contentType = "SwitchPreference") {
        SwitchPreference(
            value = ready.verbose,
            onValueChange = actions.onVerboseChange,
            modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 1), surface = surface),
            icon = {
                Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null)
            },
            title = {
                Text(
                    text = stringResource(R.string.toggle_verbose),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            summary = {
                Text(stringResource(R.string.toggle_verbose_summary))
            },
        )
    }

    item(key = "spacer_verbose_about", contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    preference(
        key = "about",
        modifier = Modifier.preferenceCard(shape = shapeForPosition(3, 2), surface = surface),
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

@Composable
private fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_cache_dialog_title)) },
        text = { Text(stringResource(R.string.clear_cache_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.clear_cache_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ResetCounterDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_counter_dialog_title)) },
        text = { Text(stringResource(R.string.reset_counter_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset_counter_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun Modifier.preferenceCard(
    shape: Shape,
    surface: Color,
): Modifier = this.padding(horizontal = Spacing.sm).background(color = surface, shape = shape).clip(shape)

private val NoOpActions =
    HomeActions(
        onVerboseChange = {},
        onAutoRecoveryChange = {},
        onLauncherIconHiddenChange = {},
        onVerify = {},
        onClearCacheOnly = {},
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
