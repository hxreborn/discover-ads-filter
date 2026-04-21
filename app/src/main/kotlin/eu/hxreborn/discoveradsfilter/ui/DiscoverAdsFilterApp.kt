package eu.hxreborn.discoveradsfilter.ui

import androidx.compose.runtime.Composable
import eu.hxreborn.discoveradsfilter.ui.screen.HomeScreen
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

@Composable
fun DiscoverAdsFilterApp(viewModel: HomeViewModel) {
    DiscoverAdsFilterTheme {
        HomeScreen(viewModel = viewModel)
    }
}
