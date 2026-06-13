package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoftBlobBadge(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = containerColor,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
