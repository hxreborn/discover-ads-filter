package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.agsaUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.state.moduleUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@Composable
fun StatusCard(
    state: VerifyUiState,
    modifier: Modifier = Modifier,
) {
    val visual = statusVisual(state)

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        shape = MaterialTheme.shapes.large,
        color = visual.container,
        contentColor = visual.content,
        tonalElevation = visual.tonalElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
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
                    text = stringResource(visual.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.moduleStatus == ModuleStatus.Inactive) {
                    Text(
                        text = stringResource(R.string.hero_module_not_active_detail),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = targetLine(state),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val hookLine = hookStatusLine(state)
                    if (hookLine != null) {
                        Text(
                            text = hookLine,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (state.adsHidden > 0) {
                        Text(
                            text = stringResource(R.string.hero_blocked_since_install, state.adsHidden),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun targetLine(state: VerifyUiState): String {
    val name = state.installedAgsaVersionName
    val code = state.installedAgsaVersion
    return when {
        name != null && code != null -> stringResource(R.string.hero_target_agsa, "v$name ($code)")
        name != null -> stringResource(R.string.hero_target_agsa, "v$name")
        code != null -> stringResource(R.string.hero_target_agsa, "v$code")
        else -> stringResource(R.string.hero_target_missing)
    }
}

@Composable
private fun hookStatusLine(state: VerifyUiState): String? {
    if (state.hookInstalled == 0 && state.hookTotal == 0) return null
    return stringResource(R.string.hero_hooks_line, state.hookInstalled, state.hookTotal)
}

private data class StatusVisual(
    val icon: ImageVector,
    val titleRes: Int,
    val container: Color,
    val content: Color,
    val tonalElevation: Dp = 0.dp,
)

@Composable
private fun statusVisual(state: VerifyUiState): StatusVisual {
    val scheme = MaterialTheme.colorScheme

    if (state.moduleStatus == ModuleStatus.Inactive) {
        return StatusVisual(
            icon = Icons.Outlined.ErrorOutline,
            titleRes = R.string.hero_module_not_active,
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
        )
    }

    if (state.phase == VerifyPhase.Running) {
        return StatusVisual(
            icon = Icons.Filled.CheckCircle,
            titleRes = R.string.hero_module_active,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        )
    }

    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)

    if (stale && state.lastResult is VerifyResult.Success) {
        return StatusVisual(
            icon = Icons.Outlined.Warning,
            titleRes = R.string.hero_stale,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        )
    }

    if (state.lastResult == null) {
        return StatusVisual(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            titleRes = R.string.hero_not_configured,
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
        )
    }

    if (state.lastResult is VerifyResult.Failure) {
        return StatusVisual(
            icon = Icons.Outlined.ErrorOutline,
            titleRes = R.string.hero_scan_failed,
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
        )
    }

    if (state.hookInstalled == 0 && state.hookTotal == 0 && state.adsHidden == 0L) {
        return StatusVisual(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            titleRes = R.string.hero_hooks_pending,
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
        )
    }

    val elevation = if (state.adsHidden > 0) 3.dp else 0.dp
    return StatusVisual(
        icon = Icons.Filled.CheckCircle,
        titleRes = R.string.hero_module_active,
        container = scheme.primaryContainer,
        content = scheme.onPrimaryContainer,
        tonalElevation = elevation,
    )
}
