package eu.hxreborn.discoveradsfilter.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class RuleAction { Block, Allow }

@Serializable
enum class RuleScope { Headline, Source, Any }

@Serializable
enum class RuleMatch { Contains, Regex }

@Serializable
data class NewsRule(
    val id: String,
    val pattern: String,
    val action: RuleAction = RuleAction.Block,
    val scope: RuleScope = RuleScope.Headline,
    val match: RuleMatch = RuleMatch.Contains,
    val enabled: Boolean = true,
    val label: String? = null,
)

data class CardText(
    val headline: String?,
    val source: String?,
) {
    val isEmpty: Boolean = headline == null && source == null
}

object NewsRules {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun defaults(): List<NewsRule> =
        listOf(
            preset(
                "ai-hype",
                "AI just killed X",
                """(chatgpt|claude|gemini|copilot|openai)\b.{0,40}\b(just killed|killed|is dead|is over|replaces)""",
                RuleMatch.Regex,
            ),
            preset(
                "declared-dead",
                "X is dead",
                """\b(java|python|seo|email|passwords?|the web|blogging|google search) is dead\b""",
                RuleMatch.Regex,
            ),
            preset(
                "listicle",
                "Listicles",
                """^\d+ (things|reasons|ways|signs|facts|tricks|habits|foods)\b""",
                RuleMatch.Regex,
            ),
            preset(
                "experts-say",
                "Experts say",
                """\b(experts|scientists|doctors|nutritionists)\b.{0,25}\b(say|reveal|warn|agree|stunned|baffled)\b""",
                RuleMatch.Regex,
            ),
            preset(
                "curiosity-gap",
                "What happened next",
                """(what happened next|the reason will surprise you|and it'?s not what you think|nobody saw it coming)""",
                RuleMatch.Regex,
            ),
            preset(
                "farewell",
                "Goodbye to X",
                """^(goodbye|farewell|say goodbye) to\b""",
                RuleMatch.Regex,
            ),
            preset(
                "official-claim",
                "Officially confirmed",
                """\b(it'?s official|officially confirmed|confirmed by the (eu|government))\b""",
                RuleMatch.Regex,
            ),
            preset(
                "secret-trick",
                "Secret trick",
                """\b(secret|hidden) (feature|trick|setting|reason|menu)\b""",
                RuleMatch.Regex,
            ),
            preset(
                "doing-it-wrong",
                "You have been doing it wrong",
                """\byou'?(ve|( have)) been .{0,25} wrong\b""",
                RuleMatch.Regex,
            ),
            preset(
                "urgent-prefix",
                "Urgent prefixes",
                """^(warning|attention|urgent|alert|breaking)[:!]""",
                RuleMatch.Regex,
            ),
            preset(
                "money-bait",
                "Money in the headline",
                """([€£] ?\d{2,}|\b(\d{2,}|thousands of) (euros|dollars|pounds)\b)""",
                RuleMatch.Regex,
            ),
            preset(
                "tabloid",
                "Tabloid drama",
                """\b(breaks? (his|her|their) silence|opens up about|slams|sparks concern|fans are convinced)\b""",
                RuleMatch.Regex,
            ),
            preset(
                "viral-bait",
                "Goes viral",
                """\b(goes viral|breaks the internet|leaves everyone speechless|the internet is losing it)\b""",
                RuleMatch.Regex,
            ),
            preset("wont-believe", "You won't believe", "you won't believe", RuleMatch.Contains),
            preset(
                "hype-words",
                "Hype words",
                """\b(game[- ]?changer|mind[- ]?blowing|shocking truth|this one trick|jaw[- ]?dropping)\b""",
                RuleMatch.Regex,
                RuleScope.Any,
            ),
            preset(
                "content-farm",
                "Hide a source",
                "tododisca",
                RuleMatch.Contains,
                RuleScope.Source,
            ),
            preset(
                "keep-source",
                "Keep a source",
                "arstechnica.com",
                RuleMatch.Contains,
                RuleScope.Source,
                RuleAction.Allow,
            ),
        )

    private fun preset(
        id: String,
        label: String,
        pattern: String,
        match: RuleMatch,
        scope: RuleScope = RuleScope.Headline,
        action: RuleAction = RuleAction.Block,
    ) = NewsRule(
        id = "preset-$id",
        pattern = pattern,
        action = action,
        scope = scope,
        match = match,
        enabled = false,
        label = label,
    )

    fun merge(
        existing: List<NewsRule>,
        incoming: List<NewsRule>,
    ): List<NewsRule> {
        val byId = existing.associateByTo(LinkedHashMap()) { it.id }
        incoming.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    fun encode(rules: List<NewsRule>): String = json.encodeToString(serializer, rules)

    fun decode(raw: String?): List<NewsRule> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private val serializer = kotlinx.serialization.builtins.ListSerializer(NewsRule.serializer())
}

class CompiledRule(
    val rule: NewsRule,
) {
    private val regex: Regex? =
        if (rule.match == RuleMatch.Regex) {
            runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE) }.getOrNull()
        } else {
            null
        }

    val usable: Boolean =
        rule.pattern.isNotBlank() && (rule.match != RuleMatch.Regex || regex != null)

    fun matches(card: CardText): Boolean {
        if (!usable) return false
        return fields(card).any(::matchesField)
    }

    private fun fields(card: CardText): List<String> =
        when (rule.scope) {
            RuleScope.Headline -> listOfNotNull(card.headline)
            RuleScope.Source -> listOfNotNull(card.source)
            RuleScope.Any -> listOfNotNull(card.headline, card.source)
        }

    private fun matchesField(value: String): Boolean =
        when (rule.match) {
            RuleMatch.Contains -> value.contains(rule.pattern, ignoreCase = true)
            RuleMatch.Regex -> regex?.containsMatchIn(value) == true
        }
}

object NewsFilter {
    fun compile(rules: List<NewsRule>): List<CompiledRule> =
        rules.filter { it.enabled }.map(::CompiledRule).filter { it.usable }

    fun shouldHide(
        compiled: List<CompiledRule>,
        card: CardText,
    ): Boolean {
        if (compiled.isEmpty()) return false
        var blocked = false
        for (rule in compiled) {
            if (!rule.matches(card)) continue
            if (rule.rule.action == RuleAction.Allow) return false
            blocked = true
        }
        return blocked
    }
}
