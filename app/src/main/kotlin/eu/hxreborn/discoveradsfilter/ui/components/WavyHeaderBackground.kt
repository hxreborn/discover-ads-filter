package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Two phase-offset sine waves drawn behind the top app bar. Amplitude scales
 * down with the bar's collapse fraction so the motion flattens as the user
 * scrolls the content up.
 */
@Composable
fun WavyHeaderBackground(
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "wavy-header")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "wave-phase",
    )

    val amplitudeScale = (1f - collapseFraction).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.10f * amplitudeScale), surface),
                ),
        )
        drawWave(
            phase = phase,
            amplitudeScale = amplitudeScale,
            color = primary.copy(alpha = 0.22f),
            yFraction = 0.55f,
            amplitudeFraction = 0.12f,
            frequency = 1.2f,
        )
        drawWave(
            phase = phase + (PI / 2f).toFloat(),
            amplitudeScale = amplitudeScale,
            color = secondary.copy(alpha = 0.18f),
            yFraction = 0.70f,
            amplitudeFraction = 0.08f,
            frequency = 1.8f,
        )
    }
}

private fun DrawScope.drawWave(
    phase: Float,
    amplitudeScale: Float,
    color: Color,
    yFraction: Float,
    amplitudeFraction: Float,
    frequency: Float,
) {
    val w = size.width
    val h = size.height
    val baseline = h * yFraction
    val amp = h * amplitudeFraction * amplitudeScale
    val path =
        Path().apply {
            moveTo(0f, baseline)
            val step = 6f
            var x = 0f
            while (x <= w) {
                val y = baseline + sin((x / w) * frequency * 2f * PI.toFloat() + phase) * amp
                lineTo(x, y)
                x += step
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(listOf(color, Color.Transparent)),
    )
}
