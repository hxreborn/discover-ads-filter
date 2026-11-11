package eu.hxreborn.discoveradsfilter.ui.viewmodel

import android.app.Application
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.hxreborn.discoveradsfilter.ui.state.Contributor
import eu.hxreborn.discoveradsfilter.ui.state.parseContributorEntries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContributorsViewModel(
    app: Application,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val contributorsFlow = MutableStateFlow<List<Contributor>>(emptyList())
    val contributors: StateFlow<List<Contributor>> = contributorsFlow

    init {
        viewModelScope.launch(ioDispatcher) {
            contributorsFlow.value =
                runCatching { loadContributors(app.assets) }
                    .onFailure { Log.w(TAG, "contributors snapshot read failed", it) }
                    .getOrDefault(emptyList())
        }
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter"
        private const val SNAPSHOT_ASSET = "contributors.json"

        val Factory =
            viewModelFactory {
                initializer { ContributorsViewModel(this[APPLICATION_KEY] as Application) }
            }

        private fun loadContributors(assets: AssetManager): List<Contributor> {
            val entries = parseContributorEntries(assets.open(SNAPSHOT_ASSET).bufferedReader().use { it.readText() })
            return entries.map { entry ->
                Contributor(
                    login = entry.login,
                    added = entry.added,
                    removed = entry.removed,
                    url = entry.url,
                    avatar =
                        runCatching {
                            assets.open(entry.avatar).use(BitmapFactory::decodeStream)?.asImageBitmap()
                        }.onFailure { Log.w(TAG, "avatar read failed login=${entry.login}", it) }.getOrNull(),
                    isOwner = entry.owner,
                )
            }
        }
    }
}
