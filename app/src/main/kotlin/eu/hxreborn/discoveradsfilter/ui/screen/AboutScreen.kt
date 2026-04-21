package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
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
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition

private const val GITHUB_URL = "https://github.com/hxreborn/discover-ads-filter"
private const val GITHUB_ISSUES_URL = "https://github.com/hxreborn/discover-ads-filter/issues"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsDetailScaffold(title = stringResource(R.string.pref_category_about), onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_about_discover),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            App.boundService?.let { service ->
                Text(
                    text = "${service.frameworkName} v${service.frameworkVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        AboutCard(
            icon = Icons.Outlined.Code,
            title = stringResource(R.string.about_source_code),
            subtitle = stringResource(R.string.about_source_code_summary),
            shape = shapeForPosition(2, 0),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())) },
        )
        Spacer(Modifier.height(2.dp))
        AboutCard(
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.about_report_issue),
            subtitle = stringResource(R.string.about_report_issue_summary),
            shape = shapeForPosition(2, 1),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_ISSUES_URL.toUri())) },
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.about_libraries),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))

        val libs =
            listOf(
                "DexKit" to "LuckyPray / LGPL-3.0",
                "libxposed" to "libxposed / Apache-2.0",
                "compose-preference" to "Hai Zhang / Apache-2.0",
                "kotlinx-serialization" to "JetBrains / Apache-2.0",
            )
        libs.forEachIndexed { index, (name, license) ->
            if (index > 0) Spacer(Modifier.height(2.dp))
            AboutCard(
                icon = Icons.Outlined.Extension,
                title = name,
                subtitle = license,
                shape = shapeForPosition(libs.size, index),
            )
        }
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
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
