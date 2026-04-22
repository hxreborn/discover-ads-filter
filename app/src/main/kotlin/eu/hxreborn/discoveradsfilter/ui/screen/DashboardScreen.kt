@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.ScanProgressCard
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.navigation.Destination
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.util.drawVerticalScrollbar
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
    var startupDismissed by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    val verify = ready?.verify
    val showStartupOverlay = verify?.showStartupOverlay == true && !startupDismissed

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (showStartupOverlay) Modifier.blur(20.dp) else Modifier),
        topBar = {
            LargeTopAppBar(
                title = {
                    val isExpanded =
                        LocalTextStyle.current.fontSize >=
                            MaterialTheme.typography.headlineMedium.fontSize
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
                windowInsets =
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
            )
        },
        contentWindowInsets =
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
    ) { innerPadding ->
        ProvidePreferenceLocals {
            val surface = MaterialTheme.colorScheme.surfaceVariant

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().drawVerticalScrollbar(listState),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 32.dp,
                    ),
            ) {
                if (ready != null) {
                    item(key = "status") {
                        StatusCard(state = ready.verify)
                    }

                    preferenceCategory(
                        key = "cat_filter",
                        title = { Text(stringResource(R.string.pref_category_filter)) },
                    )

                    val moduleActive = ready.verify.moduleStatus != ModuleStatus.Inactive
                    val filterShape = shapeForPosition(1, 0)
                    item(key = "filter_enabled", contentType = "SwitchPreference") {
                        SwitchPreference(
                            value = ready.filterEnabled,
                            onValueChange = actions.onFilterEnabledChange,
                            enabled = moduleActive,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .background(color = surface, shape = filterShape)
                                    .clip(filterShape),
                            icon = {
                                Icon(imageVector = Icons.Outlined.VisibilityOff, contentDescription = null)
                            },
                            title = {
                                Text(
                                    text = stringResource(R.string.pref_filter_sponsored),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            summary = {
                                Text(stringResource(R.string.pref_filter_sponsored_summary))
                            },
                        )
                    }

                    preferenceCategory(
                        key = "cat_diagnostics",
                        title = { Text(stringResource(R.string.pref_category_diagnostics)) },
                    )

                    val advancedCount = 3
                    val advancedTopShape = shapeForPosition(advancedCount, 0)
                    preference(
                        key = "diagnostics",
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .background(color = surface, shape = advancedTopShape)
                                .clip(advancedTopShape),
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
                        onClick = { onNavigate(Destination.Diagnostics) },
                    )

                    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

                    val advancedMidShape = shapeForPosition(advancedCount, 1)
                    item(key = "verbose", contentType = "SwitchPreference") {
                        SwitchPreference(
                            value = ready.verbose,
                            onValueChange = actions.onVerboseChange,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .background(color = surface, shape = advancedMidShape)
                                    .clip(advancedMidShape),
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.BugReport,
                                    contentDescription = null,
                                )
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

                    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

                    val scanning = ready.verify.phase == VerifyPhase.Running
                    val advancedBottomShape = shapeForPosition(advancedCount, 2)
                    preference(
                        key = "clear_cache",
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .background(color = surface, shape = advancedBottomShape)
                                .clip(advancedBottomShape),
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
                        onClick = { showClearCacheDialog = true },
                    )

                    item(contentType = "Spacer") { Spacer(Modifier.height(16.dp)) }

                    preferenceCategory(
                        key = "cat_about",
                        title = { Text(stringResource(R.string.pref_category_about)) },
                    )

                    val aboutShape = shapeForPosition(1, 0)
                    preference(
                        key = "about",
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .background(color = surface, shape = aboutShape)
                                .clip(aboutShape),
                        icon = {
                            Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
                        },
                        title = {
                            Text(
                                stringResource(R.string.pref_category_about),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        summary = { Text("v${BuildConfig.VERSION_NAME}") },
                        onClick = { onNavigate(Destination.About) },
                    )
                }
            }
        }
    }

    if (showStartupOverlay && verify != null) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            ScanProgressCard(
                progress = verify.scanProgress,
                phase = verify.phase,
                durationMs = verify.scanDurationMs,
                onDismiss =
                    if (verify.phase == VerifyPhase.Idle) {
                        {
                            startupDismissed = true
                            actions.onDismissStartupScan()
                        }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            )
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.pref_clear_cache)) },
            text = { Text(stringResource(R.string.pref_clear_cache_summary)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        actions.onClearCacheOnly()
                    },
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
