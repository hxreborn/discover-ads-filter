package eu.hxreborn.discoveradsfilter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.DexKitCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.hook.StreamSliceFilterHook
import eu.hxreborn.discoveradsfilter.hook.loadHookPrefs
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.ProcessName
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
        loadHookPrefs(prefs)

        runCatching {
            val attach =
                Application::class.java.getDeclaredMethod("attach", Context::class.java)
            attach.isAccessible = true
            deoptimize(attach)
            hook(attach).intercept(BootstrapHooker(param.classLoader, prefs, proc))
        }.onFailure { Logger.log(Log.ERROR, "failed: bootstrap hook", it) }
    }

    companion object {
        const val AGSA_PKG = "com.google.android.googlequicksearchbox"
        const val TAG = "DiscoverAdsFilter"
    }
}

private class BootstrapHooker(
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

        runCatching {
            val ctx = chain.args[0] as Context
            val versionCode =
                ctx.packageManager
                    .getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0)
                    .longVersionCode

            val targets = DexKitCache.load(versionCode, BuildConfig.VERSION_CODE, prefs)

            if (targets is ResolvedTargets.Resolved && SettingsPrefs.verbose.read(prefs)) {
                Logger.log(
                    Log.DEBUG,
                    "[resolved] stream=${targets.streamRenderableListMethod} " +
                        "adMeta=${targets.adMetadataClass} card=${targets.feedCardClass} " +
                        "processors=${targets.cardProcessorMethods.size}",
                )
            }

            if (targets is ResolvedTargets.Missing) {
                val lastRemoteWrite =
                    runCatching { SettingsPrefs.lastRemoteWrite.read(prefs) }.getOrDefault(0L)
                val fields =
                    buildList {
                        add("skipped")
                        add("proc=$proc")
                        add("v=$versionCode")
                        add("reason=${targets.reason}")
                        add("lastRemoteWrite=$lastRemoteWrite")
                        if (SettingsPrefs.verbose.read(prefs)) {
                            val keys =
                                runCatching { prefs.all.keys.sorted() }.getOrDefault(emptyList())
                            add("allKeys=$keys")
                        }
                    }
                Logger.log(Log.WARN, fields.joinToString(" "))
            }

            val streamHookInstalled =
                runCatching {
                    StreamSliceFilterHook.install(loader, prefs, targets, proc)
                }.getOrElse {
                    Logger.log(Log.ERROR, "failed: install StreamSliceFilterHook", it)
                    false
                }
            val status = if (streamHookInstalled) "1/1" else "0/1 failed:StreamSliceFilterHook"
            Logger.log(
                Log.INFO,
                "installed proc=$proc agsaV=$versionCode hooks=$status ${targets.summary()}",
            )
        }.onFailure { Logger.log(Log.ERROR, "failed: deferred install", it) }
        return null
    }
}
