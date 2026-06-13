package eu.hxreborn.discoveradsfilter.util

import android.content.pm.PackageManager
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule

fun PackageManager.agsaApkPath(): String? =
    runCatching { getApplicationInfo(DiscoverAdsFilterModule.AGSA_PKG, 0).sourceDir }.getOrNull()

fun PackageManager.agsaVersionCode(): Long =
    runCatching {
        getPackageInfo(DiscoverAdsFilterModule.AGSA_PKG, 0).longVersionCode
    }.getOrNull() ?: 0L
