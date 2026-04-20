package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.state.HookCoverage
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.agsaUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.state.hookCoverage
import eu.hxreborn.discoveradsfilter.ui.state.moduleUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.theme.ProgressStroke
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@Composable
private fun VerifyButton(
    running: Boolean,
    hasResult: Boolean,
    onVerify: () -> Unit,
) {
    FilledTonalButton(onClick = onVerify, enabled = !running) {
        if (running) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                strokeWidth = ProgressStroke.sm,
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(stringResource(R.string.button_scanning))
        } else {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                stringResource(if (hasResult) R.string.button_reverify else R.string.button_verify),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun VerifyCard(
    state: VerifyUiState,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val running = state.phase == VerifyPhase.Running
    val visual = statusVisual(state)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
        shape = MaterialTheme.shapes.large,
        color = visual.containerColor,
        contentColor = visual.contentColor,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Column {
            if (running) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.lg),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = stateLine(state),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = bindingLine(state),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val impact = impactLine(state)
                    if (impact != null) {
                        Text(
                            text = impact,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                VerifyButton(
                    running = running,
                    hasResult = state.lastResult != null,
                    onVerify = onVerify,
                )
            }
        }
    }
}

@Composable
private fun stateLine(state: VerifyUiState): String {
    val stale =
        state.agsaUpdatedSinceScan() ||
            state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)
    if (stale) return "Stale scan"

    return when (state.hookCoverage()) {
        HookCoverage.Full -> {
            "Hooks active"
        }

        HookCoverage.FallbackOnly -> {
            "Fallback only"
        }

        HookCoverage.None -> {
            stringResource(R.string.coverage_none)
        }

        HookCoverage.NotScanned -> {
            "Not configured"
        }

        HookCoverage.ScanFailed -> {
            stringResource(R.string.coverage_scan_failed)
        }
    }
}

@Composable
private fun bindingLine(state: VerifyUiState): String {
    if (state.agsaUpdatedSinceScan()) return stringResource(R.string.hook_agsa_updated)
    if (state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)) {
        return stringResource(R.string.hook_module_updated)
    }

    return when (state.hookCoverage()) {
        HookCoverage.Full -> {
            val hooks = state.hookInstallStatus
            val version = (state.lastResult as? VerifyResult.Success)?.versionCode
            when {
                hooks != null && version != null -> "$hooks hooks \u00B7 AGSA $version"
                version != null -> "AGSA $version"
                else -> stringResource(R.string.coverage_full_detail)
            }
        }

        HookCoverage.FallbackOnly -> {
            stringResource(R.string.coverage_fallback_detail)
        }

        HookCoverage.None -> {
            stringResource(R.string.coverage_none_detail)
        }

        HookCoverage.NotScanned -> {
            stringResource(R.string.hook_not_scanned_message)
        }

        HookCoverage.ScanFailed -> {
            (state.lastResult as? VerifyResult.Failure)?.reason ?: stringResource(R.string.coverage_scan_failed)
        }
    }
}

private fun impactLine(state: VerifyUiState): String? {
    val coverage = state.hookCoverage()
    if (coverage != HookCoverage.Full && coverage != HookCoverage.FallbackOnly) return null
    val ads = state.adsHidden
    return if (ads > 0) "$ads ads hidden" else "No ads detected"
}

private data class StatusVisual(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun statusVisual(state: VerifyUiState): StatusVisual {
    val scheme = MaterialTheme.colorScheme
    return when (state.hookCoverage()) {
        HookCoverage.Full -> {
            StatusVisual(
                icon = Icons.Outlined.CheckCircle,
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
            )
        }

        HookCoverage.FallbackOnly -> {
            StatusVisual(
                icon = Icons.Outlined.Warning,
                containerColor = scheme.tertiary,
                contentColor = scheme.onTertiary,
            )
        }

        HookCoverage.None -> {
            StatusVisual(
                icon = Icons.Outlined.Block,
                containerColor = scheme.error,
                contentColor = scheme.onError,
            )
        }

        HookCoverage.NotScanned -> {
            StatusVisual(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                containerColor = scheme.surfaceContainerHighest,
                contentColor = scheme.onSurfaceVariant,
            )
        }

        HookCoverage.ScanFailed -> {
            StatusVisual(
                icon = Icons.Outlined.ErrorOutline,
                containerColor = scheme.error,
                contentColor = scheme.onError,
            )
        }
    }
}
