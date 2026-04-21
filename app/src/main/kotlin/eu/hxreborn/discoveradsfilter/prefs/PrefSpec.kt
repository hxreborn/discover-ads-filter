package eu.hxreborn.discoveradsfilter.prefs

import android.content.SharedPreferences

class PrefSpec<T>(
    val key: String,
    val default: T,
    private val get: SharedPreferences.(String, T) -> T,
    private val put: SharedPreferences.Editor.(String, T) -> Unit,
) {
    fun read(prefs: SharedPreferences): T = prefs.get(key, default)

    fun write(
        editor: SharedPreferences.Editor,
        value: T,
    ) {
        editor.put(key, value)
    }
}

fun boolPref(
    key: String,
    default: Boolean,
) = PrefSpec(key, default, SharedPreferences::getBoolean) { k, v -> putBoolean(k, v) }

fun intPref(
    key: String,
    default: Int,
) = PrefSpec(key, default, SharedPreferences::getInt) { k, v -> putInt(k, v) }

fun longPref(
    key: String,
    default: Long,
) = PrefSpec(key, default, SharedPreferences::getLong) { k, v -> putLong(k, v) }

fun nullableStringPref(key: String) =
    PrefSpec<String?>(key, null, { k, _ -> getString(k, null) }) { k, v -> putString(k, v) }
