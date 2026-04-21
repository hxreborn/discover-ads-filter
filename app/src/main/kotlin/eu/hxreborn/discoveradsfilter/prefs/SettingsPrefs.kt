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

    // DexKit cache: written by the module-app, read by the hook process
    val fingerprintCurrent = nullableStringPref("fp_v4_current")
    val fingerprintCurrentVersion = longPref("fp_v4_current_version", 0L)
    val fingerprintCurrentModuleVersion = intPref("fp_v4_current_module_version", 0)

    const val KEY_FINGERPRINT_PREFIX = "fp_v4_"

    fun fingerprintKey(
        agsaVersionCode: Long,
        moduleVersionCode: Int,
    ): String = "${KEY_FINGERPRINT_PREFIX}${agsaVersionCode}_m$moduleVersionCode"
}
