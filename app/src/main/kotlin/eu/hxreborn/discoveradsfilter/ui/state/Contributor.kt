package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val DIFF_BLOCK_COUNT = 5

@Serializable
data class ContributorEntry(
    val login: String,
    val added: Int,
    val removed: Int,
    val url: String,
    val avatar: String,
    val owner: Boolean = false,
)

@Immutable
data class Contributor(
    val login: String,
    val added: Int,
    val removed: Int,
    val url: String,
    val avatar: ImageBitmap?,
    val isOwner: Boolean,
)

data class DiffBlocks(
    val added: Int,
    val removed: Int,
) {
    val neutral: Int get() = DIFF_BLOCK_COUNT - added - removed
}

fun diffBlocks(
    added: Int,
    removed: Int,
): DiffBlocks {
    val total = added.toLong() + removed.toLong()
    if (total == 0L) return DiffBlocks(added = 0, removed = 0)
    return DiffBlocks(
        added = (DIFF_BLOCK_COUNT * added.toLong() / total).toInt(),
        removed = (DIFF_BLOCK_COUNT * removed.toLong() / total).toInt(),
    )
}

private val snapshotJson = Json { ignoreUnknownKeys = true }

fun parseContributorEntries(json: String): List<ContributorEntry> = snapshotJson.decodeFromString(json)
