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
    fun `unreadable stored rules decode to an empty list`() {
        assertEquals(emptyList<NewsRule>(), NewsRules.decode("not json"))
        assertEquals(emptyList<NewsRule>(), NewsRules.decode(null))
        assertEquals(emptyList<NewsRule>(), NewsRules.decode(""))
    }
}
