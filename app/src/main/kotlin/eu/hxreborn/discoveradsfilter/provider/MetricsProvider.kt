package eu.hxreborn.discoveradsfilter.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs

class MetricsProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "eu.hxreborn.discoveradsfilter.metrics"
        const val METHOD_INCREMENT = "increment_ads"
        const val METHOD_READ = "read_ads"
        const val METHOD_REPORT_HOOK_STATUS = "report_hook_status"
        const val KEY_COUNT = "count"
        const val KEY_TOTAL = "total"
        const val KEY_STATUS = "status"
        const val KEY_PROCESS = "process"
    }

    private val lock = Any()

    private val prefs by lazy {
        requireNotNull(context).getSharedPreferences(SettingsPrefs.GROUP, Context.MODE_PRIVATE)
    }

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle? {
        if (callingPackage != "com.google.android.googlequicksearchbox") return null
        return when (method) {
            METHOD_INCREMENT -> {
                val delta = extras?.getInt(KEY_COUNT, 0) ?: 0
                if (delta <= 0) return null
                synchronized(lock) {
                    val updated = SettingsPrefs.adsHidden.read(prefs) + delta
                    prefs.edit(commit = true) {
                        SettingsPrefs.adsHidden.write(this, updated)
                    }
                    Bundle().apply { putLong(KEY_TOTAL, updated) }
                }
            }

            METHOD_READ -> {
                Bundle().apply {
                    putLong(KEY_TOTAL, SettingsPrefs.adsHidden.read(prefs))
                }
            }

            METHOD_REPORT_HOOK_STATUS -> {
                val status = extras?.getString(KEY_STATUS)
                val process = extras?.getString(KEY_PROCESS)
                if (status != null) {
                    prefs.edit(commit = true) {
                        SettingsPrefs.hookStatus.write(this, status)
                        if (process != null) SettingsPrefs.hookProcess.write(this, process)
                    }
                }
                null
            }

            else -> {
                null
            }
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        u: Uri,
        p: Array<String>?,
        s: String?,
        a: Array<String>?,
        o: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0
}
