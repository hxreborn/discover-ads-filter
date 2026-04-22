@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme

private val NoOpActions =
    HomeActions(
        onVerboseChange = {},
        onFilterEnabledChange = {},
        onVerify = {},
        onClearCacheOnly = {},
        onResetAdsCounter = {},
        onDismissStartupScan = {},
    )

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        Surface(content = content)
    }
}

@Composable
private fun StatusCardPreviewContent(state: VerifyUiState) {
    PreviewSurface { StatusCard(state = state) }
}

@Composable
private fun DiagnosticsPreviewContent(
    state: VerifyUiState,
    sections: List<eu.hxreborn.discoveradsfilter.ui.state.SymbolSection>,
) {
    PreviewSurface {
        DiagnosticsContent(
            state = state,
            sections = sections,
        )
    }
}

@Composable
private fun DashboardPreviewContent(state: HomeUiState) {
    PreviewSurface { DashboardScreenContent(state = state, actions = NoOpActions, onNavigate = {}) }
}

@Preview(name = "Not Configured", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardNotConfiguredPreview() {
    StatusCardPreviewContent(PreviewFixtures.verifyNotScanned())
}

@Preview(name = "Active", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardActivePreview() {
    StatusCardPreviewContent(PreviewFixtures.verifySuccessFull())
}

@Preview(name = "Stale Scan", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardStalePreview() {
    StatusCardPreviewContent(PreviewFixtures.verifyStaleModuleUpdated())
}

@Preview(name = "Scan Failed", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardScanFailedPreview() {
    StatusCardPreviewContent(PreviewFixtures.verifyFailureDexKitNoMatches())
}

@Preview(name = "Module Inactive", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardModuleInactivePreview() {
    StatusCardPreviewContent(PreviewFixtures.verifyModuleNotActive())
}

@Preview(name = "All Targets Mapped", group = "Diagnostics", showBackground = true)
@Composable
private fun DiagnosticsAllMappedPreview() {
    DiagnosticsPreviewContent(
        state = PreviewFixtures.verifySuccessFull(),
        sections = PreviewFixtures.allMappedSections(),
    )
}

@Preview(name = "Mixed Issues", group = "Diagnostics", showBackground = true)
@Composable
private fun DiagnosticsMixedIssuesPreview() {
    DiagnosticsPreviewContent(
        state = PreviewFixtures.verifySuccessFull(),
        sections = PreviewFixtures.mixedSections(),
    )
}

@Preview(name = "Long Values", group = "Diagnostics", showBackground = true)
@Composable
private fun DiagnosticsLongValuesPreview() {
    DiagnosticsPreviewContent(
        state = PreviewFixtures.verifySuccessFull(),
        sections = PreviewFixtures.longValueSections(),
    )
}

@Preview(name = "No Targets Resolved", group = "Diagnostics", showBackground = true)
@Composable
private fun DiagnosticsNoTargetsResolvedPreview() {
    DiagnosticsPreviewContent(
        state = PreviewFixtures.verifyNoTargetsResolved(),
        sections = PreviewFixtures.zeroResolvedSections(),
    )
}

@Preview(name = "About", group = "About", showBackground = true)
@Composable
private fun AboutScreenPreview() {
    PreviewSurface { AboutScreen(onBack = {}) }
}

@Preview(name = "Ready", group = "Dashboard", showBackground = true)
@Composable
private fun DashboardReadyPreview() {
    DashboardPreviewContent(HomeUiState.Ready(verify = PreviewFixtures.verifySuccessFull()))
}

@Preview(name = "Scanning", group = "Dashboard", showBackground = true)
@Composable
private fun DashboardScanningPreview() {
    DashboardPreviewContent(HomeUiState.Ready(verify = PreviewFixtures.verifyRunning()))
}

@Preview(name = "Scan Failed", group = "Dashboard", showBackground = true)
@Composable
private fun DashboardScanFailedPreview() {
    DashboardPreviewContent(HomeUiState.Ready(verify = PreviewFixtures.verifyFailureDexKitNoMatches()))
}

@Preview(name = "Module Inactive", group = "Dashboard", showBackground = true)
@Composable
private fun DashboardModuleInactivePreview() {
    DashboardPreviewContent(HomeUiState.Ready(verify = PreviewFixtures.verifyModuleNotActive()))
}
