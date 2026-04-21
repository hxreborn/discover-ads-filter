package eu.hxreborn.discoveradsfilter.discovery

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import kotlinx.serialization.json.Json

object DexKitCache {
    private const val TAG = "DiscoverAdsFilter/Fp"

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun load(
        versionCode: Long,
        moduleVersionCode: Int,
        prefs: SharedPreferences,
    ): ResolvedTargets {
        val exactRaw =
            versionCode
                .takeIf { it > 0L }
                ?.let { prefs.getString(SettingsPrefs.fingerprintKey(it, moduleVersionCode), null) }
                ?: return ResolvedTargets.Missing(missingReason(versionCode = versionCode))

        val decoded =
            runCatching { json.decodeFromString(ResolvedTargets.serializer(), exactRaw) }
                .onFailure {
                    module.log(Log.ERROR, TAG, "failed to parse cached targets", it)
                }.getOrNull()

        if (decoded != null && decoded.isUsableHookCache()) return decoded

        val reason =
            when (decoded) {
                null -> "corrupt cache entry"
                is ResolvedTargets.Missing -> decoded.reason
                else -> "cached targets contained no installable hook methods"
            }
        return ResolvedTargets.Missing(
            "no usable hook cache for AGSA v$versionCode; $reason; rerun Verify after updating signatures",
        )
    }

    fun encode(targets: ResolvedTargets): String =
        json.encodeToString(ResolvedTargets.serializer(), targets)

    fun decode(raw: String): ResolvedTargets =
        json.decodeFromString(ResolvedTargets.serializer(), raw)

    private fun ResolvedTargets.isUsableHookCache(): Boolean =
        this is ResolvedTargets.Resolved && adMetadataClass != null && feedCardClass != null

    private fun missingReason(versionCode: Long): String =
        if (versionCode == 0L) {
            "AGSA version unavailable and no cache exists"
        } else {
            "no cache for AGSA v$versionCode; run Verify from module app"
        }
}
