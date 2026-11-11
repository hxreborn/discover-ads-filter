package eu.hxreborn.discoveradsfilter.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributorTest {
    @Test
    fun fiveAddedOneRemovedDrawsFourAddedZeroRemovedOneNeutral() {
        val blocks = diffBlocks(added = 5, removed = 1)
        assertEquals(4, blocks.added)
        assertEquals(0, blocks.removed)
        assertEquals(1, blocks.neutral)
    }

    @Test
    fun evenSplitDrawsTwoAddedTwoRemovedOneNeutral() {
        val blocks = diffBlocks(added = 100, removed = 100)
        assertEquals(2, blocks.added)
        assertEquals(2, blocks.removed)
        assertEquals(1, blocks.neutral)
    }

    @Test
    fun onlyAddedDrawsFiveAdded() {
        val blocks = diffBlocks(added = 154, removed = 0)
        assertEquals(5, blocks.added)
        assertEquals(0, blocks.removed)
        assertEquals(0, blocks.neutral)
    }

    @Test
    fun noChangedLinesDrawsFiveNeutral() {
        assertEquals(5, diffBlocks(added = 0, removed = 0).neutral)
    }

    @Test
    fun blockCountIgnoresVolume() {
        assertEquals(diffBlocks(added = 9, removed = 1), diffBlocks(added = 900_000, removed = 100_000))
    }

    @Test
    fun parsesSnapshotWithOwnerFlagOmittedForOthers() {
        val entries =
            parseContributorEntries(
                """
                [
                  {"login": "owner", "added": 10, "removed": 2, "url": "https://github.com/owner", "avatar": "avatars/owner.webp", "owner": true},
                  {"login": "other", "added": 3, "removed": 0, "url": "https://github.com/other", "avatar": "avatars/other.webp"}
                ]
                """.trimIndent(),
            )
        assertEquals(listOf("owner", "other"), entries.map { it.login })
        assertTrue(entries[0].owner)
        assertFalse(entries[1].owner)
    }
}
