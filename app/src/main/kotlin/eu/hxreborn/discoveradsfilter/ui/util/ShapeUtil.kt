package eu.hxreborn.discoveradsfilter.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

private val Outer = ShapeDefaults.ExtraLarge
private val Inner = ShapeDefaults.Small

fun shapeForPosition(
    count: Int,
    index: Int,
): CornerBasedShape =
    when {
        count == 1 -> Outer
        index == 0 -> Outer.copy(bottomEnd = Inner.bottomEnd, bottomStart = Inner.bottomStart)
        index == count - 1 -> Outer.copy(topStart = Inner.topStart, topEnd = Inner.topEnd)
        else -> Inner
    }

internal fun Modifier.preferenceCard(
    shape: Shape,
    surface: Color,
): Modifier = this.padding(horizontal = Spacing.md).background(color = surface, shape = shape).clip(shape)
