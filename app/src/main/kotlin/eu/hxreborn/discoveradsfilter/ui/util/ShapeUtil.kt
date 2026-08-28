package eu.hxreborn.discoveradsfilter.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

private val CornerLarge = 24.dp
private val CornerSmall = 4.dp

fun shapeForPosition(
    count: Int,
    index: Int,
): RoundedCornerShape =
    when {
        count == 1 -> {
            RoundedCornerShape(CornerLarge)
        }

        index == 0 -> {
            RoundedCornerShape(
                topStart = CornerLarge,
                topEnd = CornerLarge,
                bottomEnd = CornerSmall,
                bottomStart = CornerSmall,
            )
        }

        index == count - 1 -> {
            RoundedCornerShape(
                topStart = CornerSmall,
                topEnd = CornerSmall,
                bottomEnd = CornerLarge,
                bottomStart = CornerLarge,
            )
        }

        else -> {
            RoundedCornerShape(CornerSmall)
        }
    }

internal fun Modifier.preferenceCard(
    shape: Shape,
    surface: Color,
): Modifier = this.padding(horizontal = Spacing.sm).background(color = surface, shape = shape).clip(shape)
