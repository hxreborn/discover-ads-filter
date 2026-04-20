package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing

@Composable
fun PreferencesCard(
    filterEnabled: Boolean,
    verbose: Boolean,
    onFilterEnabledChange: (Boolean) -> Unit,
    onVerboseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

        ListItem(
            colors = itemColors,
            leadingContent = { Icon(imageVector = Icons.Outlined.FilterAlt, contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.pref_filter_sponsored)) },
            supportingContent = { Text(stringResource(R.string.pref_filter_sponsored_summary)) },
            trailingContent = {
                Switch(checked = filterEnabled, onCheckedChange = onFilterEnabledChange)
            },
        )
        ListItem(
            colors = itemColors,
            leadingContent = { Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.toggle_verbose)) },
            supportingContent = { Text(stringResource(R.string.toggle_verbose_summary)) },
            trailingContent = {
                Switch(checked = verbose, onCheckedChange = onVerboseChange)
            },
        )
    }
}
