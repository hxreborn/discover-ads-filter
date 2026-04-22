@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.preview.PreviewFixtures
import eu.hxreborn.discoveradsfilter.ui.state.HomeActions
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.state.SymbolSection
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

private class StatusCardStateProvider : PreviewParameterProvider<VerifyUiState> {
    override val values: Sequence<VerifyUiState> =
        sequenceOf(
            PreviewFixtures.verifyNotScanned(),
            PreviewFixtures.verifySuccessFull(),
            PreviewFixtures.verifyModuleNotActive(),
        )
}

@Composable
private fun DiagnosticsPreviewContent(
    state: VerifyUiState,
    sections: List<SymbolSection>,
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

@Preview(name = "Status Card", group = "Status Card", showBackground = true)
@Composable
private fun StatusCardPreview(
    @PreviewParameter(StatusCardStateProvider::class) state: VerifyUiState,
) {
    StatusCardPreviewContent(state)
}

@Preview(name = "Diagnostics", group = "Diagnostics", showBackground = true)
@Composable
private fun DiagnosticsPreview() {
    DiagnosticsPreviewContent(
        state = PreviewFixtures.verifySuccessFull(),
        sections = PreviewFixtures.mixedSections(),
    )
}

@Preview(name = "About", group = "About", showBackground = true)
@Composable
private fun AboutScreenPreview() {
    PreviewSurface { AboutScreen(onBack = {}) }
}

private class DashboardStateProvider : PreviewParameterProvider<HomeUiState> {
    override val values: Sequence<HomeUiState> =
        sequenceOf(
            HomeUiState.Ready(verify = PreviewFixtures.verifySuccessFull()),
            HomeUiState.Ready(verify = PreviewFixtures.verifyRunning()),
            HomeUiState.Ready(verify = PreviewFixtures.verifyFailureDexKitNoMatches()),
        )
}

@Preview(name = "Dashboard", group = "Dashboard", showBackground = true)
@Composable
private fun DashboardPreview(
    @PreviewParameter(DashboardStateProvider::class) state: HomeUiState,
) {
    DashboardPreviewContent(state)
}
