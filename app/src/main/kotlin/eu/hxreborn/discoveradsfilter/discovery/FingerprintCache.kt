package eu.hxreborn.discoveradsfilter.discovery

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs
import kotlinx.serialization.json.Json

object FingerprintCache {
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
        val currentVersion =
            runCatching {
                SettingsPrefs.fingerprintCurrentVersion.read(prefs)
            }.getOrDefault(0L)
        val currentModuleVersion =
            runCatching {
                SettingsPrefs.fingerprintCurrentModuleVersion.read(prefs)
            }.getOrDefault(0)
        val exactRaw =
            versionCode
                .takeIf { it > 0L }
                ?.let { prefs.getString(SettingsPrefs.fingerprintKey(it, moduleVersionCode), null) }
        val currentRaw =
            SettingsPrefs.fingerprintCurrent.read(prefs)?.takeIf {
                (versionCode == 0L || currentVersion == versionCode) &&
                    (currentModuleVersion == 0 || currentModuleVersion == moduleVersionCode)
            }

        val candidates =
            buildList {
                exactRaw?.let { add("exact" to it) }
                if (currentRaw != null && currentRaw != exactRaw) {
                    add("current(v$currentVersion)" to currentRaw)
                }
            }

        if (candidates.isEmpty()) {
            return ResolvedTargets.Missing(
                missingReason(versionCode = versionCode, currentVersion = currentVersion),
            )
        }

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

    private fun missingReason(
        versionCode: Long,
        currentVersion: Long,
    ): String =
        when {
            versionCode == 0L && currentVersion > 0L -> {
                "hook-side AGSA version unavailable; only cached v$currentVersion exists"
            }

            versionCode == 0L -> {
                "hook-side AGSA version unavailable and no current cache exists"
            }

            currentVersion > 0L && currentVersion != versionCode -> {
                "no matching cache for AGSA v$versionCode (current cache is v$currentVersion); run Verify from module app"
            }

            else -> {
                "no cache for AGSA v$versionCode; run Verify from module app"
            }
        }
}
