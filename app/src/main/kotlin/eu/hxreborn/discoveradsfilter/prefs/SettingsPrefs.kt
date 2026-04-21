package eu.hxreborn.discoveradsfilter.prefs

object SettingsPrefs {
    const val GROUP = "discover_adsfilter_prefs"

    val verbose = boolPref("verbose", false)
    val filterEnabled = boolPref("filter_enabled", true)

    // Read by the module-app via remote prefs, written by the hook process
    val hookStatus = nullableStringPref("hook_install_status")
    val hookProcess = nullableStringPref("hook_process")
    val adsHidden = longPref("ads_hidden", 0L)
    val lastRemoteWrite = longPref("_last_remote_write", 0L)

    const val KEY_FINGERPRINT_PREFIX = "fp_v4_"

    fun fingerprintKey(
        agsaVersionCode: Long,
        moduleVersionCode: Int,
    ): String = "${KEY_FINGERPRINT_PREFIX}${agsaVersionCode}_m$moduleVersionCode"
}
