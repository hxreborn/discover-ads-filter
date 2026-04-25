package eu.hxreborn.discoveradsfilter.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.hxreborn.discoveradsfilter.ui.screen.AboutScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DashboardScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DiagnosticsScreen
import eu.hxreborn.discoveradsfilter.ui.screen.LicensesScreen
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

@Composable
fun AppNavHost(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Destination.Dashboard)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider =
            entryProvider {
                entry<Destination.Dashboard> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { backStack.add(it) },
                    )
                }

                entry<Destination.Diagnostics> {
                    DiagnosticsScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Destination.About> {
                    AboutScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToLicenses = { backStack.add(Destination.Licenses) },
                    )
                }

                entry<Destination.Licenses> {
                    LicensesScreen(onBack = { backStack.removeLastOrNull() })
                }
            },
    )
}
