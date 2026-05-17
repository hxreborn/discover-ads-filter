package eu.hxreborn.discoveradsfilter

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.DexKitCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.hook.StreamSliceFilterHook
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.ProcessName
import eu.hxreborn.discoveradsfilter.util.Safe
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

@PublishedApi
internal lateinit var module: DiscoverAdsFilterModule
    private set

class DiscoverAdsFilterModule : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
        Logger.log(Log.INFO, "v${BuildConfig.VERSION_NAME} loaded in ${param.processName}")
    }

    @SuppressLint("PrivateApi")
    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage && param.packageName != AGSA_PKG) return

        val proc =
            ProcessName.current() ?: run {
                Logger.log(Log.WARN, "could not read /proc/self/cmdline; aborting")
                return
            }

        val prefs = getRemotePreferences(SettingsPrefs.GROUP)

        val versionCode = currentAgsaVersionCode()
        val targets = DexKitCache.load(versionCode, BuildConfig.VERSION_CODE, prefs)
        if (targets is ResolvedTargets.Resolved && SettingsPrefs.verbose.read(prefs)) {
            Logger.log(Log.DEBUG, "[resolved] adMeta=${targets.adMetadataClass}")
            Logger.log(Log.DEBUG, "[resolved] card=${targets.feedCardClass}")
            Logger.log(Log.DEBUG, "[resolved] adFlagField=${targets.adFlagFieldName}")
            Logger.log(Log.DEBUG, "[resolved] adLabelField=${targets.adLabelFieldName}")
            Logger.log(Log.DEBUG, "[resolved] adMetaField=${targets.adMetadataFieldName}")
            Logger.log(Log.DEBUG, "[resolved] stream=${targets.streamRenderableListMethod}")
            Logger.log(Log.DEBUG, "[resolved] processors=${targets.cardProcessorMethods.size}")
        }
        if (targets is ResolvedTargets.Missing) {
            val allKeys = runCatching { prefs.all.keys.sorted() }.getOrDefault(emptyList())
            val lastRemoteWrite =
                runCatching {
                    SettingsPrefs.lastRemoteWrite.read(prefs)
                }.getOrDefault(0L)
            Logger.log(
                Log.WARN,
                "skipped proc=$proc v=$versionCode reason=${targets.reason} " +
                    "lastRemoteWrite=$lastRemoteWrite allKeys=$allKeys",
            )
        }

        val ok =
            runCatching {
                StreamSliceFilterHook.install(
                    param.classLoader,
                    prefs,
                    targets,
                    proc,
                )
                true
            }.onFailure { t ->
                Safe.logFailure("install StreamSliceFilterHook", t)
            }.isSuccess

        val status = if (ok) "1/1" else "0/1 failed:StreamSliceFilterHook"
        Logger.log(
            Log.INFO,
            "installed proc=$proc agsaV=$versionCode hooks=$status ${targets.summary()}",
        )
    }

    @Suppress("PrivateApi")
    private fun currentAgsaVersionCode(): Long =
        runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val app =
                activityThread
                    .getMethod(
                        "currentApplication",
                    ).invoke(null) as? Application ?: return@runCatching 0L
            app.packageManager.getPackageInfo(AGSA_PKG, 0).longVersionCode
        }.getOrDefault(0L)

    companion object {
        const val AGSA_PKG = "com.google.android.googlequicksearchbox"
        const val TAG = "DiscoverAdsFilter"
    }
}
