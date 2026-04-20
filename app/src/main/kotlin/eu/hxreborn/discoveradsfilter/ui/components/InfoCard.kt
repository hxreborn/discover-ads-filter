package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.discovery.KnownGoodEntry
import eu.hxreborn.discoveradsfilter.ui.state.KnownGoodUiState
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@Composable
internal fun KnownGoodDialog(
    state: KnownGoodUiState,
    onDismiss: () -> Unit,
    onForget: (KnownGoodEntry) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info_known_good_title)) },
        text = {
            if (state.total == 0) {
                Text(stringResource(R.string.info_known_good_empty))
            } else {
                LazyColumn {
                    items(state.bundled) { entry -> KnownGoodDialogRow(entry, onForget = null) }
                    items(state.local) { entry -> KnownGoodDialogRow(entry, onForget = { onForget(entry) }) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
private fun KnownGoodDialogRow(
    entry: KnownGoodEntry,
    onForget: (() -> Unit)?,
) {
    val agsaLabel = entry.agsaVersionName ?: entry.agsaVersionCode.toString()
    val moduleLabel = entry.moduleVersionName ?: entry.moduleVersionCode.toString()
    val line =
        if (entry.source == KnownGoodEntry.Source.Local) {
            stringResource(R.string.info_known_good_entry_local, agsaLabel, moduleLabel)
        } else {
            stringResource(R.string.info_known_good_entry_bundled, agsaLabel)
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = line, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "#${entry.targetsHash}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onForget != null) {
            IconButton(onClick = onForget) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.info_known_good_forget),
                )
            }
        }
    }
}
