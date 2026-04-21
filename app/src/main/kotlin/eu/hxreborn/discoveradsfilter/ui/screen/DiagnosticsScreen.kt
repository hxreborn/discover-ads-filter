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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
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
import eu.hxreborn.discoveradsfilter.ui.state.ScanStep
import eu.hxreborn.discoveradsfilter.ui.state.SymbolSection
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
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
    var showInfoDialog by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showManualProgressThisVisit by remember { mutableStateOf(false) }
    val anyRunning = state.phase == VerifyPhase.Running
    val manualRunning =
        anyRunning && state.scanOrigin == ScanOrigin.Manual
    val showProgress =
        showManualProgressThisVisit &&
            (manualRunning || state.scanProgress.isNotEmpty())

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
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(stringResource(R.string.button_rescan))
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
    ComboCard(
        state = state,
    )
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

    Spacer(Modifier.height(80.dp))
}

@Composable
private fun ComboCard(state: VerifyUiState) {
    val scheme = MaterialTheme.colorScheme
    val mappedVersionCode = (state.lastResult as? VerifyResult.Success)?.versionCode
    val mappedModuleVersion = state.scanModuleVersion
    val agsaLine =
        when {
            mappedVersionCode == null -> {
                stringResource(R.string.diag_mapped_agsa_empty)
            }

            state.installedAgsaVersionName != null &&
                state.installedAgsaVersion == mappedVersionCode -> {
                stringResource(
                    R.string.diag_mapped_agsa_versioned,
                    state.installedAgsaVersionName,
                    mappedVersionCode,
                )
            }

            else -> {
                stringResource(R.string.diag_mapped_agsa_code_only, mappedVersionCode)
            }
        }
    val moduleLine =
        when {
            mappedModuleVersion <= 0 -> {
                stringResource(R.string.diag_mapped_module_empty)
            }

            mappedModuleVersion == BuildConfig.VERSION_CODE -> {
                stringResource(
                    R.string.diag_mapped_module_versioned,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                )
            }

            else -> {
                stringResource(R.string.diag_mapped_module_code_only, mappedModuleVersion)
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
            style = MaterialTheme.typography.titleSmall,
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
