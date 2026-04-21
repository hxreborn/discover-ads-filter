package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import eu.hxreborn.discoveradsfilter.util.rememberAgsaIconPainter

private const val DIAG_COUNT = 3

fun LazyListScope.diagnosticsItems(
    state: VerifyUiState,
    surface: Color,
    onInspectFingerprints: () -> Unit,
) {
    item(key = "diag_target", contentType = "DiagItem") {
        val shape = shapeForPosition(DIAG_COUNT, 0)
        val agsaPainter = rememberAgsaIconPainter()
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(horizontal = 8.dp).background(color = surface, shape = shape).clip(shape),
            leadingContent = {
                Image(
                    painter = agsaPainter,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.md),
                )
            },
            headlineContent = { Text(stringResource(R.string.diag_target_app)) },
            supportingContent = { Text(targetSupporting(state)) },
        )
    }

    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    item(key = "diag_mapping", contentType = "DiagItem") {
        val shape = shapeForPosition(DIAG_COUNT, 1)
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(horizontal = 8.dp).background(color = surface, shape = shape).clip(shape),
            leadingContent = {
                Icon(imageVector = Icons.Outlined.Map, contentDescription = null)
            },
            headlineContent = { Text(stringResource(R.string.diag_dexkit_mapping)) },
            supportingContent = { Text(mappingSupporting(state)) },
        )
    }

    item(contentType = "Spacer") { Spacer(Modifier.height(2.dp)) }

    item(key = "diag_fingerprints", contentType = "DiagItem") {
        val shape = shapeForPosition(DIAG_COUNT, 2)
        val resolved = state.resolvedTargetCount
        val total = state.totalTargetCount
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .background(color = surface, shape = shape)
                    .clip(shape)
                    .clickable(onClick = onInspectFingerprints),
            leadingContent = {
                Icon(imageVector = Icons.Outlined.Fingerprint, contentDescription = null)
            },
            headlineContent = { Text(stringResource(R.string.pref_dexkit_title)) },
            supportingContent = {
                Text(
                    if (state.lastResult != null) {
                        stringResource(R.string.pref_dexkit_summary_ready, resolved, total)
                    } else {
                        stringResource(R.string.pref_dexkit_summary_none)
                    },
                )
            },
        )
    }
}

@Composable
private fun targetSupporting(state: VerifyUiState): String {
    val name = state.installedAgsaVersionName
    val code = state.installedAgsaVersion
    return when {
        name != null -> name
        code != null -> "v$code"
        else -> stringResource(R.string.hero_target_missing)
    }
}

@Composable
private fun mappingSupporting(state: VerifyUiState): String {
    val result = state.lastResult
    if (result !is VerifyResult.Success) return stringResource(R.string.diag_mapping_pending)
    val resolved = state.resolvedTargetCount
    val total = state.totalTargetCount
    return stringResource(R.string.diag_mapping_ratio, result.versionCode, resolved, total)
}
