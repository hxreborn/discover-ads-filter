@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = (uiState as? HomeUiState.Ready)?.verify ?: VerifyUiState()

    DiagnosticsScreenContent(
        state = state,
        onVerify = viewModel.actions.onVerify,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreenContent(
    state: VerifyUiState,
    onVerify: () -> Unit,
    onBack: () -> Unit,
) {
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showManualProgressThisVisit by rememberSaveable { mutableStateOf(false) }
    val anyRunning = state.phase == VerifyPhase.Running
    val manualRunning = anyRunning && state.scanOrigin == ScanOrigin.Manual

    LaunchedEffect(manualRunning) {
        if (manualRunning) showManualProgressThisVisit = true
    }

    val showProgress = showManualProgressThisVisit && (manualRunning || state.scanProgress.isNotEmpty())

    SettingsDetailScaffold(
        title = stringResource(R.string.nav_diagnostics),
        onBack = onBack,
        actions = {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiagnosticsFabHost(
    anyRunning: Boolean,
    hasCachedScan: Boolean,
    onTap: () -> Unit,
) {
    val fabEnabled = !anyRunning
    val onTapState by rememberUpdatedState(onTap)
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (fabEnabled && isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 70),
        label = "diagnostics_fab_press_scale",
    )

    DiagnosticsFab(
        anyRunning = anyRunning,
        hasCachedScan = hasCachedScan,
        onTap = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onTapState()
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
    val baseModifier = modifier.animateContentSize(animationSpec = tween(durationMillis = 140))
    val fabModifier = if (fabEnabled) baseModifier else baseModifier.alpha(0.7f).semantics { disabled() }
    val idleFabLabel = stringResource(if (hasCachedScan) R.string.button_rescan else R.string.button_scan)
    val runningFabLabel = stringResource(R.string.fab_resolving)

    ExtendedFloatingActionButton(
        onClick = { if (fabEnabled) onTap() },
        modifier = fabModifier,
        containerColor = fabContainerColor,
        contentColor = fabContentColor,
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

    Spacer(Modifier.height(96.dp))
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

    val fpSchema = SettingsPrefs.KEY_FINGERPRINT_PREFIX.removePrefix("fp_v").removeSuffix("_")
    val fpLine = stringResource(R.string.diag_mapped_fp_value, fpSchema)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeForPosition(3, 0),
        color = scheme.surfaceVariant,
    ) {
        MappingRow(
            label = stringResource(R.string.diag_mapped_google_app_label),
            value = agsaLine,
        )
    }
    Spacer(Modifier.height(2.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeForPosition(3, 1),
        color = scheme.surfaceVariant,
    ) {
        MappingRow(
            label = stringResource(R.string.diag_mapped_module_label),
            value = moduleLine,
        )
    }
    Spacer(Modifier.height(2.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeForPosition(3, 2),
        color = scheme.surfaceVariant,
    ) {
        MappingRow(
            label = stringResource(R.string.diag_mapped_fp_label),
            value = fpLine,
        )
    }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shapeForPosition(rowCount, rowIndex),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                ResolvedSymbolRow(
                    row = row,
                    modifier =
                        Modifier.padding(
                            horizontal = Spacing.md,
                            vertical = 8.dp,
                        ),
                )
            }
            if (rowIndex < rowCount - 1) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// region Previews

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

// endregion
