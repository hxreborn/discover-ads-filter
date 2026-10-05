@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import android.content.Intent
import android.os.Process
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
    onOpenDiagnostics: (() -> Unit)? = null,
) {
    val visual = statusVisual(state)
    val colors = MaterialTheme.colorScheme.colorsFor(visual.tone)
    val scanning = state.phase == VerifyPhase.Running && state.lastResult == null
    val isInactive = state.moduleStatus == ModuleStatus.Inactive
    val context = LocalContext.current
    val restartLabel = stringResource(R.string.action_restart_app)
    val diagnosticsLabel = stringResource(R.string.nav_diagnostics_open)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                .clip(MaterialTheme.shapes.extraLarge)
                .then(
                    when {
                        isInactive -> {
                            Modifier.clickable(
                                onClickLabel = restartLabel,
                            ) {
                                val intent =
                                    context.packageManager
                                        .getLaunchIntentForPackage(context.packageName)
                                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) }
                                intent?.let { context.startActivity(it) }
                                Process.killProcess(Process.myPid())
                            }
                        }

                        onOpenDiagnostics != null -> {
                            Modifier.clickable(onClickLabel = diagnosticsLabel) { onOpenDiagnostics() }
                        }

                        else -> {
                            Modifier
                        }
                    },
                ),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = colors.container,
                contentColor = colors.content,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (scanning) {
                    val loadingDesc = stringResource(R.string.loading)
                    LoadingIndicator(
                        color = colors.badge,
                        modifier =
                            Modifier.size(IconSize.badge).semantics {
                                contentDescription = loadingDesc
                            },
                    )
                } else {
                    SoftBlobBadge(
                        size = IconSize.badge,
                        shape = if (visual.tone == Tone.Healthy) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                        containerColor = colors.badge,
                    ) {
                        Icon(imageVector = visual.icon, contentDescription = null, tint = colors.onBadge)
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
                if (isInactive || onOpenDiagnostics != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.sm),
                    )
                }
            }
            if (state.phase == VerifyPhase.Running) {
                LinearWavyProgressIndicator(
                    progress = { state.scanProgress.size.toFloat() / VerifyUiState.TOTAL_TARGETS },
                    color = colors.badge,
                    modifier = Modifier.fillMaxWidth(),
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
            if (state.newsHidden > 0) {
                val verbs = stringArrayResource(R.array.clickbait_verbs)
                val verb = remember { verbs.random() }
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.hero_clickbait_hidden,
                            state.newsHidden.toInt(),
                            state.newsHidden,
                            verb,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private enum class Tone { Healthy, Attention, Error, Neutral }

private data class StatusVisual(
    val icon: ImageVector,
    val titleRes: Int,
    val tone: Tone,
)

private data class ToneColors(
    val container: Color,
    val content: Color,
    val badge: Color,
    val onBadge: Color,
)

private fun ColorScheme.colorsFor(tone: Tone): ToneColors =
    when (tone) {
        Tone.Healthy -> ToneColors(primaryContainer, onPrimaryContainer, primary, onPrimary)
        Tone.Attention -> ToneColors(secondaryContainer, onSecondaryContainer, secondary, onSecondary)
        Tone.Error -> ToneColors(errorContainer, onErrorContainer, error, onError)
        Tone.Neutral -> ToneColors(surfaceContainerHighest, onSurfaceVariant, onSurfaceVariant, surfaceContainerHighest)
    }

private fun statusVisual(state: VerifyUiState): StatusVisual {
    val moduleActive = state.moduleStatus == ModuleStatus.Active

    if (state.moduleStatus == ModuleStatus.Inactive) {
        return StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_module_not_active, Tone.Error)
    }

    if (moduleActive && state.installedAgsaVersion == null) {
        return StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_target_missing, Tone.Error)
    }

    if (state.phase == VerifyPhase.Running && state.lastResult != null) {
        return StatusVisual(Icons.Rounded.Check, R.string.hero_module_active, Tone.Healthy)
    }

    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)

    if (stale && state.lastResult is VerifyResult.Success) {
        return StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_stale, Tone.Attention)
    }

    if (state.phase == VerifyPhase.Running) {
        return StatusVisual(Icons.Rounded.Check, R.string.hero_scanning, Tone.Healthy)
    }

    if (state.lastResult == null) {
        return if (moduleActive) {
            StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_scan_required, Tone.Attention)
        } else {
            StatusVisual(Icons.Rounded.QuestionMark, R.string.hero_not_configured, Tone.Neutral)
        }
    }

    if (state.lastResult is VerifyResult.Failure) {
        return if (moduleActive) {
            StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_signatures_missing, Tone.Attention)
        } else {
            StatusVisual(Icons.Rounded.PriorityHigh, R.string.hero_scan_failed, Tone.Error)
        }
    }

    return StatusVisual(Icons.Rounded.Check, R.string.hero_module_active, Tone.Healthy)
}

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
