package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import eu.hxreborn.discoveradsfilter.App
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailScaffold
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val GITHUB_URL = "https://github.com/hxreborn/discover-ads-filter"
private const val GITHUB_ISSUES_URL = "https://github.com/hxreborn/discover-ads-filter/issues"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
) {
    val context = LocalContext.current

    SettingsDetailScaffold(title = stringResource(R.string.pref_category_about), onBack = onBack) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapeForPosition(1, 0),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_about_discover),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) – ${BuildConfig.BUILD_TYPE} build",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text =
                        remember {
                            val ts = BuildConfig.BUILD_TIMESTAMP
                            if (ts == 0L) {
                                "Built Apr 25, 2026"
                            } else {
                                val formatter =
                                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                val zoned =
                                    Instant
                                        .ofEpochMilli(ts)
                                        .atZone(ZoneId.systemDefault())
                                "Built ${formatter.format(zoned)}"
                            }
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                App.boundService?.let { service ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "${service.frameworkName} v${service.frameworkVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.about_links),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.xs))

        val githubIcon = painterResource(R.drawable.ic_github_24)
        AboutCard(
            icon = githubIcon,
            title = stringResource(R.string.about_source_code),
            subtitle = stringResource(R.string.about_source_code_summary),
            shape = shapeForPosition(3, 0),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())) },
        )
        Spacer(Modifier.height(2.dp))
        AboutCard(
            icon = Icons.Outlined.Gavel,
            title = stringResource(R.string.about_licenses),
            subtitle = stringResource(R.string.about_licenses_summary),
            shape = shapeForPosition(3, 1),
            onClick = onNavigateToLicenses,
        )
        Spacer(Modifier.height(2.dp))
        AboutCard(
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.about_report_issue),
            subtitle = stringResource(R.string.about_report_issue_summary),
            shape = shapeForPosition(3, 2),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_ISSUES_URL.toUri())) },
        )
    }
}

@Composable
private fun AboutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    AboutCard(
        iconContent = { Icon(imageVector = icon, contentDescription = null) },
        title = title,
        subtitle = subtitle,
        shape = shape,
        onClick = onClick,
    )
}

@Composable
private fun AboutCard(
    icon: Painter,
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    AboutCard(
        iconContent = { Icon(painter = icon, contentDescription = null) },
        title = title,
        subtitle = subtitle,
        shape = shape,
        onClick = onClick,
    )
}

@Composable
private fun AboutCard(
    iconContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconContent()
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
