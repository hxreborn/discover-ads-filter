package eu.hxreborn.discoveradsfilter.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule

@Composable
fun rememberAgsaIconPainter(): Painter {
    val context = LocalContext.current
    val fallback = rememberVectorPainter(Icons.Outlined.Public)
    return remember(context) {
        runCatching {
            val drawable =
                context.packageManager.getApplicationIcon(DiscoverAdsFilterModule.AGSA_PKG)
            val bitmap = drawable.toBitmap(width = 96, height = 96)
            BitmapPainter(bitmap.asImageBitmap())
        }.getOrNull()
    } ?: fallback
}
