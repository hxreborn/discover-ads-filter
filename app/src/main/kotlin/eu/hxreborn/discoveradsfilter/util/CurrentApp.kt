package eu.hxreborn.discoveradsfilter.util

import android.app.Application

object CurrentApp {
    @Suppress("PrivateApi")
    fun get(): Application? =
        runCatching {
            Class
                .forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Application
        }.getOrNull()
}
