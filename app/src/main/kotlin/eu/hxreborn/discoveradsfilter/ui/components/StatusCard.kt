@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import android.content.Intent
import android.os.Process
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
import eu.hxreborn.discoveradsfilter.ui.theme.Tokens
import eu.hxreborn.discoveradsfilter.ui.theme.isReducedMotion

private enum class BadgeKind {
    Healthy,
    Busy,
    Attention,
    Problem,
}

private data class StatusContent(
    val title: String,
    val body: List<String>,
    val badge: BadgeKind,
)

private val BadgeSize = 56.dp
private val BadgeIconSize = 28.dp

@Composable
private fun statusContent(state: VerifyUiState): StatusContent {
    val scanning = state.phase == VerifyPhase.Running && state.lastResult == null
    val stale = state.agsaUpdatedSinceScan() || state.moduleUpdatedSinceScan(BuildConfig.VERSION_CODE)
    return when {
        state.moduleStatus == ModuleStatus.Inactive -> {
            StatusContent(
                stringResource(R.string.hero_module_not_active),
                listOf(stringResource(R.string.hero_module_not_active_detail)),
                BadgeKind.Problem,
            )
        }

        state.installedAgsaVersion == null -> {
            StatusContent(stringResource(R.string.hero_target_missing), emptyList(), BadgeKind.Problem)
        }

        scanning -> {
            StatusContent(
                stringResource(R.string.hero_scanning),
                listOf(stringResource(R.string.hero_scanning_detail)) + counterLines(state),
                BadgeKind.Busy,
            )
        }

        state.phase == VerifyPhase.Running -> {
            healthyContent(state)
        }

        stale && state.lastResult is VerifyResult.Success -> {
            StatusContent(stringResource(R.string.hero_stale), counterLines(state), BadgeKind.Attention)
        }

        state.lastResult == null -> {
            StatusContent(
                stringResource(R.string.hero_scan_required),
                listOf(stringResource(R.string.hero_scan_required_detail)) + counterLines(state),
                BadgeKind.Attention,
            )
        }

        state.lastResult is VerifyResult.Failure -> {
            StatusContent(
                stringResource(R.string.hero_signatures_missing),
                counterLines(state),
                BadgeKind.Attention,
            )
        }

        else -> {
            healthyContent(state)
        }
    }
}

@Composable
private fun healthyContent(state: VerifyUiState): StatusContent {
    val framework = state.framework
    val frameworkLine =
        framework?.let {
            stringResource(R.string.status_framework, it.name, it.versionName, it.apiVersion)
        }
    return StatusContent(
        stringResource(R.string.status_active_title),
        listOfNotNull(frameworkLine) + counterLines(state),
        BadgeKind.Healthy,
    )
}

@Composable
private fun counterLines(state: VerifyUiState): List<String> {
    val verbs = stringArrayResource(R.array.clickbait_verbs)
    val verb = remember { verbs.random() }
    val adsLine =
        if (state.adsHidden > 0 || state.lastResult is VerifyResult.Success) {
            stringResource(R.string.hero_blocked_since_install, state.adsHidden)
        } else {
            null
        }
    val newsLine =
        if (state.newsHidden > 0) {
            pluralStringResource(
                R.plurals.hero_clickbait_hidden,
                state.newsHidden.toInt(),
                state.newsHidden,
                verb,
            )
        } else {
            null
        }
    return listOfNotNull(adsLine, newsLine)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusCard(
    state: VerifyUiState,
    modifier: Modifier = Modifier,
    onOpenDiagnostics: (() -> Unit)? = null,
) {
    val content = statusContent(state)
    val colors = MaterialTheme.colorScheme
    val isInactive = state.moduleStatus == ModuleStatus.Inactive
    val problem = content.badge == BadgeKind.Problem
    val attention = content.badge == BadgeKind.Attention
    val containerColor by animateColorAsState(
        targetValue =
            when {
                problem -> colors.errorContainer
                attention -> colors.secondaryContainer
                else -> colors.surfaceContainerHigh
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "statusContainer",
    )
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val context = LocalContext.current
    val restartLabel = stringResource(R.string.action_restart_app)
    val diagnosticsLabel = stringResource(R.string.nav_diagnostics_open)
    val shape = RoundedCornerShape(Tokens.OuterRowCorner)
    val clickable =
        when {
            isInactive -> {
                Modifier.clickable(onClickLabel = restartLabel) {
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
        }
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Tokens.ScreenMargin),
        shape = shape,
        color = containerColor,
    ) {
        Column(modifier = clickable) {
            AnimatedContent(
                targetState = content,
                transitionSpec = {
                    fadeIn(effects) togetherWith fadeOut(effects) using SizeTransform(clip = false)
                },
                label = "statusContent",
            ) { shown ->
                Row(
                    modifier = Modifier.padding(Tokens.RowPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.RowPadding),
                ) {
                    StatusBadge(shown.badge)
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    ) {
                        val onError = shown.badge == BadgeKind.Problem
                        val onAttention = shown.badge == BadgeKind.Attention
                        val contentColor =
                            when {
                                onError -> colors.onErrorContainer
                                onAttention -> colors.onSecondaryContainer
                                else -> colors.onSurfaceVariant
                            }
                        Text(
                            text = shown.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (onError || onAttention) contentColor else colors.onSurface,
                        )
                        shown.body.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
            if (state.phase == VerifyPhase.Running) {
                LinearWavyProgressIndicator(
                    progress = { state.scanProgress.size.toFloat() / VerifyUiState.TOTAL_TARGETS },
                    color = colors.primary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = Tokens.RowPadding, end = Tokens.RowPadding, bottom = Tokens.RowPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusBadge(kind: BadgeKind) {
    val colors = MaterialTheme.colorScheme
    when (kind) {
        BadgeKind.Healthy -> {
            val pulse = rememberLogoPulse()
            MorphingLogo(
                pulse = pulse,
                modifier =
                    Modifier
                        .size(BadgeSize)
                        .pointerInput(pulse) { detectTapGestures { pulse.tap() } },
            ) {
                Box(Modifier.matchParentSize().background(colors.primary))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(BadgeIconSize),
                )
            }
        }

        BadgeKind.Busy -> {
            if (isReducedMotion()) {
                Box(
                    Modifier
                        .size(BadgeSize)
                        .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
                )
            } else {
                val loadingDesc = stringResource(R.string.loading)
                LoadingIndicator(
                    modifier = Modifier.size(BadgeSize).semantics { contentDescription = loadingDesc },
                )
            }
        }

        BadgeKind.Attention -> {
            Box(
                modifier =
                    Modifier
                        .size(BadgeSize)
                        .background(colors.secondary, MaterialShapes.Sunny.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PriorityHigh,
                    contentDescription = null,
                    tint = colors.onSecondary,
                    modifier = Modifier.size(BadgeIconSize),
                )
            }
        }

        BadgeKind.Problem -> {
            Box(
                modifier =
                    Modifier
                        .size(BadgeSize)
                        .background(colors.error, MaterialShapes.Sunny.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PriorityHigh,
                    contentDescription = null,
                    tint = colors.onError,
                    modifier = Modifier.size(BadgeIconSize),
                )
            }
        }
    }
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
