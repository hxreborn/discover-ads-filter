package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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

private const val PROGRESS_TRACK_ALPHA = 0.24f

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun HeroStatusCard(
    state: VerifyUiState,
    modifier: Modifier = Modifier,
) {
    val visual = heroVisual(state)
    val running = state.phase == VerifyPhase.Running

    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = visual.container,
                contentColor = visual.content,
            ),
    ) {
        Column {
            if (running) {
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = visual.accent,
                    trackColor = visual.accent.copy(alpha = PROGRESS_TRACK_ALPHA),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
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
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = targetLine(state),
                        style = MaterialTheme.typography.bodyMedium,
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
        name != null -> "Google v$name"
        code != null -> stringResource(R.string.hero_target_agsa, "v$code")
        else -> stringResource(R.string.hero_target_missing)
    }
}

private data class HeroVisual(
    val icon: ImageVector,
    val titleRes: Int,
    val container: Color,
    val content: Color,
    val accent: Color,
)

@Composable
private fun heroVisual(state: VerifyUiState): HeroVisual {
    val scheme = MaterialTheme.colorScheme
    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)

    if (stale && state.lastResult is VerifyResult.Success) {
        return HeroVisual(
            icon = Icons.Outlined.Warning,
            titleRes = R.string.hero_stale,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            accent = scheme.secondary,
        )
    }

    return when (state.hookCoverage()) {
        HookCoverage.Full -> {
            val filtering = state.hookInstalled > 0 && state.adsHidden > 0
            if (filtering) {
                HeroVisual(
                    icon = Icons.Outlined.CheckCircle,
                    titleRes = R.string.hero_hooks_active_verified,
                    container = scheme.primaryContainer,
                    content = scheme.onPrimaryContainer,
                    accent = scheme.primary,
                )
            } else {
                HeroVisual(
                    icon = Icons.Outlined.CheckCircle,
                    titleRes = R.string.hero_hooks_active,
                    container = scheme.secondaryContainer,
                    content = scheme.onSecondaryContainer,
                    accent = scheme.secondary,
                )
            }
        }

        HookCoverage.FallbackOnly -> {
            HeroVisual(
                icon = Icons.Outlined.Warning,
                titleRes = R.string.hero_fallback_only,
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                accent = scheme.secondary,
            )
        }

        HookCoverage.None -> {
            HeroVisual(
                icon = Icons.Outlined.Block,
                titleRes = R.string.hero_no_targets,
                container = scheme.errorContainer,
                content = scheme.onErrorContainer,
                accent = scheme.error,
            )
        }

        HookCoverage.NotScanned -> {
            HeroVisual(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                titleRes = R.string.hero_not_configured,
                container = scheme.surfaceContainerHighest,
                content = scheme.onSurfaceVariant,
                accent = scheme.primary,
            )
        }

        HookCoverage.ScanFailed -> {
            HeroVisual(
                icon = Icons.Outlined.ErrorOutline,
                titleRes = R.string.hero_scan_failed,
                container = scheme.errorContainer,
                content = scheme.onErrorContainer,
                accent = scheme.error,
            )
        }
    }
}
