package eu.hxreborn.discoveradsfilter.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable data object Dashboard : Destination

    @Serializable data object Diagnostics : Destination

    @Serializable data object About : Destination
}
