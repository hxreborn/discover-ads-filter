package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState

@Composable
internal fun DexKitDialog(
    state: VerifyUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dexkit_dialog_title)) },
        text = {
            val result = state.lastResult
            if (result !is VerifyResult.Success) {
                Text(stringResource(R.string.dexkit_no_results))
            } else {
                MappingTable(targets = result.targets)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
private fun MappingTable(targets: ResolvedTargets.Resolved) {
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        MappingRow(
            symbol = "AdMetadata",
            resolved = targets.adMetadataClass?.substringAfterLast('.'),
        )
        MappingRow(
            symbol = "FeedCard",
            resolved = targets.feedCardClass?.substringAfterLast('.'),
        )
        MappingRow(
            symbol = "isAd",
            resolved = targets.adFlagFieldName,
        )
        MappingRow(
            symbol = "adLabel",
            resolved = targets.adLabelFieldName,
        )
        MappingRow(
            symbol = "adMetadata",
            resolved = targets.adMetadataFieldName,
        )

        if (targets.cardProcessorMethods.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "cardProcessors",
                style = MaterialTheme.typography.labelSmall,
                color = dimColor,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            targets.cardProcessorMethods.forEachIndexed { i, method ->
                Text(
                    text = method.shortForm(),
                    style = mono,
                    modifier = Modifier.padding(bottom = if (i < targets.cardProcessorMethods.lastIndex) 2.dp else 0.dp),
                )
            }
        }

        targets.streamRenderableListMethod?.let { method ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "streamList hook",
                style = MaterialTheme.typography.labelSmall,
                color = dimColor,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(text = method.shortForm(), style = mono)
        }
    }
}

@Composable
private fun MappingRow(
    symbol: String,
    resolved: String?,
) {
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.bodySmall,
            color = dimColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "→",
            style = MaterialTheme.typography.bodySmall,
            color = dimColor,
        )
        Text(
            text = resolved ?: "–",
            style = mono,
            color = if (resolved != null) MaterialTheme.colorScheme.primary else dimColor,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun MethodRef.shortForm(): String {
    val cls = className.substringAfterLast('.')
    val params = paramTypeNames.joinToString(", ") { it.substringAfterLast('.') }
    return "$cls.$methodName($params)"
}
