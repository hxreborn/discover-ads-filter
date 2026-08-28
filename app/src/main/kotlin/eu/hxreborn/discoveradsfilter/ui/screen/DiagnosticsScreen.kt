@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhonelinkSetup
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.ResolvedSymbolRow
import eu.hxreborn.discoveradsfilter.ui.components.ScanProgressCard
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailScaffold
import eu.hxreborn.discoveradsfilter.ui.screen.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.ScanOrigin
import eu.hxreborn.discoveradsfilter.ui.state.SymbolSection
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.toDiagnosticsReport
import eu.hxreborn.discoveradsfilter.ui.state.toSymbolSections
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState as? HomeUiState.Ready
    val state = ready?.verify ?: VerifyUiState()

    DiagnosticsScreenContent(
        state = state,
        onVerify = viewModel.actions.onVerify,
        onBack = onBack,
        modifier = modifier,
        onClearCache = viewModel.actions.onClearCacheOnly,
        onRestartAgsa = viewModel.actions.onRestartGoogleApp,
        onResetCounters = viewModel.actions.onResetAdsCounter,
        rootLikelyGranted = ready?.autoRecoveryOnUpdate == true,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreenContent(
    state: VerifyUiState,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onClearCache: () -> Unit = {},
    onRestartAgsa: () -> Unit = {},
    onResetCounters: () -> Unit = {},
    rootLikelyGranted: Boolean = false,
) {
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showRestartAgsaDialog by rememberSaveable { mutableStateOf(false) }
    var showResetCountersDialog by rememberSaveable { mutableStateOf(false) }
    var showManualProgressThisVisit by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val anyRunning = state.phase == VerifyPhase.Running
    val manualRunning = anyRunning && state.scanOrigin == ScanOrigin.Manual

    LaunchedEffect(manualRunning) {
        if (manualRunning) showManualProgressThisVisit = true
    }

    val showProgress = showManualProgressThisVisit && (manualRunning || state.scanProgress.isNotEmpty())

    SettingsDetailScaffold(
        title = stringResource(R.string.nav_diagnostics),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = { copyDiagnostics(context, state) }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.diag_copy),
                )
            }
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.nav_info),
                )
            }
        },
        floatingActionButton = {
            DiagnosticsFabHost(
                anyRunning = anyRunning,
                hasCachedScan = state.lastResult != null,
                onTap = onVerify,
            )
        },
    ) {
        val sections = remember(state.lastResult) { state.toSymbolSections() }
        DiagnosticsContent(
            state = state,
            sections = sections,
            showProgress = showProgress,
        )
        MaintenanceSection(
            enabled = !anyRunning,
            rootLikelyGranted = rootLikelyGranted,
            onClearCache = { showClearCacheDialog = true },
            onRestartAgsa = { showRestartAgsaDialog = true },
            onResetCounters = { showResetCountersDialog = true },
        )
        Spacer(Modifier.height(96.dp))
    }

    if (showClearCacheDialog) {
        ConfirmActionDialog(
            titleRes = R.string.clear_cache_dialog_title,
            bodyRes = R.string.clear_cache_dialog_body,
            confirmRes = R.string.clear_cache_dialog_confirm,
            onDismiss = { showClearCacheDialog = false },
            onConfirm = {
                showClearCacheDialog = false
                onClearCache()
            },
        )
    }

    if (showRestartAgsaDialog) {
        ConfirmActionDialog(
            titleRes = R.string.restart_agsa_dialog_title,
            bodyRes = R.string.restart_agsa_dialog_body,
            confirmRes = R.string.restart_agsa_dialog_confirm,
            onDismiss = { showRestartAgsaDialog = false },
            onConfirm = {
                showRestartAgsaDialog = false
                onRestartAgsa()
            },
        )
    }

    if (showResetCountersDialog) {
        ConfirmActionDialog(
            titleRes = R.string.reset_counter_dialog_title,
            bodyRes = R.string.reset_counter_dialog_body,
            confirmRes = R.string.reset_counter_dialog_confirm,
            onDismiss = { showResetCountersDialog = false },
            onConfirm = {
                showResetCountersDialog = false
                onResetCounters()
            },
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.diag_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.diag_info_body_1))
                    Text(stringResource(R.string.diag_info_body_2))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

private fun copyDiagnostics(
    context: Context,
    state: VerifyUiState,
) {
    val report = state.toDiagnosticsReport(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.diag_copy), report))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.diag_copied, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiagnosticsFabHost(
    anyRunning: Boolean,
    hasCachedScan: Boolean,
    onTap: () -> Unit,
) {
    val fabEnabled = !anyRunning
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (fabEnabled && isPressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "diagnostics_fab_press_scale",
    )

    DiagnosticsFab(
        anyRunning = anyRunning,
        hasCachedScan = hasCachedScan,
        onTap = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onTap()
        },
        fabEnabled = fabEnabled,
        interactionSource = interactionSource,
        modifier =
            Modifier.graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiagnosticsFab(
    anyRunning: Boolean,
    hasCachedScan: Boolean,
    onTap: () -> Unit,
    fabEnabled: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fabContainerColor = if (fabEnabled) scheme.primaryContainer else scheme.surfaceVariant
    val fabContentColor = if (fabEnabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val baseModifier = modifier.animateContentSize(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
    val runningFabLabel = stringResource(R.string.fab_resolving)
    val fabModifier =
        if (fabEnabled) {
            baseModifier
        } else {
            baseModifier.alpha(0.7f).clearAndSetSemantics { contentDescription = runningFabLabel }
        }
    val idleFabLabel = stringResource(if (hasCachedScan) R.string.button_rescan else R.string.button_scan)

    ExtendedFloatingActionButton(
        onClick = { if (fabEnabled) onTap() },
        modifier = fabModifier,
        containerColor = fabContainerColor,
        contentColor = fabContentColor,
        elevation = FloatingActionButtonDefaults.loweredElevation(),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (anyRunning) {
                LoadingIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            }
            Text(
                text = if (anyRunning) runningFabLabel else idleFabLabel,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
internal fun DiagnosticsContent(
    state: VerifyUiState,
    sections: List<SymbolSection>,
    showProgress: Boolean = false,
) {
    ComboCard(state = state)
    Spacer(Modifier.height(Spacing.lg))

    if (showProgress) {
        ScanProgressCard(
            progress = state.scanProgress,
            phase = state.phase,
            durationMs = state.scanDurationMs,
            showRawValues = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        SymbolSections(sections)
    }

    if (state.lastRefreshError != null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.diag_last_refresh_failed, state.lastRefreshError),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun MaintenanceSection(
    enabled: Boolean,
    rootLikelyGranted: Boolean,
    onClearCache: () -> Unit,
    onRestartAgsa: () -> Unit,
    onResetCounters: () -> Unit,
) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.diag_category_maintenance),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xs))
    MaintenanceRow(
        icon = Icons.Outlined.DeleteSweep,
        title = stringResource(R.string.pref_clear_cache),
        summary = stringResource(R.string.pref_clear_cache_summary),
        enabled = enabled,
        onClick = onClearCache,
        shape = shapeForPosition(3, 0),
    )
    Spacer(Modifier.height(2.dp))
    MaintenanceRow(
        icon = Icons.Outlined.PhonelinkSetup,
        title = stringResource(R.string.pref_restart_agsa),
        summary =
            stringResource(
                if (rootLikelyGranted) {
                    R.string.pref_restart_agsa_summary
                } else {
                    R.string.pref_restart_agsa_summary_root
                },
            ),
        enabled = enabled,
        onClick = onRestartAgsa,
        shape = shapeForPosition(3, 1),
    )
    Spacer(Modifier.height(2.dp))
    MaintenanceRow(
        icon = Icons.Outlined.RestartAlt,
        title = stringResource(R.string.pref_reset_counter),
        summary = stringResource(R.string.pref_reset_counter_summary),
        enabled = true,
        onClick = onResetCounters,
        shape = shapeForPosition(3, 2),
    )
}

@Composable
private fun MaintenanceRow(
    icon: ImageVector,
    title: String,
    summary: String,
    enabled: Boolean,
    onClick: () -> Unit,
    shape: Shape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(enabled = enabled, onClick = onClick)
                    .alpha(if (enabled) 1f else 0.38f)
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    titleRes: Int,
    bodyRes: Int,
    confirmRes: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(bodyRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(confirmRes))
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
private fun ComboCard(state: VerifyUiState) {
    val scheme = MaterialTheme.colorScheme
    val mappedVersionCode = (state.lastResult as? VerifyResult.Success)?.versionCode
    val mappedModuleVersion = state.scanModuleVersion
    val name = state.installedAgsaVersionName
    val code = state.installedAgsaVersion
    val agsaLine =
        when {
            name != null && code != null -> {
                stringResource(R.string.diag_mapped_agsa_versioned, name, code)
            }

            name != null -> {
                stringResource(R.string.hero_target_agsa, "v$name")
            }

            mappedVersionCode != null -> {
                stringResource(R.string.diag_mapped_agsa_code_only, mappedVersionCode)
            }

            else -> {
                stringResource(R.string.diag_mapped_agsa_empty)
            }
        }
    val moduleLine =
        when {
            mappedModuleVersion > 0 && mappedModuleVersion != BuildConfig.VERSION_CODE -> {
                stringResource(R.string.diag_mapped_module_code_only, mappedModuleVersion)
            }

            else -> {
                stringResource(
                    R.string.diag_mapped_module_versioned,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                )
            }
        }

    Text(
        text = stringResource(R.string.diag_mapped_combo_title),
        style = MaterialTheme.typography.titleSmall,
        color = scheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))

    val rows =
        listOf(
            stringResource(R.string.diag_mapped_google_app_label) to agsaLine,
            stringResource(R.string.diag_mapped_module_label) to moduleLine,
        )
    rows.forEachIndexed { index, (label, value) ->
        if (index > 0) Spacer(Modifier.height(2.dp))
        GroupedSurface(count = rows.size, index = index) {
            MappingRow(label = label, value = value)
        }
    }
}

@Composable
private fun GroupedSurface(
    count: Int,
    index: Int,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeForPosition(count, index),
        color = MaterialTheme.colorScheme.surfaceVariant,
        content = content,
    )
}

@Composable
private fun MappingRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SymbolSections(sections: List<SymbolSection>) {
    sections.forEachIndexed { sectionIndex, section ->
        if (sectionIndex > 0) {
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text =
                stringResource(
                    R.string.diag_section_summary,
                    section.title,
                    section.resolvedCount,
                    section.totalCount,
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))

        val rowCount = section.rows.size
        section.rows.forEachIndexed { rowIndex, row ->
            GroupedSurface(count = rowCount, index = rowIndex) {
                ResolvedSymbolRow(
                    row = row,
                    modifier =
                        Modifier.padding(
                            horizontal = Spacing.md,
                            vertical = Spacing.sm,
                        ),
                )
            }
            if (rowIndex < rowCount - 1) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Preview(name = "Diagnostics - All mapped", showSystemUi = true)
@Composable
private fun DiagnosticsAllMappedPreview() {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        DiagnosticsScreenContent(
            state = PreviewFixtures.verifySuccessFull(),
            onVerify = {},
            onBack = {},
        )
    }
}

@Preview(name = "Diagnostics - Needs scan", showSystemUi = true)
@Composable
private fun DiagnosticsNeedsScanPreview() {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        DiagnosticsScreenContent(
            state = PreviewFixtures.verifyNeedsScan(),
            onVerify = {},
            onBack = {},
        )
    }
}
