package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.ui.state.DIFF_BLOCK_COUNT
import eu.hxreborn.discoveradsfilter.ui.state.diffBlocks
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.theme.extendedColors
import java.text.NumberFormat

private const val MINUS = "−"

private val BlockSize = 9.dp
private val BlockGap = 2.dp
private val BlockCorner = 1.dp
private val HatchSpacing = 3.dp
private val HatchStroke = 1.dp
private const val HATCH_ALPHA = 0.5f

@Composable
fun DiffStat(
    added: Int,
    removed: Int,
    modifier: Modifier = Modifier,
) {
    val addedText = remember(added) { "+" + NumberFormat.getIntegerInstance().format(added) }
    val removedText = remember(removed) { MINUS + NumberFormat.getIntegerInstance().format(removed) }
    val numberStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = addedText, style = numberStyle, color = MaterialTheme.extendedColors.diffAdded)
        Spacer(Modifier.width(Spacing.sm))
        Text(text = removedText, style = numberStyle, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(Spacing.sm))
        DiffBlocksBar(added = added, removed = removed)
    }
}

@Composable
private fun DiffBlocksBar(
    added: Int,
    removed: Int,
) {
    val blocks = remember(added, removed) { diffBlocks(added, removed) }
    val addedColor = MaterialTheme.extendedColors.diffAdded
    val removedColor = MaterialTheme.colorScheme.error
    val hatchColor = MaterialTheme.colorScheme.onError.copy(alpha = HATCH_ALPHA)
    val neutralColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Spacer(
        Modifier
            .size(width = BlockSize * DIFF_BLOCK_COUNT + BlockGap * (DIFF_BLOCK_COUNT - 1), height = BlockSize)
            .clearAndSetSemantics {}
            .drawWithCache {
                val block = BlockSize.toPx()
                val step = block + BlockGap.toPx()
                val corner = CornerRadius(BlockCorner.toPx())
                val blockSize = Size(block, block)
                val hatchStroke = Stroke(width = HatchStroke.toPx())
                val hatch = hatchPath(block, HatchSpacing.toPx())
                onDrawBehind {
                    for (index in 0 until DIFF_BLOCK_COUNT) {
                        val isAdded = index < blocks.added
                        val isRemoved = !isAdded && index < blocks.added + blocks.removed
                        val color =
                            when {
                                isAdded -> addedColor
                                isRemoved -> removedColor
                                else -> neutralColor
                            }
                        drawRoundRect(color, Offset(index * step, 0f), blockSize, corner)
                        if (isRemoved) {
                            translate(left = index * step) {
                                drawPath(hatch, hatchColor, style = hatchStroke)
                            }
                        }
                    }
                }
            },
    )
}

private fun hatchPath(
    block: Float,
    spacing: Float,
): Path =
    Path().apply {
        var offset = spacing
        while (offset < block * 2) {
            moveTo((offset - block).coerceAtLeast(0f), offset.coerceAtMost(block))
            lineTo(offset.coerceAtMost(block), (offset - block).coerceAtLeast(0f))
            offset += spacing
        }
    }
