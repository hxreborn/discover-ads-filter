package eu.hxreborn.discoveradsfilter.ui.navigation

import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.hxreborn.discoveradsfilter.ui.screen.AboutScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DashboardScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DiagnosticsScreen
import eu.hxreborn.discoveradsfilter.ui.screen.LicensesScreen
import eu.hxreborn.discoveradsfilter.ui.screen.NewsRulesScreen
import eu.hxreborn.discoveradsfilter.ui.state.HomeUiState
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

@Composable
fun AppNavHost(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Destination.Dashboard)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
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

                entry<Destination.NewsRules> {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    NewsRulesScreen(
                        rules = (state as? HomeUiState.Ready)?.newsRules.orEmpty(),
                        onSave = viewModel.actions.onNewsRuleSaved,
                        onDelete = viewModel.actions.onNewsRuleDeleted,
                        onLoadPresets = viewModel.actions.onLoadPresets,
                        onImport = viewModel.actions.onImportRules,
                        onExport = viewModel.actions.onExportRules,
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
