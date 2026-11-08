/*
 * Copyright (C) 2024 The Android Open Source Project
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: Apache-2.0
 *
 * Adapted from material-components/material-components-android:
 * https://github.com/material-components/material-components-android/commit/66b0eeb53afeb6025ffbbd37734b08e5bce197ed
 * lib/java/com/google/android/material/loadingindicator/LoadingIndicatorAnimatorDelegate.java and
 * lib/java/com/google/android/material/loadingindicator/LoadingIndicatorDrawingDelegate.java
 */
package eu.hxreborn.discoveradsfilter.hook

import android.R.color
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val NANOS_PER_SHAPE = 650_000_000L
private const val CONSTANT_ROTATION_PER_SHAPE_DEGREES = 50f
private const val EXTRA_ROTATION_PER_SHAPE_DEGREES = 90f
private const val ROTATION_PER_SHAPE_DEGREES =
    CONSTANT_ROTATION_PER_SHAPE_DEGREES + EXTRA_ROTATION_PER_SHAPE_DEGREES
private const val SPRING_STIFFNESS = 200.0
private const val SPRING_DAMPING_RATIO = 0.6
private const val SPRING_VALUE_THRESHOLD = 0.01 * 0.75
private const val SPRING_VELOCITY_THRESHOLD = SPRING_VALUE_THRESHOLD * 62.5
private const val NANOS_PER_SECOND = 1e9
private const val INDICATOR_SIZE_RATIO = 38f / 48f
private const val DETERMINATE_DEGREES_PER_PROGRESS = -180f
private const val START_ANGLE_DEGREES = -90f
private const val FULL_TURN_DEGREES = 360f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class LoadingIndicatorDrawing(
    private val context: Context,
) {
    var progress: Float = 0f
    private val pathBounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var night: Boolean? = null
    private val path = Path()
    private var startNanos = 0L
    private var morphTarget = 1
    private var morphFactor = 0f
    private var morphVelocity = 0.0
    private var springFrameNanos = 0L
    private var rotationDegrees = 0f

    fun draw(
        stock: Drawable,
        canvas: Canvas,
    ) {
        val running = (stock as Animatable).isRunning && ValueAnimator.areAnimatorsEnabled()
        val morph: Morph
        val fraction: Float
        val rotation: Float
        if (running) {
            advance(System.nanoTime())
            val shapeIndex = floor(morphFactor).toInt()
            morph = indeterminateMorphs[shapeIndex.mod(indeterminateMorphs.size)]
            fraction = morphFactor - shapeIndex
            rotation = rotationDegrees
        } else {
            startNanos = 0L
            morph = determinateMorph
            fraction = progress.coerceIn(0f, 1f)
            rotation = fraction * DETERMINATE_DEGREES_PER_PROGRESS
        }
        val area = stock.bounds
        val radius = min(area.width(), area.height()) / 2f
        resolveColors()
        paint.alpha = stock.alpha
        containerPaint.alpha = stock.alpha
        canvas.drawCircle(area.exactCenterX(), area.exactCenterY(), radius, containerPaint)
        morph.toPath(fraction, path)
        path.computeBounds(pathBounds, true)
        val scale = radius * 2f * INDICATOR_SIZE_RATIO * shapeScale
        canvas.save()
        canvas.translate(area.exactCenterX(), area.exactCenterY())
        canvas.rotate(START_ANGLE_DEGREES + rotation)
        canvas.scale(scale, scale)
        canvas.translate(-pathBounds.centerX(), -pathBounds.centerY())
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun resolveColors() {
        val nightNow = context.resources.configuration.isNightModeActive
        if (nightNow == night) return
        night = nightNow
        if (Build.VERSION.SDK_INT >= 34) {
            containerPaint.color =
                context.getColor(
                    if (nightNow) {
                        color.system_primary_container_dark
                    } else {
                        color.system_primary_container_light
                    },
                )
            paint.color =
                context.getColor(
                    if (nightNow) {
                        color.system_on_primary_container_dark
                    } else {
                        color.system_on_primary_container_light
                    },
                )
        } else if (Build.VERSION.SDK_INT >= 31) {
            containerPaint.color =
                context.getColor(
                    if (nightNow) color.system_accent1_700 else color.system_accent1_100,
                )
            paint.color =
                context.getColor(
                    if (nightNow) color.system_accent1_100 else color.system_accent1_900,
                )
        } else {
            containerPaint.color = themeColor(android.R.attr.colorBackground)
            paint.color = themeColor(android.R.attr.colorAccent)
        }
    }

    private fun themeColor(attribute: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attribute, value, true)
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
    }

    private fun advance(now: Long) {
        if (startNanos == 0L) {
            startNanos = now
            morphFactor = 0f
            morphVelocity = 0.0
            springFrameNanos = 0L
        }
        val elapsed = now - startNanos
        morphTarget = 1 + (elapsed / NANOS_PER_SHAPE).toInt()
        val timeFraction = (elapsed % NANOS_PER_SHAPE).toFloat() / NANOS_PER_SHAPE
        stepSpring(now)
        val shapeBase = morphTarget - 1
        rotationDegrees =
            (
                ROTATION_PER_SHAPE_DEGREES * shapeBase +
                    CONSTANT_ROTATION_PER_SHAPE_DEGREES * timeFraction +
                    EXTRA_ROTATION_PER_SHAPE_DEGREES * (morphFactor - shapeBase)
            ) % FULL_TURN_DEGREES
    }

    private fun stepSpring(frameNanos: Long) {
        val elapsedSeconds =
            if (springFrameNanos == 0L) 0.0 else (frameNanos - springFrameNanos) / NANOS_PER_SECOND
        springFrameNanos = frameNanos
        val naturalFrequency = sqrt(SPRING_STIFFNESS)
        val dampedFrequency =
            naturalFrequency * sqrt(1 - SPRING_DAMPING_RATIO * SPRING_DAMPING_RATIO)
        val decayRate = SPRING_DAMPING_RATIO * naturalFrequency
        val displacement = (morphFactor - morphTarget).toDouble()
        val sineCoefficient = (morphVelocity + decayRate * displacement) / dampedFrequency
        val decay = exp(-decayRate * elapsedSeconds)
        val cosine = cos(dampedFrequency * elapsedSeconds)
        val sine = sin(dampedFrequency * elapsedSeconds)
        var nextDisplacement = decay * (displacement * cosine + sineCoefficient * sine)
        var nextVelocity =
            -decayRate * nextDisplacement +
                decay * dampedFrequency * (sineCoefficient * cosine - displacement * sine)
        if (abs(nextDisplacement) < SPRING_VALUE_THRESHOLD &&
            abs(nextVelocity) < SPRING_VELOCITY_THRESHOLD
        ) {
            nextDisplacement = 0.0
            nextVelocity = 0.0
        }
        morphFactor = (morphTarget + nextDisplacement).toFloat()
        morphVelocity = nextVelocity
    }

    companion object {
        private val polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
        private val indeterminateMorphs =
            polygons.indices.map { index ->
                Morph(
                    polygons[index].normalized(),
                    polygons[(index + 1) % polygons.size].normalized(),
                )
            }
        private val determinateMorph =
            LoadingIndicatorDefaults.DeterminateIndicatorPolygons.let {
                Morph(it.first().normalized(), it.last().normalized())
            }
        private val shapeScale =
            polygons.minOf { polygon ->
                val bounds = polygon.calculateBounds()
                val maxBounds = polygon.calculateMaxBounds()
                max(
                    (bounds[2] - bounds[0]) / (maxBounds[2] - maxBounds[0]),
                    (bounds[3] - bounds[1]) / (maxBounds[3] - maxBounds[1]),
                )
            }
    }
}
