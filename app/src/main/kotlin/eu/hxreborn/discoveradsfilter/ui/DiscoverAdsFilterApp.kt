package eu.hxreborn.discoveradsfilter.ui

import androidx.compose.runtime.Composable
import eu.hxreborn.discoveradsfilter.ui.screen.HomeScreen
import eu.hxreborn.discoveradsfilter.ui.theme.DiscoverAdsFilterTheme

@Composable
fun DiscoverAdsFilterApp() {
    DiscoverAdsFilterTheme {
        HomeScreen()
    }
}
