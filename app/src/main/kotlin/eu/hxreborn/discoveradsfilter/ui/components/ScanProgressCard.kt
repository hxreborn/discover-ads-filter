@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.state.ScanStep
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScanProgressCard(
    progress: List<ScanStep>,
    phase: VerifyPhase,
    modifier: Modifier = Modifier,
    durationMs: Long = 0,
    showRawValues: Boolean = false,
) {
    val running = phase == VerifyPhase.Running
    val completed = progress.size
    val done = !running && completed > 0
    val totalSteps = VerifyUiState.TOTAL_TARGETS
    val showActive = running && completed < totalSteps
    val segmentCount = 1 + completed + if (showActive) 1 else 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.segmentGap),
    ) {
        Segment(count = segmentCount, index = 0) {
            Header(
                done = done,
                running = running,
                completed = completed,
                totalSteps = totalSteps,
                durationMs = durationMs,
            )
        }
        progress.forEachIndexed { index, step ->
            val state = remember(step) { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn() + slideInVertically { it / 2 },
            ) {
                Segment(count = segmentCount, index = index + 1) {
                    StepRow(step = step, showRawValue = showRawValues)
                }
            }
        }
        if (showActive) {
            Segment(count = segmentCount, index = segmentCount - 1) {
                ActiveStepRow(label = scanStepLabel(stepIndex = completed))
            }
        }
    }
}

@Composable
private fun Segment(
    count: Int,
    index: Int,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeForPosition(count, index),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            content()
        }
    }
}

@Composable
private fun scanStepLabel(stepIndex: Int): String =
    when (stepIndex) {
        0 -> stringResource(R.string.scan_step_ad_metadata)
        1 -> stringResource(R.string.scan_step_feed_card)
        2 -> stringResource(R.string.scan_step_ad_flag)
        3 -> stringResource(R.string.scan_step_ad_label)
        4 -> stringResource(R.string.scan_step_ad_metadata_ref)
        5 -> stringResource(R.string.scan_step_card_processors)
        6 -> stringResource(R.string.scan_step_stream_list)
        else -> stringResource(R.string.scan_step_generic, stepIndex + 1)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header(
    done: Boolean,
    running: Boolean,
    completed: Int,
    totalSteps: Int,
    durationMs: Long,
) {
    val scheme = MaterialTheme.colorScheme
    val titleText = if (done) stringResource(R.string.scan_complete) else stringResource(R.string.scan_verifying)
    val subtitleText = if (done) "%.1fs".format(durationMs / 1000f) else stringResource(R.string.scan_progress, completed, totalSteps)
    val subtitleFontFamily = if (done) FontFamily.Monospace else FontFamily.Default

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (done) {
                SoftBlobBadge(
                    size = IconSize.lg,
                    shape = MaterialShapes.Sunny.toShape(),
                    containerColor = scheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(IconSize.sm),
                    )
                }
            } else {
                LoadingIndicator(modifier = Modifier.size(IconSize.lg))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = titleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = subtitleFontFamily),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (running) {
            LinearWavyProgressIndicator(
                progress = { completed.toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepRow(
    step: ScanStep,
    showRawValue: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = step.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        TrailingValueAndBadge(
            rawValue = step.rawValue,
            showRawValue = showRawValue,
            badgeColor = if (step.resolved) scheme.secondaryContainer else scheme.errorContainer,
            badgeContentColor = if (step.resolved) scheme.onSecondaryContainer else scheme.onErrorContainer,
            badgeLabel =
                if (step.resolved) {
                    stringResource(R.string.scan_step_found)
                } else {
                    stringResource(R.string.scan_step_not_found)
                },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActiveStepRow(label: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = scheme.onSurface,
        )

        Surface(
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            shape = CircleShape,
        ) {
            CircularWavyProgressIndicator(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp).size(14.dp),
            )
        }
    }
}

@Composable
private fun TrailingValueAndBadge(
    rawValue: String?,
    showRawValue: Boolean,
    badgeColor: Color,
    badgeContentColor: Color,
    badgeLabel: String,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showRawValue && rawValue != null) {
            Text(
                text = rawValue,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                color = scheme.onSurfaceVariant,
            )
        }

        Surface(
            color = badgeColor,
            contentColor = badgeContentColor,
            shape = CircleShape,
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}
