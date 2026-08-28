package eu.hxreborn.discoveradsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class NewsRulesMergeTest {
    private fun rule(
        id: String,
        pattern: String,
        enabled: Boolean = true,
    ) = NewsRule(id = id, pattern = pattern, enabled = enabled)

    @Test
    fun `new rules are appended and counted`() {
        val result = NewsRules.merge(listOf(rule("a", "one")), listOf(rule("b", "two")))
        assertEquals(2, result.rules.size)
        assertEquals(1, result.added)
        assertEquals(0, result.updated)
    }

    @Test
    fun `importing the same file twice changes nothing`() {
        val pack = listOf(rule("a", "one"), rule("b", "two"))
        val once = NewsRules.merge(emptyList(), pack)
        val twice = NewsRules.merge(once.rules, pack)
        assertEquals(once.rules, twice.rules)
        assertEquals(0, twice.added)
        assertEquals(0, twice.updated)
    }

    @Test
    fun `same id with different content updates in place`() {
        val result =
            NewsRules.merge(
                listOf(rule("a", "one")),
                listOf(rule("a", "one", enabled = false)),
            )
        assertEquals(1, result.rules.size)
        assertEquals(0, result.added)
        assertEquals(1, result.updated)
    }

    @Test
    fun `same content under a different id is skipped`() {
        val result = NewsRules.merge(listOf(rule("a", "One ")), listOf(rule("b", "one")))
        assertEquals(1, result.rules.size)
        assertEquals(0, result.added)
    }

    @Test
    fun `blank patterns are skipped`() {
        val result = NewsRules.merge(emptyList(), listOf(rule("a", "  "), rule("b", "real")))
        assertEquals(1, result.rules.size)
        assertEquals(1, result.added)
    }

    @Test
    fun `duplicates inside the incoming file collapse to one`() {
        val result = NewsRules.merge(emptyList(), listOf(rule("a", "one"), rule("b", "ONE")))
        assertEquals(1, result.rules.size)
        assertEquals(1, result.added)
    }
}
