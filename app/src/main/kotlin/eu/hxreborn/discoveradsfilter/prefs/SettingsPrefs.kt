package eu.hxreborn.discoveradsfilter.prefs

object SettingsPrefs {
    const val GROUP = "discover_adsfilter_prefs"

    val verbose = BoolPref("verbose", false)

    // Written by the hook process; read by the module-app via remote prefs
    val hookStatus = NullableStringPref("hook_install_status")
    val hookProcess = NullableStringPref("hook_process")
    val adsHidden = LongPref("ads_hidden", 0L)
    val lastRemoteWrite = LongPref("_last_remote_write", 0L)

    // Fingerprint cache: written by the module-app, read by the hook process
    val fingerprintCurrent = NullableStringPref("fp_v4_current")
    val fingerprintCurrentVersion = LongPref("fp_v4_current_version", 0L)
    val fingerprintCurrentModuleVersion = IntPref("fp_v4_current_module_version", 0)

    const val KEY_FINGERPRINT_PREFIX = "fp_v4_"

    fun fingerprintKey(
        agsaVersionCode: Long,
        moduleVersionCode: Int,
    ): String = "${KEY_FINGERPRINT_PREFIX}${agsaVersionCode}_m$moduleVersionCode"
}
