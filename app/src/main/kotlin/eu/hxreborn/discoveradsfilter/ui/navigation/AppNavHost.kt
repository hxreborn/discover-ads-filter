package eu.hxreborn.discoveradsfilter.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import eu.hxreborn.discoveradsfilter.ui.screen.AboutScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DashboardScreen
import eu.hxreborn.discoveradsfilter.ui.screen.DiagnosticsScreen
import eu.hxreborn.discoveradsfilter.ui.screen.LicensesScreen
import eu.hxreborn.discoveradsfilter.ui.viewmodel.HomeViewModel

private val slideTransitionMetadata =
    NavDisplay.transitionSpec {
        slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
    } +
        NavDisplay.popTransitionSpec {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
        } +
        NavDisplay.predictivePopTransitionSpec {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
        }

@Composable
fun AppNavHost(viewModel: HomeViewModel) {
    val backStack = rememberNavBackStack(Destination.Dashboard)
    val navigateUp: () -> Unit = { backStack.removeLastOrNull() }

    val entries =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        ) { destination ->
            when (destination) {
                Destination.Dashboard -> {
                    NavEntry(destination) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { backStack.add(it) },
                        )
                    }
                }

                Destination.Diagnostics -> {
                    NavEntry(destination, metadata = slideTransitionMetadata) {
                        DiagnosticsScreen(
                            viewModel = viewModel,
                            onBack = navigateUp,
                        )
                    }
                }

                Destination.About -> {
                    NavEntry(destination, metadata = slideTransitionMetadata) {
                        AboutScreen(
                            onBack = navigateUp,
                            onNavigateToLicenses = { backStack.add(Destination.Licenses) },
                        )
                    }
                }

                Destination.Licenses -> {
                    NavEntry(destination, metadata = slideTransitionMetadata) {
                        LicensesScreen(onBack = navigateUp)
                    }
                }

                else -> {
                    error("Unsupported destination: $destination")
                }
            }
        }

    val sceneState =
        rememberSceneState(
            entries = entries,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            onBack = navigateUp,
        )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map(::SceneInfo)

    val gestureState =
        key(currentInfo, previousSceneInfos) {
            rememberNavigationEventState(
                currentInfo = currentInfo,
                backInfo = previousSceneInfos,
            )
        }

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            repeat(backStack.size - scene.previousEntries.size) { navigateUp() }
        },
    )

    NavDisplay(
        sceneState = sceneState,
        navigationEventState = gestureState,
    )
}
