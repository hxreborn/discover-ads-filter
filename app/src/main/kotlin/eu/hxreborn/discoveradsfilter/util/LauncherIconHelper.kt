package eu.hxreborn.discoveradsfilter.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal fun isLauncherIconVisible(context: Context): Boolean {
    val state =
        context.packageManager.getComponentEnabledSetting(
            ComponentName(context.packageName, "${context.packageName}.LauncherAlias"),
        )
    return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

internal fun setLauncherIconVisible(
    context: Context,
    visible: Boolean,
) {
    context.packageManager.setComponentEnabledSetting(
        ComponentName(context.packageName, "${context.packageName}.LauncherAlias"),
        if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        },
        PackageManager.DONT_KILL_APP,
    )
}
