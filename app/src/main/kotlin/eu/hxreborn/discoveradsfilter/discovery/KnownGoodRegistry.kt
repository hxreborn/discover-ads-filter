package eu.hxreborn.discoveradsfilter.discovery

import android.content.Context
import android.util.Log
import eu.hxreborn.discoveradsfilter.module
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class KnownGoodEntry(
    val agsaVersionCode: Long,
    val agsaVersionName: String? = null,
    // Device-local install timestamp; 0 for bundled entries (shared across users).
    // Non-zero local values detect silent reinstalls on the same versionCode.
    val agsaLastUpdateTime: Long = 0,
    val moduleVersionCode: Int,
    val moduleVersionName: String? = null,
    val targetsHash: String,
    val verifiedAt: Long,
    val notes: String? = null,
    val source: Source = Source.Local,
) {
    @Serializable
    enum class Source { Bundled, Local }
}

enum class RegistryStatus { Verified, Unverified, Unknown }

object KnownGoodRegistry {
    private const val TAG = "DiscoverAdsFilter/Reg"
    private const val BUNDLED_ASSET = "known_good.json"

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val listSerializer = ListSerializer(KnownGoodEntry.serializer())

    @Volatile
    private var bundledCache: List<KnownGoodEntry>? = null

    fun bundled(context: Context): List<KnownGoodEntry> {
        bundledCache?.let { return it }
        val loaded =
            runCatching {
                context.assets
                    .open(BUNDLED_ASSET)
                    .bufferedReader()
                    .use { it.readText() }
                    .let { raw ->
                        if (raw.isBlank()) {
                            emptyList()
                        } else {
                            json.decodeFromString(
                                listSerializer,
                                raw,
                            )
                        }
                    }.map { it.copy(source = KnownGoodEntry.Source.Bundled) }
            }.getOrElse { t ->
                module.log(Log.WARN, TAG, "failed to load bundled known_good.json", t)
                emptyList()
            }
        bundledCache = loaded
        return loaded
    }

    fun decode(raw: String?): List<KnownGoodEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrElse {
                module.log(Log.WARN, TAG, "failed to decode local known-good list", it)
                emptyList()
            }.map { it.copy(source = KnownGoodEntry.Source.Local) }
    }

    fun encode(entries: List<KnownGoodEntry>): String =
        json.encodeToString(
            listSerializer,
            entries.map { it.copy(source = KnownGoodEntry.Source.Local) },
        )

    fun hash(targets: ResolvedTargets.Resolved): String {
        // Canonical JSON with sorted keys — kotlinx.serialization emits declared-field order,
        // which is stable for a given class version. Hash the encoded form directly.
        val encoded = json.encodeToString(ResolvedTargets.Resolved.serializer(), targets)
        val digest = MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun status(
        agsaVersionCode: Long,
        agsaLastUpdateTime: Long,
        moduleVersionCode: Int,
        targets: ResolvedTargets.Resolved?,
        entries: List<KnownGoodEntry>,
    ): RegistryStatus {
        if (targets == null || agsaVersionCode <= 0L ||
            moduleVersionCode <= 0
        ) {
            return RegistryStatus.Unknown
        }
        val h = hash(targets)
        // Local entries pin to lastUpdateTime to detect silent reinstalls on the same versionCode
        // (WaEnhancer's UnobfuscatorCache invalidation pattern). Bundled entries skip this check.
        val match =
            entries.any {
                it.agsaVersionCode == agsaVersionCode &&
                    it.moduleVersionCode == moduleVersionCode &&
                    it.targetsHash == h &&
                    (it.agsaLastUpdateTime == 0L || it.agsaLastUpdateTime == agsaLastUpdateTime)
            }
        return if (match) RegistryStatus.Verified else RegistryStatus.Unverified
    }
}
