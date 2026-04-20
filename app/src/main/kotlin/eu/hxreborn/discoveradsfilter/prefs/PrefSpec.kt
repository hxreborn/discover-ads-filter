package eu.hxreborn.discoveradsfilter.prefs

import android.content.SharedPreferences

sealed class PrefSpec<T>(
    val key: String,
    val default: T,
) {
    abstract fun read(prefs: SharedPreferences): T

    abstract fun write(
        editor: SharedPreferences.Editor,
        value: T,
    )
}

class BoolPref(
    key: String,
    default: Boolean,
) : PrefSpec<Boolean>(key, default) {
    override fun read(prefs: SharedPreferences): Boolean = prefs.getBoolean(key, default)

    override fun write(
        editor: SharedPreferences.Editor,
        value: Boolean,
    ) {
        editor.putBoolean(key, value)
    }
}

class IntPref(
    key: String,
    default: Int,
) : PrefSpec<Int>(key, default) {
    override fun read(prefs: SharedPreferences): Int = prefs.getInt(key, default)

    override fun write(
        editor: SharedPreferences.Editor,
        value: Int,
    ) {
        editor.putInt(key, value)
    }
}

class LongPref(
    key: String,
    default: Long,
) : PrefSpec<Long>(key, default) {
    override fun read(prefs: SharedPreferences): Long = prefs.getLong(key, default)

    override fun write(
        editor: SharedPreferences.Editor,
        value: Long,
    ) {
        editor.putLong(key, value)
    }
}

class NullableStringPref(
    key: String,
) : PrefSpec<String?>(key, null) {
    override fun read(prefs: SharedPreferences): String? = prefs.getString(key, null)

    override fun write(
        editor: SharedPreferences.Editor,
        value: String?,
    ) {
        editor.putString(key, value)
    }
}
