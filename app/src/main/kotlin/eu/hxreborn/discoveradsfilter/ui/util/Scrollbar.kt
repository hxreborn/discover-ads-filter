// MIT License - Copyright (c) 2022 Albert Chang
// https://gist.github.com/mxalbert1996/33a360fcab2105a31e5355af98216f5a
// Migrated from Modifier.composed to Modifier.Node

package eu.hxreborn.discoveradsfilter.ui.util

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
): Modifier {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    return this then ScrollbarElement(state, reverseScrolling, color)
}

private data class ScrollbarElement(
    val state: LazyListState,
    val reverseScrolling: Boolean,
    val color: Color,
) : ModifierNodeElement<ScrollbarNode>() {
    override fun create() = ScrollbarNode(state, reverseScrolling, color)

    override fun update(node: ScrollbarNode) {
        node.state = state
        node.reverseScrolling = reverseScrolling
        node.color = color
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawVerticalScrollbar"
        properties["state"] = state
        properties["reverseScrolling"] = reverseScrolling
    }
}

private class ScrollbarNode(
    var state: LazyListState,
    var reverseScrolling: Boolean,
    var color: Color,
) : Modifier.Node(),
    DrawModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest {
                    alpha.snapTo(1f)
                    delay(ViewConfiguration.getScrollDefaultDelay().toLong())
                    alpha.animateTo(0f, animationSpec = FADE_OUT_ANIMATION_SPEC)
                }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val layoutInfo = state.layoutInfo
        val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val items = layoutInfo.visibleItemsInfo
        val itemsSize = items.fastSumBy { it.size }
        if (items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize) {
            val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
            val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
            val canvasSize = size.height
            val thumbSize = viewportSize / totalSize * canvasSize
            val startOffset =
                if (items.isEmpty()) {
                    0f
                } else {
                    items.first().run {
                        (estimatedItemSize * index - offset) / totalSize * canvasSize
                    }
                }
            val atEnd = layoutDirection == LayoutDirection.Ltr
            drawVerticalThumb(
                atEnd = atEnd,
                color = color,
                alpha = alpha.value,
                thumbHeight = thumbSize,
                startOffset = if (reverseScrolling) canvasSize - startOffset - thumbSize else startOffset,
            )
        }
    }
}

private fun DrawScope.drawVerticalThumb(
    atEnd: Boolean,
    color: Color,
    alpha: Float,
    thumbHeight: Float,
    startOffset: Float,
) {
    val thicknessPx = THICKNESS.toPx()
    val topLeft =
        Offset(
            if (atEnd) size.width - thicknessPx else 0f,
            startOffset,
        )
    drawRect(color = color, topLeft = topLeft, size = Size(thicknessPx, thumbHeight), alpha = alpha)
}

private val THICKNESS = 4.dp
private val FADE_OUT_ANIMATION_SPEC = tween<Float>(durationMillis = ViewConfiguration.getScrollBarFadeDuration())
