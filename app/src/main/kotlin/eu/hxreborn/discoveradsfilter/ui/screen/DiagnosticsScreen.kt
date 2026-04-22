@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.ResolvedSymbolRow
import eu.hxreborn.discoveradsfilter.ui.components.ScanProgressCard
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailScaffold
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.ScanOrigin
import eu.hxreborn.discoveradsfilter.ui.state.SymbolSection
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.agsaUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.state.moduleUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.state.toSymbolSections
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
    var showManualProgressThisVisit by remember { mutableStateOf(false) }
    val anyRunning = state.phase == VerifyPhase.Running
    val manualRunning = anyRunning && state.scanOrigin == ScanOrigin.Manual
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
            ExtendedFloatingActionButton(
                onClick = {
                    if (!anyRunning) {
                        showManualProgressThisVisit = true
                        onVerify()
                    }
                },
            ) {
                AnimatedContent(
                    targetState = manualRunning,
                    label = "fab-content",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { isRunning ->
                    if (isRunning) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.button_reverify))
                    }
                }
            }
        },
    ) {
        DiagnosticsContent(
            state = state,
            sections = state.toSymbolSections(),
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

@Composable
internal fun DiagnosticsContent(
    state: VerifyUiState,
    sections: List<SymbolSection>,
    showProgress: Boolean = false,
) {
    ScanSummaryCard(state = state, sections = sections)
    Spacer(Modifier.height(Spacing.md))

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
            text = "Last background refresh failed: ${state.lastRefreshError}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(96.dp))
}

@Composable
private fun ScanSummaryCard(
    state: VerifyUiState,
    sections: List<SymbolSection>,
) {
    val scheme = MaterialTheme.colorScheme
    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)
    val resolved = sections.sumOf { it.resolvedCount }
    val total = sections.sumOf { it.totalCount }

    val (icon, label, container, content) =
        when {
            state.lastResult == null -> {
                SummaryVisual(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    label = stringResource(R.string.diag_summary_no_scan),
                    container = scheme.surfaceContainerHighest,
                    content = scheme.onSurfaceVariant,
                )
            }

            stale -> {
                SummaryVisual(
                    icon = Icons.Outlined.Warning,
                    label = stringResource(R.string.diag_summary_stale),
                    container = scheme.secondaryContainer,
                    content = scheme.onSecondaryContainer,
                )
            }

            state.lastResult is VerifyResult.Failure -> {
                SummaryVisual(
                    icon = Icons.Outlined.ErrorOutline,
                    label = stringResource(R.string.diag_summary_none),
                    container = scheme.errorContainer,
                    content = scheme.onErrorContainer,
                )
            }

            resolved == total -> {
                SummaryVisual(
                    icon = Icons.Filled.CheckCircle,
                    label = stringResource(R.string.diag_summary_all_mapped),
                    container = scheme.primaryContainer,
                    content = scheme.onPrimaryContainer,
                )
            }

            else -> {
                SummaryVisual(
                    icon = Icons.Outlined.Warning,
                    label = stringResource(R.string.diag_summary_partial, resolved, total),
                    container = scheme.tertiaryContainer,
                    content = scheme.onTertiaryContainer,
                )
            }
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

private data class SummaryVisual(
    val icon: ImageVector,
    val label: String,
    val container: Color,
    val content: Color,
)

@Composable
private fun ComboCard(state: VerifyUiState) {
    val scheme = MaterialTheme.colorScheme
    val mappedVersionCode = (state.lastResult as? VerifyResult.Success)?.versionCode
    val mappedModuleVersion = state.scanModuleVersion
    val agsaLine =
        when {
            state.installedAgsaVersionName != null -> {
                stringResource(
                    R.string.diag_mapped_agsa_versioned,
                    state.installedAgsaVersionName,
                    state.installedAgsaVersion!!,
                )
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
        shape = shapeForPosition(2, 1),
        color = scheme.surfaceVariant,
    ) {
        MappingRow(
            label = stringResource(R.string.diag_mapped_module_label),
            value = moduleLine,
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
