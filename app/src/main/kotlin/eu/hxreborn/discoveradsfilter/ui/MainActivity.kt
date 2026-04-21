package eu.hxreborn.discoveradsfilter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import eu.hxreborn.discoveradsfilter.App
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MainActivity :
    ComponentActivity(),
    XposedServiceHelper.OnServiceListener {
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.uiState.value is HomeUiState.Loading
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        App.addServiceListener(this)

        setContent { DiscoverAdsFilterApp(viewModel) }
    }

    override fun onServiceBind(service: XposedService) {
        viewModel.onServiceBound()
    }

    override fun onServiceDied(service: XposedService) = Unit

    override fun onDestroy() {
        App.removeServiceListener(this)
        super.onDestroy()
    }
}
