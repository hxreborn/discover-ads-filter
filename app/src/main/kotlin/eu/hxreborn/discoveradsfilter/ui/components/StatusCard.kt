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
import androidx.compose.material.icons.outlined.Block
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
                Text(
                    text = targetLine(state),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val versionCode = state.installedAgsaVersion
                if (versionCode != null) {
                    Text(
                        text = "build $versionCode",
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

@Composable
private fun targetLine(state: VerifyUiState): String {
    val name = state.installedAgsaVersionName
    val code = state.installedAgsaVersion
    return when {
        name != null -> stringResource(R.string.hero_target_agsa, "v$name")
        code != null -> stringResource(R.string.hero_target_agsa, "v$code")
        else -> stringResource(R.string.hero_target_missing)
    }
}

private data class StatusVisual(
    val icon: ImageVector,
    val titleRes: Int,
    val container: Color,
    val content: Color,
)

@Composable
private fun statusVisual(state: VerifyUiState): StatusVisual {
    val scheme = MaterialTheme.colorScheme
    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)
    val refreshing = state.phase == VerifyPhase.Running

    if (stale && !refreshing && state.lastResult is VerifyResult.Success) {
        return StatusVisual(
            icon = Icons.Outlined.Warning,
            titleRes = R.string.hero_stale,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        )
    }

    return when (state.hookCoverage()) {
        HookCoverage.Full -> {
            val titleRes =
                if (state.hookInstalled > 0 &&
                    state.adsHidden > 0
                ) {
                    R.string.hero_hooks_active_verified
                } else {
                    R.string.hero_hooks_active
                }
            StatusVisual(Icons.Filled.CheckCircle, titleRes, scheme.primaryContainer, scheme.onPrimaryContainer)
        }

        HookCoverage.FallbackOnly -> {
            StatusVisual(Icons.Outlined.Warning, R.string.hero_fallback_only, scheme.secondaryContainer, scheme.onSecondaryContainer)
        }

        HookCoverage.None -> {
            StatusVisual(Icons.Outlined.Block, R.string.hero_no_targets, scheme.errorContainer, scheme.onErrorContainer)
        }

        HookCoverage.NotScanned -> {
            StatusVisual(
                Icons.AutoMirrored.Outlined.HelpOutline,
                R.string.hero_not_configured,
                scheme.surfaceContainerHighest,
                scheme.onSurfaceVariant,
            )
        }

        HookCoverage.ScanFailed -> {
            StatusVisual(Icons.Outlined.ErrorOutline, R.string.hero_scan_failed, scheme.errorContainer, scheme.onErrorContainer)
        }
    }
}
