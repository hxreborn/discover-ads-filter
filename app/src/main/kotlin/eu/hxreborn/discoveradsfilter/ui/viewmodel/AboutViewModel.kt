package eu.hxreborn.discoveradsfilter.ui.viewmodel

import android.app.Application
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.hxreborn.discoveradsfilter.ui.state.AboutInfo
import eu.hxreborn.discoveradsfilter.ui.state.Contributor
import eu.hxreborn.discoveradsfilter.ui.state.parseContributorEntries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    app: Application,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val aboutFlow = MutableStateFlow<AboutInfo?>(null)
    val about: StateFlow<AboutInfo?> = aboutFlow

    init {
        viewModelScope.launch(ioDispatcher) { aboutFlow.value = load(app) }
    }

    companion object {
        private const val TAG = "DiscoverAdsFilter"
        private const val SNAPSHOT_ASSET = "contributors.json"
        private const val ICON_PIXELS = 432

        val Factory =
            viewModelFactory {
                initializer { AboutViewModel(this[APPLICATION_KEY] as Application) }
            }

        private fun load(app: Application): AboutInfo {
            val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            return AboutInfo(
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = packageInfo.longVersionCode,
                icon =
                    runCatching { launcherArt(app) }
                        .onFailure { Log.w(TAG, "launcher icon read failed", it) }
                        .getOrNull(),
                contributors =
                    runCatching { loadContributors(app.assets) }
                        .onFailure { Log.w(TAG, "contributors snapshot read failed", it) }
                        .getOrDefault(emptyList()),
            )
        }

        private fun launcherArt(app: Application): ImageBitmap? {
            val icon = app.packageManager.getApplicationIcon(app.applicationInfo) as? AdaptiveIconDrawable ?: return null
            val bitmap = Bitmap.createBitmap(ICON_PIXELS, ICON_PIXELS, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            listOfNotNull<Drawable>(icon.background, icon.foreground).forEach { layer ->
                layer.setBounds(0, 0, ICON_PIXELS, ICON_PIXELS)
                layer.draw(canvas)
            }
            return bitmap.asImageBitmap()
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
