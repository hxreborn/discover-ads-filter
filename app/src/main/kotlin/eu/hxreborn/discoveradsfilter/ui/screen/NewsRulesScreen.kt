@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailTopBar
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.preferenceCard
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsRulesScreen(
    rules: List<NewsRule>,
    onSave: (NewsRule) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.nav_news_rules),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
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
                item(key = "empty") { NewsRulesEmptyState(Modifier.fillParentMaxWidth()) }
            } else {
                itemsIndexed(rules, key = { _, rule -> rule.id }) { index, rule ->
                    NewsRuleRow(
                        rule = rule,
                        shape = shapeForPosition(rules.size, index),
                        surface = surface,
                        onToggle = { onSave(rule.copy(enabled = it)) },
                        onEdit = { editingId = rule.id },
                        onDelete = { pendingDelete = rule.id },
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.news_rule_edit)
    ListItem(
        modifier =
            modifier
                .preferenceCard(shape = shape, surface = surface)
                .clickable(onClickLabel = editLabel, onClick = onEdit),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        supportingContent = {
            Text(
                stringResource(
                    R.string.news_rule_summary,
                    stringResource(actionLabel(rule.action)),
                    stringResource(scopeLabel(rule.scope)),
                    stringResource(matchLabel(rule.match)),
                ),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = rule.pattern },
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.news_rule_delete),
                    )
                }
            }
        },
    ) {
        Text(
            text = rule.pattern,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = if (rule.match == RuleMatch.Regex) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
private fun NewsRuleDialog(
    initial: NewsRule,
    isNew: Boolean,
    onConfirm: (NewsRule) -> Unit,
    onDismiss: () -> Unit,
) {
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
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(initial.copy(pattern = pattern.trim(), action = action, scope = scope, match = match))
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun DeleteRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.news_rule_delete_dialog_title)) },
        text = { Text(stringResource(R.string.news_rule_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.news_rule_delete_dialog_confirm))
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
private fun NewsRulesEmptyState(modifier: Modifier = Modifier) {
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
            onBack = {},
        )
    }
}
