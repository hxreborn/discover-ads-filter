@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.filter.NewsRule
import eu.hxreborn.discoveradsfilter.filter.RuleAction
import eu.hxreborn.discoveradsfilter.filter.RuleMatch
import eu.hxreborn.discoveradsfilter.filter.RuleScope
import eu.hxreborn.discoveradsfilter.ui.components.IconSwitch
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailTopBar
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.IconSize
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.preferenceCard
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import java.util.UUID

private const val EXPORT_MIME = "application/json"
private const val EXPORT_FILE_NAME = "discover-news-filters.json"

private fun launchPicker(
    context: Context,
    launch: () -> Unit,
) {
    runCatching(launch).onFailure {
        Toast.makeText(context, R.string.news_rules_no_picker, Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewsRulesScreen(
    rules: List<NewsRule>,
    onSave: (NewsRule) -> Unit,
    onDelete: (String) -> Unit,
    onLoadPresets: () -> Unit,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit,
    onSetAllEnabled: (Boolean) -> Unit,
    onDeleteAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPresets by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onImport)
        }
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(EXPORT_MIME),
        ) { uri -> uri?.let(onExport) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    editingId?.let { id ->
        val existing = rules.find { it.id == id }
        NewsRuleDialog(
            initial = existing ?: NewsRule(id = id, pattern = ""),
            isNew = existing == null,
            onConfirm = {
                onSave(it)
                editingId = null
            },
            onDismiss = { editingId = null },
            onDelete =
                if (existing != null) {
                    {
                        editingId = null
                        pendingDelete = id
                    }
                } else {
                    null
                },
        )
    }

    pendingDelete?.let { id ->
        DeleteRuleDialog(
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                onDelete(id)
            },
        )
    }

    if (confirmPresets) {
        ConfirmDialog(
            title = stringResource(R.string.news_rules_presets_dialog_title),
            confirmLabel = stringResource(R.string.news_rules_presets_dialog_confirm),
            onDismiss = { confirmPresets = false },
            onConfirm = {
                confirmPresets = false
                onLoadPresets()
            },
        )
    }

    if (confirmDeleteAll) {
        ConfirmDialog(
            title = stringResource(R.string.news_rules_delete_all_dialog_title),
            body = stringResource(R.string.news_rules_delete_all_dialog_body),
            confirmLabel = stringResource(R.string.news_rule_delete_dialog_confirm),
            onDismiss = { confirmDeleteAll = false },
            onConfirm = {
                confirmDeleteAll = false
                onDeleteAll()
            },
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.nav_news_rules),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.news_rules_more),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.news_rules_load_presets)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirmPresets = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.news_rules_import)) },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                launchPicker(context) { importLauncher.launch(arrayOf("*/*")) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.news_rules_export)) },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                launchPicker(context) { exportLauncher.launch(EXPORT_FILE_NAME) }
                            },
                        )
                        if (rules.isNotEmpty()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.news_rules_enable_all)) },
                                leadingIcon = { Icon(Icons.Outlined.ToggleOn, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onSetAllEnabled(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.news_rules_disable_all)) },
                                leadingIcon = { Icon(Icons.Outlined.ToggleOff, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onSetAllEnabled(false)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.news_rules_delete_all)) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDeleteAll = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingId = UUID.randomUUID().toString() },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.news_rule_add)) },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { innerPadding ->
        val surface = MaterialTheme.colorScheme.surfaceVariant
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(top = Spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (rules.isEmpty()) {
                item(key = "empty") {
                    NewsRulesEmptyState(
                        onLoadPresets = onLoadPresets,
                        modifier = Modifier.fillParentMaxWidth(),
                    )
                }
            } else {
                itemsIndexed(rules, key = { _, rule -> rule.id }) { index, rule ->
                    NewsRuleRow(
                        rule = rule,
                        shape = shapeForPosition(rules.size, index),
                        surface = surface,
                        onToggle = { onSave(rule.copy(enabled = it)) },
                        onEdit = { editingId = rule.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsRuleRow(
    rule: NewsRule,
    shape: Shape,
    surface: Color,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.news_rule_edit)
    Row(
        modifier =
            modifier
                .preferenceCard(shape = shape, surface = surface)
                .clickable(onClickLabel = editLabel, onClick = onEdit)
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = rule.label ?: rule.pattern,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily =
                    if (rule.label == null && rule.match == RuleMatch.Regex) {
                        FontFamily.Monospace
                    } else {
                        FontFamily.Default
                    },
            )
            if (rule.label != null) {
                Text(
                    text = rule.pattern,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily =
                        if (rule.match == RuleMatch.Regex) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(top = Spacing.xs),
            ) {
                if (rule.action == RuleAction.Allow) {
                    RuleChip(
                        icon = actionIcon(rule.action),
                        text = stringResource(actionLabel(rule.action)),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    RuleChip(
                        icon = actionIcon(rule.action),
                        text = stringResource(actionLabel(rule.action)),
                    )
                }
                RuleChip(icon = scopeIcon(rule.scope), text = stringResource(scopeLabel(rule.scope)))
                RuleChip(icon = matchIcon(rule.match), text = stringResource(matchLabel(rule.match)))
            }
        }
        IconSwitch(
            checked = rule.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics { contentDescription = rule.pattern },
        )
    }
}

@Composable
private fun RuleChip(
    icon: ImageVector,
    text: String,
    container: Color = MaterialTheme.colorScheme.surface,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier =
            Modifier
                .background(color = container, shape = MaterialTheme.shapes.small)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(IconSize.xs),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun NewsRuleDialog(
    initial: NewsRule,
    isNew: Boolean,
    onConfirm: (NewsRule) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial.label.orEmpty()) }
    var pattern by rememberSaveable { mutableStateOf(initial.pattern) }
    var action by rememberSaveable { mutableStateOf(initial.action) }
    var scope by rememberSaveable { mutableStateOf(initial.scope) }
    var match by rememberSaveable { mutableStateOf(initial.match) }

    val blank by remember { derivedStateOf { pattern.isBlank() } }
    val badRegex by
        remember {
            derivedStateOf {
                match == RuleMatch.Regex && !blank && runCatching { Regex(pattern) }.isFailure
            }
        }
    val valid = !blank && !badRegex

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.news_rule_add else R.string.news_rule_edit)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.news_rule_name_label)) },
                    placeholder = { Text(stringResource(R.string.news_rule_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(R.string.news_rule_pattern_label)) },
                    placeholder = { Text(stringResource(R.string.news_rule_pattern_hint)) },
                    isError = badRegex,
                    supportingText = if (badRegex) ({ Text(stringResource(R.string.news_rule_invalid)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = if (match == RuleMatch.Regex) FontFamily.Monospace else FontFamily.Default,
                        ),
                )
                Spacer(Modifier.height(Spacing.md))
                Choice(
                    label = stringResource(R.string.news_rule_scope),
                    options = RuleScope.entries,
                    selected = scope,
                    labelFor = { stringResource(scopeLabel(it)) },
                    onSelect = { scope = it },
                )
                Spacer(Modifier.height(Spacing.sm))
                Choice(
                    label = stringResource(R.string.news_rule_match),
                    options = RuleMatch.entries,
                    selected = match,
                    labelFor = { stringResource(matchLabel(it)) },
                    onSelect = { match = it },
                )
                Spacer(Modifier.height(Spacing.sm))
                Choice(
                    label = stringResource(R.string.news_rule_action),
                    options = RuleAction.entries,
                    selected = action,
                    labelFor = { stringResource(actionLabel(it)) },
                    onSelect = { action = it },
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.news_rule_delete_dialog_confirm))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    enabled = valid,
                    onClick = {
                        onConfirm(
                            initial.copy(
                                pattern = pattern.trim(),
                                action = action,
                                scope = scope,
                                match = match,
                                label = name.trim().takeIf { it.isNotEmpty() },
                            ),
                        )
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        },
    )
}

@Composable
private fun DeleteRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.news_rule_delete_dialog_title),
        body = stringResource(R.string.news_rule_delete_dialog_body),
        confirmLabel = stringResource(R.string.news_rule_delete_dialog_confirm),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    body: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = body?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun <T> Choice(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xs))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(labelFor(option), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NewsRulesEmptyState(
    onLoadPresets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterAltOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.news_rules_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.news_rules_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        FilledTonalButton(onClick = onLoadPresets) {
            Text(stringResource(R.string.news_rules_load_presets))
        }
    }
}

private fun scopeLabel(scope: RuleScope): Int =
    when (scope) {
        RuleScope.Headline -> R.string.news_rule_scope_headline
        RuleScope.Source -> R.string.news_rule_scope_source
        RuleScope.Any -> R.string.news_rule_scope_any
    }

private fun matchLabel(match: RuleMatch): Int =
    when (match) {
        RuleMatch.Contains -> R.string.news_rule_match_contains
        RuleMatch.Regex -> R.string.news_rule_match_regex
    }

private fun actionLabel(action: RuleAction): Int =
    when (action) {
        RuleAction.Block -> R.string.news_rule_action_block
        RuleAction.Allow -> R.string.news_rule_action_allow
    }

private fun scopeIcon(scope: RuleScope): ImageVector =
    when (scope) {
        RuleScope.Headline -> Icons.Outlined.Title
        RuleScope.Source -> Icons.Outlined.Language
        RuleScope.Any -> Icons.Outlined.AllInclusive
    }

private fun matchIcon(match: RuleMatch): ImageVector =
    when (match) {
        RuleMatch.Contains -> Icons.Outlined.Abc
        RuleMatch.Regex -> Icons.Outlined.Code
    }

private fun actionIcon(action: RuleAction): ImageVector =
    when (action) {
        RuleAction.Block -> Icons.Outlined.VisibilityOff
        RuleAction.Allow -> Icons.Outlined.Visibility
    }

@Preview(name = "News filters", showSystemUi = true)
@Composable
private fun NewsRulesScreenPreview() {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        NewsRulesScreen(
            rules =
                listOf(
                    NewsRule(id = "1", pattern = "confirmado por la UE"),
                    NewsRule(id = "2", pattern = "infobae.com", scope = RuleScope.Source),
                ),
            onSave = {},
            onDelete = {},
            onLoadPresets = {},
            onImport = {},
            onExport = {},
            onSetAllEnabled = {},
            onDeleteAll = {},
            onBack = {},
        )
    }
}
