package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.DexKitCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.hook.StreamSliceFilterHook
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.ProcessName
import eu.hxreborn.discoveradsfilter.util.Safe
import io.github.libxposed.api.XposedInterface
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

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage && param.packageName != AGSA_PKG) return

        val proc =
            ProcessName.current() ?: run {
                Logger.log(Log.WARN, "could not read /proc/self/cmdline; aborting")
                return
            }

        val prefs = getRemotePreferences(SettingsPrefs.GROUP)

        Safe.run("bootstrap hook") {
            val attach =
                Application::class.java.getDeclaredMethod("attach", Context::class.java)
            attach.isAccessible = true
            deoptimize(attach)
            hook(attach).intercept(BootstrapHook(param.classLoader, prefs, proc))
        }
    }

    companion object {
        const val AGSA_PKG = "com.google.android.googlequicksearchbox"
        const val TAG = "DiscoverAdsFilter"
    }
}

private class BootstrapHook(
    private val loader: ClassLoader,
    private val prefs: SharedPreferences,
    private val proc: String,
) : XposedInterface.Hooker {
    @Volatile
    private var installed = false

    override fun intercept(chain: XposedInterface.Chain): Any? {
        chain.proceed()
        if (installed) return null
        installed = true

        Safe.run("deferred install") {
            val ctx = chain.getArgs()[0] as Context
            val versionCode =
                ctx.packageManager
                    .getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0)
                    .longVersionCode

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
                val lastRemoteWrite =
                    runCatching {
                        SettingsPrefs.lastRemoteWrite.read(prefs)
                    }.getOrDefault(0L)
                val keysSuffix =
                    if (SettingsPrefs.verbose.read(prefs)) {
                        val allKeys =
                            runCatching { prefs.all.keys.sorted() }.getOrDefault(emptyList())
                        " allKeys=$allKeys"
                    } else {
                        ""
                    }
                Logger.log(
                    Log.WARN,
                    "skipped proc=$proc v=$versionCode reason=${targets.reason} " +
                        "lastRemoteWrite=$lastRemoteWrite$keysSuffix",
                )
            }

            val ok =
                runCatching {
                    StreamSliceFilterHook.install(loader, prefs, targets, proc)
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
        return null
    }
}
