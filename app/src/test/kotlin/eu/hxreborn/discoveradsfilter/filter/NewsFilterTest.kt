package eu.hxreborn.discoveradsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsFilterTest {
    private val card =
        CardText(
            headline = "Confirmado por la UE: el fin de las bombillas halógenas",
            source = "infobae.com",
        )

    private fun hidden(vararg rules: NewsRule) =
        NewsFilter.shouldHide(NewsFilter.compile(rules.toList()), card)

    private fun rule(
        pattern: String,
        action: RuleAction = RuleAction.Block,
        scope: RuleScope = RuleScope.Headline,
        match: RuleMatch = RuleMatch.Contains,
        enabled: Boolean = true,
    ) = NewsRule("id-$pattern-$scope-$action", pattern, action, scope, match, enabled)

    @Test
    fun `no rules keeps everything`() {
        assertFalse(NewsFilter.shouldHide(emptyList(), card))
    }

    @Test
    fun `a headline substring blocks the card`() {
        assertTrue(hidden(rule("confirmado por la UE")))
    }

    @Test
    fun `matching is case insensitive`() {
        assertTrue(hidden(rule("CONFIRMADO POR LA ue")))
    }

    @Test
    fun `a headline rule does not match the source`() {
        assertFalse(hidden(rule("infobae")))
    }

    @Test
    fun `a source rule matches the host without www`() {
        assertTrue(hidden(rule("infobae.com", scope = RuleScope.Source)))
    }

    @Test
    fun `an any rule matches either field`() {
        assertTrue(hidden(rule("infobae", scope = RuleScope.Any)))
        assertTrue(hidden(rule("halógenas", scope = RuleScope.Any)))
    }

    @Test
    fun `a regex rule matches the headline`() {
        assertTrue(hidden(rule("""confirmado (por|desde) la ue""", match = RuleMatch.Regex)))
    }

    @Test
    fun `a regex rule that does not match keeps the card`() {
        assertFalse(hidden(rule("""^desmentido""", match = RuleMatch.Regex)))
    }

    @Test
    fun `a malformed regex never matches and never throws`() {
        val bad = rule("""confirmado ([a-z""", match = RuleMatch.Regex)

        assertFalse(CompiledRule(bad).usable)
        assertFalse(hidden(bad))
    }

    @Test
    fun `a blank pattern is inert`() {
        assertFalse(CompiledRule(rule("   ")).usable)
        assertFalse(hidden(rule("   ")))
    }

    @Test
    fun `a disabled rule is ignored`() {
        assertFalse(hidden(rule("confirmado", enabled = false)))
    }

    @Test
    fun `an allow rule overrides a blocking rule`() {
        assertFalse(hidden(rule("confirmado"), rule("bombillas", action = RuleAction.Allow)))
    }

    @Test
    fun `allow wins regardless of rule order`() {
        assertFalse(hidden(rule("bombillas", action = RuleAction.Allow), rule("confirmado")))
    }

    @Test
    fun `an allow rule alone does not hide unrelated cards`() {
        assertFalse(hidden(rule("nothing here", action = RuleAction.Allow)))
    }

    @Test
    fun `a disabled allow rule stops overriding`() {
        assertTrue(
            hidden(
                rule("confirmado"),
                rule("bombillas", action = RuleAction.Allow, enabled = false),
            ),
        )
    }

    @Test
    fun `a card with no text is never hidden`() {
        val empty = CardText(headline = null, source = null)

        assertFalse(NewsFilter.shouldHide(NewsFilter.compile(listOf(rule("confirmado"))), empty))
    }

    @Test
    fun `rules survive a json round trip`() {
        val rules =
            listOf(
                rule("confirmado por la UE"),
                rule("infobae.com", action = RuleAction.Allow, scope = RuleScope.Source),
                rule(
                    """^\d+ (cosas|razones)""",
                    scope = RuleScope.Any,
                    match = RuleMatch.Regex,
                    enabled = false,
                ),
            )

        assertEquals(rules, NewsRules.decode(NewsRules.encode(rules)))
    }

    @Test
    fun `every preset compiles and ships disabled`() {
        val defaults = NewsRules.defaults()

        assertTrue(defaults.isNotEmpty())
        defaults.forEach { rule ->
            assertTrue("${rule.id} is enabled", !rule.enabled)
            assertTrue(
                "${rule.id} does not compile",
                CompiledRule(rule.copy(enabled = true)).usable,
            )
        }
    }

    @Test
    fun `preset ids are unique and stable`() {
        val ids = NewsRules.defaults().map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("preset-") })
    }

    @Test
    fun `presets hide clickbait once enabled`() {
        val enabled = NewsFilter.compile(NewsRules.defaults().map { it.copy(enabled = true) })
        val clickbait =
            listOf(
                "ChatGPT just killed the search engine" to "example.com",
                "10 things you should never do on a plane" to "example.com",
                "Experts warn about this common kitchen mistake" to "example.com",
                "You won't believe what happened next" to "example.com",
                "Say goodbye to slow WiFi" to "example.com",
                "The hidden setting that doubles your battery" to "example.com",
            )

        clickbait.forEach { (headline, source) ->
            assertTrue(headline, NewsFilter.shouldHide(enabled, CardText(headline, source)))
        }
    }

    @Test
    fun `presets leave ordinary headlines alone`() {
        val enabled = NewsFilter.compile(NewsRules.defaults().map { it.copy(enabled = true) })
        val ordinary =
            listOf(
                "Council approves the new tram line through the old town",
                "Bank of England holds interest rates at 4 percent",
                "Zaragoza beats Tarragona 2-1 in the league opener",
            )

        ordinary.forEach { headline ->
            assertFalse(headline, NewsFilter.shouldHide(enabled, CardText(headline, "example.com")))
        }
    }

    @Test
    fun `unreadable stored rules decode to an empty list`() {
        assertEquals(emptyList<NewsRule>(), NewsRules.decode("not json"))
        assertEquals(emptyList<NewsRule>(), NewsRules.decode(null))
        assertEquals(emptyList<NewsRule>(), NewsRules.decode(""))
    }

    @Test
    fun `merge keeps existing rules and lets imported ones win by id`() {
        val existing =
            listOf(
                NewsRule(id = "a", pattern = "old", label = "mine"),
                NewsRule(id = "b", pattern = "keep"),
            )
        val imported =
            listOf(
                NewsRule(id = "a", pattern = "new", label = "theirs"),
                NewsRule(id = "c", pattern = "fresh"),
            )

        val merged = NewsRules.merge(existing, imported).rules

        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
        assertEquals("new", merged.first { it.id == "a" }.pattern)
        assertEquals("keep", merged.first { it.id == "b" }.pattern)
    }

    @Test
    fun `exported rules survive a decode round trip`() {
        val rules = NewsRules.defaults()

        val restored = NewsRules.decode(NewsRules.encode(rules))

        assertEquals(rules, restored)
    }
}
