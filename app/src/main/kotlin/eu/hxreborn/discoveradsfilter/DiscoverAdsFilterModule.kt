package eu.hxreborn.discoveradsfilter

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.discovery.FingerprintCache
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.hook.HookMetrics
import eu.hxreborn.discoveradsfilter.hook.StreamSliceFilterHook
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
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
        log(Log.INFO, TAG, "v${BuildConfig.VERSION_NAME} loaded in ${param.processName}")
    }

    @SuppressLint("PrivateApi")
    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage) return
        if (param.packageName != AGSA_PKG) return

        val proc =
            ProcessName.current() ?: run {
                log(Log.WARN, TAG, "could not read /proc/self/cmdline; aborting")
                return
            }

        val prefs = getRemotePreferences(SettingsPrefs.GROUP)

        val versionCode = currentAgsaVersionCode()
        val targets = FingerprintCache.load(versionCode, BuildConfig.VERSION_CODE, prefs)
        if (targets is ResolvedTargets.Missing) {
            val allKeys = runCatching { prefs.all.keys.sorted() }.getOrDefault(emptyList())
            val lastRemoteWrite =
                runCatching {
                    SettingsPrefs.lastRemoteWrite.read(prefs)
                }.getOrDefault(0L)
            log(
                Log.WARN,
                TAG,
                "skipped proc=$proc v=$versionCode reason=${targets.reason} " +
                    "lastRemoteWrite=$lastRemoteWrite allKeys=$allKeys",
            )
        }

        HookMetrics.init()

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
                Safe.logFailure(TAG, "install StreamSliceFilterHook", t)
            }.isSuccess

        val status = if (ok) "1/1" else "0/1 failed:StreamSliceFilterHook"
        runCatching {
            prefs.edit(commit = true) {
                SettingsPrefs.hookStatus.write(this, status)
                SettingsPrefs.hookProcess.write(this, proc)
            }
        }

        log(
            Log.INFO,
            TAG,
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
