@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.DexKitDialog
import eu.hxreborn.discoveradsfilter.ui.components.HeroStatusCard
import eu.hxreborn.discoveradsfilter.ui.components.WavyHeaderBackground
import eu.hxreborn.discoveradsfilter.ui.components.diagnosticsItems
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.util.drawVerticalScrollbar
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory

private const val GITHUB_URL = "https://github.com/hxreborn/discover-ads-filter"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        modifier = modifier,
        state = state,
        actions = viewModel.actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val ready = state as? HomeUiState.Ready

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var prevPhase by remember { mutableStateOf(ready?.verify?.phase) }
    LaunchedEffect(ready?.verify?.phase) {
        if (prevPhase == VerifyPhase.Running && ready?.verify?.phase == VerifyPhase.Idle) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevPhase = ready?.verify?.phase
    }

    var showDexKitDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box {
                WavyHeaderBackground(
                    collapseFraction = scrollBehavior.state.collapsedFraction,
                    modifier = Modifier.matchParentSize(),
                )
                LargeTopAppBar(
                    title = {
                        val isExpanded =
                            LocalTextStyle.current.fontSize >= MaterialTheme.typography.headlineMedium.fontSize
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
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                    actions = {
                        val running = ready?.verify?.phase == VerifyPhase.Running
                        IconButton(onClick = actions.onVerify, enabled = !running) {
                            if (running) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(4.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.button_reverify),
                                )
                            }
                        }
                    },
                    windowInsets =
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                )
            }
        },
        contentWindowInsets =
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { innerPadding ->
        ProvidePreferenceLocals {
            val surface = MaterialTheme.colorScheme.surfaceVariant
            val listState = rememberLazyListState()

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
                    item(key = "hero") {
                        HeroStatusCard(state = ready.verify)
                    }

                    preferenceCategory(
                        key = "cat_diagnostics",
                        title = { Text(stringResource(R.string.pref_category_diagnostics)) },
                    )

                    diagnosticsItems(
                        state = ready.verify,
                        surface = surface,
                        onInspectFingerprints = { showDexKitDialog = true },
                    )

                    preferenceCategory(
                        key = "cat_filter",
                        title = { Text(stringResource(R.string.pref_category_filter)) },
                    )

                    val filterCount = 2
                    val filterShape0 = shapeForPosition(filterCount, 0)
                    item(key = "filter_enabled", contentType = "SwitchPreference") {
                        SwitchPreference(
                            value = ready.verify.filterEnabled,
                            onValueChange = actions.onFilterEnabledChange,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .background(color = surface, shape = filterShape0)
                                    .clip(filterShape0),
                            icon = {
                                Icon(imageVector = Icons.Outlined.FilterAlt, contentDescription = null)
                            },
                            title = {
                                Text(
                                    text = stringResource(R.string.pref_filter_sponsored),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            summary = { Text(stringResource(R.string.pref_filter_sponsored_summary)) },
                        )
                    }

                    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

                    val filterShape1 = shapeForPosition(filterCount, 1)
                    item(key = "verbose", contentType = "SwitchPreference") {
                        SwitchPreference(
                            value = ready.verbose,
                            onValueChange = actions.onVerboseChange,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .background(color = surface, shape = filterShape1)
                                    .clip(filterShape1),
                            icon = {
                                Icon(imageVector = Icons.Outlined.Adb, contentDescription = null)
                            },
                            title = {
                                Text(
                                    text = stringResource(R.string.toggle_verbose),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            summary = { Text(stringResource(R.string.toggle_verbose_summary)) },
                        )
                    }

                    preferenceCategory(
                        key = "cat_about",
                        title = { Text(stringResource(R.string.pref_category_about)) },
                    )

                    val aboutCount = 2
                    val aboutShape0 = shapeForPosition(aboutCount, 0)
                    preference(
                        key = "version",
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .background(color = surface, shape = aboutShape0)
                                .clip(aboutShape0),
                        icon = { Icon(imageVector = Icons.Rounded.Info, contentDescription = null) },
                        title = {
                            Text(
                                text = stringResource(R.string.pref_version_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        summary = {
                            Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        },
                        onClick = {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(
                                        R.string.pref_version_easter_egg,
                                        BuildConfig.VERSION_NAME,
                                        BuildConfig.VERSION_CODE,
                                        BuildConfig.BUILD_TYPE,
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                    )

                    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

                    val aboutShape1 = shapeForPosition(aboutCount, 1)
                    preference(
                        key = "source",
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .background(color = surface, shape = aboutShape1)
                                .clip(aboutShape1),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_github_24),
                                contentDescription = null,
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.pref_github_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        summary = { Text(stringResource(R.string.pref_github_summary)) },
                        onClick = { context.openUrl(GITHUB_URL) },
                    )
                }
            }
        }
    }

    if (showDexKitDialog && ready != null) {
        DexKitDialog(
            state = ready.verify,
            onDismiss = { showDexKitDialog = false },
        )
    }
}

private fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
