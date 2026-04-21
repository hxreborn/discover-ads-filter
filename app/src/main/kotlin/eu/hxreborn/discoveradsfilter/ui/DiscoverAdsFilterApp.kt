package eu.hxreborn.discoveradsfilter.ui

import androidx.compose.runtime.Composable
import eu.hxreborn.discoveradsfilter.ui.navigation.AppNavHost
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

@Composable
fun DiscoverAdsFilterApp(viewModel: HomeViewModel) {
    DiscoverAdsFilterTheme {
        AppNavHost(viewModel = viewModel)
    }
}
