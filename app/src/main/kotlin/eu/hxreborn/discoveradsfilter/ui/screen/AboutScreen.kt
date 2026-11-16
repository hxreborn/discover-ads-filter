package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.AdaptiveButtonRow
import eu.hxreborn.discoveradsfilter.ui.components.ContributorRow
import eu.hxreborn.discoveradsfilter.ui.components.DetailScaffold
import eu.hxreborn.discoveradsfilter.ui.components.MorphingLogo
import eu.hxreborn.discoveradsfilter.ui.components.NavigationRow
import eu.hxreborn.discoveradsfilter.ui.components.RowGroup
import eu.hxreborn.discoveradsfilter.ui.components.RowTrailing
import eu.hxreborn.discoveradsfilter.ui.components.SectionHeader
import eu.hxreborn.discoveradsfilter.ui.components.centeredContentWidth
import eu.hxreborn.discoveradsfilter.ui.components.rememberLogoPulse
import eu.hxreborn.discoveradsfilter.ui.state.AboutInfo
import eu.hxreborn.discoveradsfilter.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.preferenceTheme

private const val REPO = "hxreborn/discover-ads-filter"
private const val REPO_URL = "https://github.com/$REPO"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val ISSUES_URL = "$REPO_URL/issues/new/choose"
private const val LICENSE_URL = "$REPO_URL/blob/main/LICENSE"
private const val KOFI_URL = "https://ko-fi.com/hxreborn"
private const val LICENSE = "GPL-3.0"
private const val ADAPTIVE_ICON_ZOOM = 1.425f

private val LogoSize = 96.dp

private fun UriHandler.openOrIgnore(url: String) {
    try {
        openUri(url)
    } catch (_: ActivityNotFoundException) {
    } catch (_: IllegalArgumentException) {
    }
}

@Composable
fun AboutScreen(
    about: AboutInfo?,
    onBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { uriHandler.openOrIgnore(it) }
    ProvidePreferenceTheme(theme = preferenceTheme()) {
        DetailScaffold(
            title = stringResource(R.string.pref_category_about),
            onBack = onBack,
        ) { contentPadding ->
            Column(
                modifier =
                    Modifier
                        .centeredContentWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
            ) {
                if (about == null) return@Column
                IdentityCard(
                    about = about,
                    onOpenUrl = open,
                    modifier =
                        Modifier.padding(
                            horizontal = Tokens.ScreenMargin,
                            vertical = Tokens.ScreenMargin,
                        ),
                )
                LinkRows(about = about, onOpenUrl = open, onOpenLicenses = onNavigateToLicenses)
                if (about.contributors.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.about_contributors))
                    RowGroup(
                        rows =
                            about.contributors.mapIndexed { index, contributor ->
                                { shape ->
                                    ContributorRow(
                                        contributor = contributor,
                                        index = index,
                                        shape = shape,
                                        onClick = { open(contributor.url) },
                                    )
                                }
                            },
                        modifier = Modifier.padding(bottom = Tokens.ScreenMargin),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkRows(
    about: AboutInfo,
    onOpenUrl: (String) -> Unit,
    onOpenLicenses: () -> Unit,
) {
    RowGroup(
        rows =
            listOf(
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.NewReleases,
                        title = stringResource(R.string.about_whats_new),
                        summary =
                            stringResource(
                                R.string.about_whats_new_summary,
                                about.versionName,
                            ),
                        onClick = { onOpenUrl("$RELEASES_URL/tag/v${about.versionName}") },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.about_changelog),
                        summary = stringResource(R.string.about_changelog_summary),
                        onClick = { onOpenUrl(RELEASES_URL) },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = ImageVector.vectorResource(R.drawable.ic_github_24),
                        title = stringResource(R.string.about_source_code),
                        summary = REPO,
                        onClick = { onOpenUrl(REPO_URL) },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.about_report_issue),
                        summary = stringResource(R.string.about_report_issue_summary),
                        onClick = { onOpenUrl(ISSUES_URL) },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Gavel,
                        title = stringResource(R.string.about_licenses),
                        summary = stringResource(R.string.about_licenses_summary),
                        onClick = onOpenLicenses,
                    )
                },
            ),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IdentityCard(
    about: AboutInfo,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    Surface(
        shape = RoundedCornerShape(Tokens.HeaderCardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = about.icon
            if (icon != null) {
                val pulse = rememberLogoPulse()
                MorphingLogo(
                    pulse = pulse,
                    modifier =
                        Modifier
                            .size(LogoSize)
                            .pointerInput(pulse) { detectTapGestures { pulse.tap() } },
                ) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier =
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = ADAPTIVE_ICON_ZOOM
                                scaleY = ADAPTIVE_ICON_ZOOM
                            },
                    )
                }
            }
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onOpenUrl(RELEASES_URL) },
                    label = { Text("v${about.versionName} (${about.versionCode})", style = meta) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Sell, null, Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
                AssistChip(
                    onClick = { onOpenUrl(LICENSE_URL) },
                    label = { Text(LICENSE, style = meta) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Balance,
                            null,
                            Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
            SupportButtons(onOpenUrl = onOpenUrl, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SupportButtons(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        listOf(
            stringResource(R.string.about_star_on_github),
            stringResource(R.string.about_buy_coffee),
        )
    AdaptiveButtonRow(items = labels, modifier = modifier) { index, label, stacked, itemModifier ->
        val buttonModifier = itemModifier.heightIn(min = 52.dp)
        if (index == 0) {
            Button(
                onClick = { onOpenUrl(REPO_URL) },
                shapes =
                    if (stacked) {
                        ButtonDefaults.shapes()
                    } else {
                        ButtonDefaults.shapes(
                            ButtonGroupDefaults.connectedLeadingButtonShape,
                            ButtonGroupDefaults.connectedLeadingButtonPressShape,
                        )
                    },
                modifier = buttonModifier,
            ) {
                Icon(Icons.Outlined.StarOutline, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        } else {
            FilledTonalButton(
                onClick = { onOpenUrl(KOFI_URL) },
                shapes =
                    if (stacked) {
                        ButtonDefaults.shapes()
                    } else {
                        ButtonDefaults.shapes(
                            ButtonGroupDefaults.connectedTrailingButtonShape,
                            ButtonGroupDefaults.connectedTrailingButtonPressShape,
                        )
                    },
                modifier = buttonModifier,
            ) {
                Icon(Icons.Outlined.Coffee, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        }
    }
}
