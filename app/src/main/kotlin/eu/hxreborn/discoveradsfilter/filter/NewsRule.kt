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

    // An Allow rule is an exception to the block list, never an exclusive allow list.
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
