package eu.hxreborn.discoveradsfilter.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import eu.hxreborn.discoveradsfilter.App
import eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule
import eu.hxreborn.discoveradsfilter.prefs.SettingsPrefs

class MetricsProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "eu.hxreborn.discoveradsfilter.metrics"
        const val METHOD_INCREMENT = "increment_ads"
        const val KEY_COUNT = "count"
        const val METHOD_REQUEST_RECOVERY = "request_recovery"
        const val KEY_VERSION = "version"
        const val KEY_FIRST = "first"
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
        if (callingPackage != DiscoverAdsFilterModule.AGSA_PKG) return null
        when (method) {
            METHOD_INCREMENT -> {
                val delta = extras?.getInt(KEY_COUNT, 0) ?: 0
                if (delta > 0) {
                    synchronized(lock) {
                        val updated = SettingsPrefs.adsHidden.read(prefs) + delta
                        prefs.edit(commit = true) { SettingsPrefs.adsHidden.write(this, updated) }
                    }
                }
            }

            METHOD_REQUEST_RECOVERY -> {
                val version = extras?.getLong(KEY_VERSION, 0L) ?: 0L
                val first =
                    (context?.applicationContext as? App)?.onRecoveryRequested(version) ?: false
                return Bundle().apply { putBoolean(KEY_FIRST, first) }
            }
        }
        return null
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
