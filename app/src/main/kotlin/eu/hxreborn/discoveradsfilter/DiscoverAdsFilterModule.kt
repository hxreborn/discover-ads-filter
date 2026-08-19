package eu.hxreborn.discoveradsfilter

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import eu.hxreborn.discoveradsfilter.discovery.DexKitResolver
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.discovery.TargetCache
import eu.hxreborn.discoveradsfilter.hook.StreamSliceFilterHook
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

@PublishedApi
internal lateinit var module: DiscoverAdsFilterModule

class DiscoverAdsFilterModule : XposedModule() {
    private lateinit var processName: String

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
        processName = param.processName
        if (processName != FEED_PROCESS) return
        Logger.info("loaded in $processName")
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET_PACKAGE ||
            processName != FEED_PROCESS ||
            !param.isFirstPackage
        ) {
            return
        }
        try {
            val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            attach.isAccessible = true
            deoptimize(attach)
            val interceptor = BootstrapInterceptor(this, param.classLoader)
            hook(attach)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain -> interceptor.intercept(chain) }
        } catch (exception: Exception) {
            Logger.error("bootstrap hook installation failed", exception)
        }
    }

    private class BootstrapInterceptor(
        private val module: DiscoverAdsFilterModule,
        private val classLoader: ClassLoader,
    ) {
        @Volatile
        private var installed = false

        fun intercept(chain: XposedInterface.Chain): Any? {
            chain.proceed()
            if (installed) return null
            synchronized(this) {
                if (installed) return null
                installed = true
            }
            module.install(chain.getArg(0) as Context, classLoader)
            return null
        }
    }

    private fun install(
        context: Context,
        classLoader: ClassLoader,
    ) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(TARGET_PACKAGE, 0)
            val targetVersionCode = packageInfo.longVersionCode
            val moduleVersionCode = BuildConfig.VERSION_CODE.toLong()
            val status =
                "Google app ${packageInfo.versionName} ($targetVersionCode) " +
                    "module v${BuildConfig.VERSION_NAME}"
            val applicationInfo = context.applicationInfo
            val cached =
                TargetCache.load(
                    dataDir = applicationInfo.dataDir,
                    targetVersionCode = targetVersionCode,
                    moduleVersionCode = moduleVersionCode,
                )
            val targets =
                cached ?: DexKitResolver
                    .resolve(
                        buildList {
                            add(applicationInfo.sourceDir)
                            applicationInfo.splitSourceDirs?.let(::addAll)
                        },
                    ).also { resolved ->
                        TargetCache.store(
                            dataDir = applicationInfo.dataDir,
                            targetVersionCode = targetVersionCode,
                            moduleVersionCode = moduleVersionCode,
                            targets = resolved,
                        )
                    }
            val source =
                when {
                    cached != null -> "from-cache"
                    DexKitResolver.hasNativeLoadFailure() -> "native-load-failed"
                    else -> "fresh-scan"
                }

            when (targets) {
                is ResolvedTargets.Missing -> {
                    Logger.error(
                        "resolution missing resolved=$source : $status reason=${targets.reason}",
                    )
                    notifyFilteringUnavailable(context)
                }

                is ResolvedTargets.Resolved -> {
                    val streamMethod = targets.streamRenderableListMethod
                    Logger.debug {
                        "targets resolved streamMethod=${streamMethod.className}." +
                            streamMethod.methodName
                    }
                    try {
                        StreamSliceFilterHook.install(this, classLoader, targets)
                        Logger.info("hooks installed hooks=[stream] resolved=$source : $status")
                    } catch (exception: Exception) {
                        Logger.error("hook group 'stream' failed to install", exception)
                        notifyFilteringUnavailable(context)
                    }
                }
            }
        } catch (exception: Exception) {
            Logger.error("deferred installation failed", exception)
            notifyFilteringUnavailable(context)
        }
    }

    private companion object {
        const val FILTERING_UNAVAILABLE_TOAST_DELAY_MS = 3000L
        const val FILTERING_UNAVAILABLE_MESSAGE =
            "Discover Ads Filter couldn't start. Ads may appear. Check Xposed logs."
        val TARGET_PACKAGE: String = BuildConfig.TARGET_PACKAGE
        val FEED_PROCESS = "$TARGET_PACKAGE:googleapp"

        fun notifyFilteringUnavailable(context: Context) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    try {
                        Toast
                            .makeText(context, FILTERING_UNAVAILABLE_MESSAGE, Toast.LENGTH_LONG)
                            .show()
                    } catch (_: Exception) {
                    }
                },
                FILTERING_UNAVAILABLE_TOAST_DELAY_MS,
            )
        }
    }
}
