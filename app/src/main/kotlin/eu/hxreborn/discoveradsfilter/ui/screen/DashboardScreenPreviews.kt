@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.hxreborn.discoveradsfilter.ui.components.StatusCard
import eu.hxreborn.discoveradsfilter.ui.preview.DiagnosticsPreviewCase
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
    )

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    DiscoverAdsFilterTheme(dynamicColor = false) {
        Surface(content = content)
    }
}

private class VerifyStateProvider : PreviewParameterProvider<VerifyUiState> {
    override val values = PreviewFixtures.verifyStatesAll.asSequence()
}

private class DashboardStateProvider : PreviewParameterProvider<HomeUiState> {
    override val values = PreviewFixtures.homeStatesAll.asSequence()
}

private class DiagnosticsCaseProvider : PreviewParameterProvider<DiagnosticsPreviewCase> {
    override val values = PreviewFixtures.diagnosticsPreviewCases.asSequence()
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview(
    @PreviewParameter(VerifyStateProvider::class) state: VerifyUiState,
) {
    PreviewSurface { StatusCard(state = state) }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsScreenPreview(
    @PreviewParameter(DiagnosticsCaseProvider::class) preview: DiagnosticsPreviewCase,
) {
    PreviewSurface {
        DiagnosticsContent(
            state = preview.state,
            sections = preview.sections,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    PreviewSurface { AboutScreen(onBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview(
    @PreviewParameter(DashboardStateProvider::class) state: HomeUiState,
) {
    PreviewSurface { DashboardScreenContent(state = state, actions = NoOpActions, onNavigate = {}) }
}
