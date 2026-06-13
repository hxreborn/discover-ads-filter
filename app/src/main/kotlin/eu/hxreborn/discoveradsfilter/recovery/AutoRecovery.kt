package eu.hxreborn.discoveradsfilter.recovery

import android.content.Context
import android.util.Log
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.discovery.DexKitResolver
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.prefs.PrefsRepository
import eu.hxreborn.discoveradsfilter.util.RootShell
import eu.hxreborn.discoveradsfilter.util.agsaApkPath
import eu.hxreborn.discoveradsfilter.util.agsaVersionCode

object AutoRecovery {
    private const val TAG = "DiscoverAdsFilter/Recovery"

    suspend fun run(
        context: Context,
        repo: PrefsRepository,
        signalVersion: Long,
    ) {
        val pkg = DiscoverAdsFilterModule.AGSA_PKG
        val pm = context.packageManager
        val apkPath = pm.agsaApkPath()
        val versionCode = pm.agsaVersionCode().takeIf { it > 0L } ?: signalVersion

        if (apkPath == null) {
            Log.w(TAG, "recovery scan failed pkg=$pkg reason=apk path unavailable")
            return
        }

        Log.i(TAG, "recovery scan started pkg=$pkg v=$versionCode apk=$apkPath")
        when (val resolved = DexKitResolver.resolveAll(apkPath)) {
            is ResolvedTargets.Resolved -> {
                repo.writeResolvedTargets(versionCode, resolved)
                Log.i(TAG, "recovery scan finished pkg=$pkg v=$versionCode ${resolved.summary()}")
                Log.i(TAG, "force-close attempt pkg=$pkg")
                RootShell
                    .forceStop(pkg)
                    .onSuccess { Log.i(TAG, "force-close ok pkg=$pkg") }
                    .onFailure { Log.w(TAG, "force-close failed pkg=$pkg err=${it.message}") }
            }

            // Skip force-stop on failure since restarting hits the same cache miss and re-signals.
            is ResolvedTargets.Missing -> {
                Log.w(TAG, "recovery scan failed pkg=$pkg v=$versionCode reason=${resolved.reason}")
            }
        }
    }
}
