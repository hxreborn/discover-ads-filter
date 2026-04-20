package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@Composable
fun AboutFooter(
    version: String,
    buildTimestamp: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "v$version · $buildTimestamp",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.lg),
    )
}
