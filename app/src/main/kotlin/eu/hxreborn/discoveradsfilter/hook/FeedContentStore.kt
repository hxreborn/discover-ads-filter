package eu.hxreborn.discoveradsfilter.hook

import android.database.sqlite.SQLiteDatabase
import eu.hxreborn.discoveradsfilter.filter.CardBlob
import eu.hxreborn.discoveradsfilter.filter.CardText
import eu.hxreborn.discoveradsfilter.util.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object FeedContentStore {
    const val CARD_PREFIX = "CARD::"

    private const val DB_NAME = "name_value_stream_store.db"
    private const val QUERY = "SELECT value FROM feed_content_table WHERE name = ? LIMIT 1"
    private const val MAX_CACHED_CARDS = 600

    private val cache = ConcurrentHashMap<String, CardText>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var stores: List<File> = emptyList()

    fun init(filesDir: File) {
        stores =
            runCatching {
                File(filesDir, "accounts")
                    .listFiles()
                    ?.filter(File::isDirectory)
                    ?.map { File(it, DB_NAME) }
                    ?.filter(File::isFile)
                    .orEmpty()
            }.getOrDefault(emptyList())
    }

    fun reset() {
        cache.clear()
        misses.clear()
    }

    fun cardText(name: String): CardText? {
        cache[name]?.let { return it }
        if (name in misses) return null
        val card = readBlob(name)?.let(CardBlob::parse)?.takeIf { !it.isEmpty }
        if (card == null) {
            misses += name
            return null
        }
        if (cache.size > MAX_CACHED_CARDS) cache.clear()
        cache[name] = card
        return card
    }

    private fun readBlob(name: String): ByteArray? {
        for (store in stores) {
            queryBlob(store, name)?.let { return it }
        }
        return null
    }

    private fun queryBlob(
        file: File,
        name: String,
    ): ByteArray? =
        runCatching {
            SQLiteDatabase
                .openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
                .use { db ->
                    db.rawQuery(QUERY, arrayOf(name)).use { cursor ->
                        if (cursor.moveToFirst()) cursor.getBlob(0) else null
                    }
                }
        }.onFailure {
            Logger.debug { "feed store read failed: ${it.message}" }
        }.getOrNull()
}
