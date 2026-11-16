@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.ui.theme.Tokens
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import me.zhanghai.compose.preference.Preference

enum class RowTrailing {
    Chevron,
    External,
    None,
}

@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .padding(
                    start = Tokens.RowPadding + 4.dp,
                    end = Tokens.RowPadding,
                    top = 20.dp,
                    bottom = 8.dp,
                ).semantics { heading() },
    )
}

@Composable
internal fun RowGroup(
    rows: List<@Composable (Shape) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Tokens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(Tokens.RowGap),
    ) {
        rows.forEachIndexed { index, row -> row(shapeForPosition(rows.size, index)) }
    }
}

@Composable
internal fun Modifier.rowSurface(shape: Shape): Modifier =
    clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .fillMaxWidth()
        .heightIn(min = Tokens.RowMinHeight)

@Composable
internal fun SwitchRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Preference(
        title = { Text(title) },
        modifier =
            modifier
                .rowSurface(shape)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch) { on ->
                    haptics.performHapticFeedback(
                        if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onCheckedChange(on)
                },
        enabled = enabled,
        icon = { Icon(icon, contentDescription = null) },
        summary = { Text(summary) },
        widgetContainer = {
            IconSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.padding(horizontal = Tokens.RowPadding),
            )
        },
    )
}

@Composable
internal fun NavigationRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: RowTrailing = RowTrailing.Chevron,
) {
    Preference(
        title = { Text(title) },
        modifier = modifier.rowSurface(shape),
        icon = { Icon(icon, contentDescription = null) },
        summary = summary?.let { { Text(it) } },
        widgetContainer = {
            val trailingIcon =
                when (trailing) {
                    RowTrailing.Chevron -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    RowTrailing.External -> Icons.AutoMirrored.Outlined.OpenInNew
                    RowTrailing.None -> null
                }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Tokens.RowPadding),
                )
            }
        },
        onClick = onClick,
    )
}
