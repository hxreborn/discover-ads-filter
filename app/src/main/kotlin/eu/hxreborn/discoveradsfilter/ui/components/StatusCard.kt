@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import android.content.Intent
import android.os.Process
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.screen.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.agsaUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.state.moduleUpdatedSinceScan
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusCard(
    state: VerifyUiState,
    modifier: Modifier = Modifier,
) {
    val visual = statusVisual(state)
    val scanning = state.phase == VerifyPhase.Running && state.lastResult == null
    val isInactive = state.moduleStatus == ModuleStatus.Inactive
    val context = LocalContext.current

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                .let { base ->
                    if (isInactive) {
                        base.clickable {
                            val intent =
                                context.packageManager
                                    .getLaunchIntentForPackage(context.packageName)
                                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) }
                            intent?.let { context.startActivity(it) }
                            Process.killProcess(Process.myPid())
                        }
                    } else {
                        base
                    }
                },
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
            Box(modifier = Modifier.size(IconSize.lg), contentAlignment = Alignment.Center) {
                if (scanning) {
                    LoadingIndicator(modifier = Modifier.size(IconSize.lg))
                } else {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.lg),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(visual.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusCardBody(state)
            }
            if (isInactive) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm),
                )
            }
        }
    }
}

@Composable
private fun StatusCardBody(state: VerifyUiState) {
    when {
        state.moduleStatus == ModuleStatus.Inactive -> {
            Text(
                text = stringResource(R.string.hero_module_not_active_detail),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        else -> {
            Text(
                text = targetLine(state),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.phase == VerifyPhase.Running && state.lastResult == null) {
                Text(
                    text = stringResource(R.string.hero_scanning_detail),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (state.lastResult == null && state.moduleStatus == ModuleStatus.Active) {
                Text(
                    text = stringResource(R.string.hero_scan_required_detail),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.adsHidden > 0 || state.lastResult is VerifyResult.Success) {
                Text(
                    text = stringResource(R.string.hero_blocked_since_install, state.adsHidden),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
    val moduleActive = state.moduleStatus == ModuleStatus.Active

    if (state.moduleStatus == ModuleStatus.Inactive) {
        return StatusVisual(
            icon = Icons.Outlined.ErrorOutline,
            titleRes = R.string.hero_module_not_active,
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
        )
    }

    if (moduleActive && state.installedAgsaVersion == null) {
        return StatusVisual(
            icon = Icons.Outlined.ErrorOutline,
            titleRes = R.string.hero_target_missing,
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
        )
    }

    if (state.phase == VerifyPhase.Running && state.lastResult != null) {
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

    if (state.phase == VerifyPhase.Running) {
        return StatusVisual(
            icon = Icons.Filled.CheckCircle,
            titleRes = R.string.hero_scanning,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        )
    }

    if (state.lastResult == null) return noResultVisual(scheme, moduleActive)
    if (state.lastResult is VerifyResult.Failure) return failureVisual(scheme, moduleActive)

    val elevation = if (state.adsHidden > 0) 3.dp else 0.dp
    return StatusVisual(
        icon = Icons.Filled.CheckCircle,
        titleRes = R.string.hero_module_active,
        container = scheme.primaryContainer,
        content = scheme.onPrimaryContainer,
        tonalElevation = elevation,
    )
}

private fun noResultVisual(
    scheme: ColorScheme,
    moduleActive: Boolean,
): StatusVisual =
    if (moduleActive) {
        StatusVisual(
            icon = Icons.Outlined.Warning,
            titleRes = R.string.hero_scan_required,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        )
    } else {
        StatusVisual(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            titleRes = R.string.hero_not_configured,
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
        )
    }

private fun failureVisual(
    scheme: ColorScheme,
    moduleActive: Boolean,
): StatusVisual =
    if (moduleActive) {
        StatusVisual(
            icon = Icons.Outlined.Warning,
            titleRes = R.string.hero_signatures_missing,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        )
    } else {
        StatusVisual(
            icon = Icons.Outlined.ErrorOutline,
            titleRes = R.string.hero_scan_failed,
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
        )
    }

// region Previews

private class StatusCardStateProvider : PreviewParameterProvider<VerifyUiState> {
    override val values: Sequence<VerifyUiState> =
        sequenceOf(
            PreviewFixtures.verifySuccessFull(),
            PreviewFixtures.verifyNeedsScan(),
            PreviewFixtures.verifyFailureDexKitNoMatches(),
            PreviewFixtures.verifyModuleNotActive(),
        )
}

@Preview(name = "Status Card", showBackground = true)
@Composable
private fun StatusCardPreview(
    @PreviewParameter(StatusCardStateProvider::class) state: VerifyUiState,
) {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        StatusCard(state = state)
    }
}

// endregion
