@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

val SCAN_STEP_LABELS =
    listOf(
        "Ad metadata",
        "Feed card",
        "Ad flag",
        "Ad label",
        "Ad metadata ref",
        "Card processors",
        "Stream list",
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScanProgressCard(
    progress: List<ScanStep>,
    phase: VerifyPhase,
    totalSteps: Int = VerifyUiState.TOTAL_TARGETS,
    durationMs: Long = 0,
    showRawValues: Boolean = false,
    maxVisibleCompleted: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    val running = phase == VerifyPhase.Running
    val scheme = MaterialTheme.colorScheme
    val completed = progress.size
    val done = !running && completed > 0
    val visibleCompleted =
        if (maxVisibleCompleted < completed) {
            progress.takeLast(maxVisibleCompleted)
        } else {
            progress
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (running) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Header(
                done = done,
                completed = completed,
                totalSteps = totalSteps,
                durationMs = durationMs,
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = 8.dp)) {
                    visibleCompleted.forEachIndexed { index, step ->
                        val state =
                            remember(step) {
                                MutableTransitionState(false).apply {
                                    targetState = true
                                }
                            }
                        AnimatedVisibility(
                            visibleState = state,
                            enter = fadeIn() + slideInVertically { it / 2 },
                        ) {
                            StepRow(
                                step = step,
                                showRawValue = showRawValues,
                                paddingValues = PaddingValues(vertical = 12.dp),
                            )
                        }
                        if (index < visibleCompleted.lastIndex || (running && completed < totalSteps)) {
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    if (running && completed < totalSteps) {
                        val activeLabel =
                            SCAN_STEP_LABELS.getOrNull(completed)
                                ?: "Step ${completed + 1}"
                        ActiveStepRow(
                            label = activeLabel,
                            paddingValues = PaddingValues(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header(
    done: Boolean,
    completed: Int,
    totalSteps: Int,
    durationMs: Long,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (done) scheme.secondaryContainer else scheme.primaryContainer,
            contentColor = if (done) scheme.onSecondaryContainer else scheme.onPrimaryContainer,
        ) {
            Row(modifier = Modifier.padding(10.dp)) {
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    LoadingIndicator(modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text =
                    if (done) {
                        stringResource(R.string.scan_complete)
                    } else {
                        stringResource(R.string.scan_verifying)
                    },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    if (done) {
                        "%.1fs".format(durationMs / 1000f)
                    } else {
                        stringResource(R.string.scan_progress, completed, totalSteps)
                    },
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontFamily = if (done) FontFamily.Monospace else FontFamily.Default,
                    ),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepRow(
    step: ScanStep,
    showRawValue: Boolean,
    paddingValues: PaddingValues,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddingValues),
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
private fun ActiveStepRow(
    label: String,
    paddingValues: PaddingValues,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddingValues),
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
            shape = RoundedCornerShape(999.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(14.dp))
            }
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
            shape = RoundedCornerShape(999.dp),
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
