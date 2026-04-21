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

        if (exactRaw == null) {
            return ResolvedTargets.Missing(
                missingReason(versionCode = versionCode),
            )
        }

        val candidates = listOf("exact" to exactRaw)

        val failures = mutableListOf<String>()
        for ((source, raw) in candidates) {
            val decoded =
                runCatching { json.decodeFromString(ResolvedTargets.serializer(), raw) }
                    .onFailure {
                        module.log(
                            Log.ERROR,
                            TAG,
                            "failed to parse cached fingerprints from $source",
                            it,
                        )
                    }.getOrNull()

            failures +=
                when {
                    decoded == null -> "$source: corrupt cache entry"
                    decoded.isUsableHookCache() -> return decoded
                    decoded is ResolvedTargets.Missing -> "$source: ${decoded.reason}"
                    else -> "$source: cached targets contained no installable hook methods"
                }
        }

        return ResolvedTargets.Missing(
            "no usable hook cache for AGSA v$versionCode; " +
                "${failures.joinToString("; ")}; rerun Verify after updating signatures",
        )
    }

    fun encode(targets: ResolvedTargets): String =
        json.encodeToString(ResolvedTargets.serializer(), targets)

    fun decode(raw: String): ResolvedTargets =
        json.decodeFromString(ResolvedTargets.serializer(), raw)

    private fun ResolvedTargets.isUsableHookCache(): Boolean =
        when (this) {
            is ResolvedTargets.Missing -> {
                false
            }

            is ResolvedTargets.Resolved -> {
                adMetadataClass != null && feedCardClass != null
            }
        }

    private fun missingReason(versionCode: Long): String =
        if (versionCode == 0L) {
            "AGSA version unavailable and no cache exists"
        } else {
            "no cache for AGSA v$versionCode; run Verify from module app"
        }
}
