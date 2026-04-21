@file:Suppress("ktlint:standard:function-naming")

package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.hxreborn.discoveradsfilter.ui.components.DexKitDialog
import eu.hxreborn.discoveradsfilter.ui.components.HeroStatusCard
import eu.hxreborn.discoveradsfilter.ui.components.diagnosticsItems
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

private class HomeStateProvider : PreviewParameterProvider<HomeUiState> {
    override val values = PreviewFixtures.homeStatesAll.asSequence()
}

private class DexKitDialogStateProvider : PreviewParameterProvider<VerifyUiState> {
    override val values = PreviewFixtures.dexKitDialogStates.asSequence()
}

@Preview(showBackground = true)
@Composable
private fun HeroStatusCardPreview(
    @PreviewParameter(VerifyStateProvider::class) state: VerifyUiState,
) {
    PreviewSurface { HeroStatusCard(state = state) }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsItemsPreview(
    @PreviewParameter(VerifyStateProvider::class) state: VerifyUiState,
) {
    PreviewSurface {
        val surface = MaterialTheme.colorScheme.surfaceVariant
        LazyColumn {
            diagnosticsItems(
                state = state,
                surface = surface,
                onInspectFingerprints = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DexKitDialogPreview(
    @PreviewParameter(DexKitDialogStateProvider::class) state: VerifyUiState,
) {
    PreviewSurface { DexKitDialog(state = state, onDismiss = {}) }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeStateProvider::class) state: HomeUiState,
) {
    PreviewSurface { HomeScreenContent(state = state, actions = NoOpActions) }
}
