package eu.hxreborn.discoveradsfilter.ui.screen

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.SettingsDetailScaffold
import eu.hxreborn.discoveradsfilter.ui.components.SoftBlobBadge
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.theme.Spacing
import eu.hxreborn.discoveradsfilter.ui.util.shapeForPosition

private const val GITHUB_URL = "https://github.com/hxreborn/discover-ads-filter"
private const val GITHUB_ISSUES_URL = "https://github.com/hxreborn/discover-ads-filter/issues"
private const val BLOB_SPIN_DURATION_MS = 8_000

private val BlobSize: Dp = 128.dp
private val BlobIconSize: Dp = 72.dp

private val EMPTY_CLICK: () -> Unit = {}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLicenses: () -> Unit = {},
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "blob_spin")
    val blobRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = BLOB_SPIN_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "blob_rotation",
    )

    SettingsDetailScaffold(
        title = stringResource(R.string.pref_category_about),
        onBack = onBack,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(BlobSize),
                contentAlignment = Alignment.Center,
            ) {
                SoftBlobBadge(
                    modifier = Modifier.graphicsLayer { rotationZ = blobRotation },
                    size = BlobSize,
                )
                Image(
                    painter = painterResource(R.drawable.ic_about_discover),
                    contentDescription = null,
                    modifier = Modifier.size(BlobIconSize),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.BUILD_TYPE} build",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.about_links),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.xs))

        AboutCard(
            icon = { Icon(painter = painterResource(R.drawable.ic_github_24), contentDescription = null) },
            title = stringResource(R.string.about_source_code),
            subtitle = stringResource(R.string.about_source_code_summary),
            shape = shapeForPosition(3, 0),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())) },
        )
        Spacer(Modifier.height(2.dp))
        AboutCard(
            icon = { Icon(imageVector = Icons.Outlined.Gavel, contentDescription = null) },
            title = stringResource(R.string.about_licenses),
            subtitle = stringResource(R.string.about_licenses_summary),
            shape = shapeForPosition(3, 1),
            onClick = onNavigateToLicenses,
        )
        Spacer(Modifier.height(2.dp))
        AboutCard(
            icon = { Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null) },
            title = stringResource(R.string.about_report_issue),
            subtitle = stringResource(R.string.about_report_issue_summary),
            shape = shapeForPosition(3, 2),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_ISSUES_URL.toUri())) },
        )
    }
}

@Composable
private fun AboutCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick ?: EMPTY_CLICK,
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(name = "About", showSystemUi = true)
@Composable
private fun AboutScreenPreview() {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        AboutScreen(onBack = {})
    }
}
