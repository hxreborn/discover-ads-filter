package eu.hxreborn.discoveradsfilter.hook

import android.content.SharedPreferences
import android.util.Log
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.filter.CardText
import eu.hxreborn.discoveradsfilter.filter.NewsFilter
import eu.hxreborn.discoveradsfilter.module
import eu.hxreborn.discoveradsfilter.util.Logger
import eu.hxreborn.discoveradsfilter.util.ObjectStrings
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object StreamSliceFilterHook {
    private const val CONTENT_ID_FIELD = "f122746b"
    private const val CARD_NAME_DEPTH = 4
    private const val CARD_NAME_NODES = 300
    private const val MAX_NEWS_DECISIONS = 2000

    private val adClusterTokens = setOf("feedads")

    private val decisionCache = ConcurrentHashMap<String, Boolean>()
    private val countedAdKeys = ConcurrentHashMap.newKeySet<String>()
    private val newsDecisions = ConcurrentHashMap<String, Boolean>()

    private val contentIdFieldCache = ConcurrentHashMap<Class<*>, Field>()
    private val noContentIdClasses = ConcurrentHashMap.newKeySet<Class<*>>()
    private val stringFieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()
    private val nonSliceClasses = ConcurrentHashMap.newKeySet<Class<*>>()

    private class Snapshot(
        val fingerprint: Long,
        val list: List<Any?>?,
    )

    @Volatile
    private var snapshot: Snapshot? = null

    fun install(
        loader: ClassLoader,
        prefs: SharedPreferences,
        targets: ResolvedTargets,
        processName: String,
    ): Boolean {
        val streamRef = (targets as? ResolvedTargets.Resolved)?.streamRenderableListMethod
        if (streamRef == null) {
            val reason = (targets as? ResolvedTargets.Missing)?.reason ?: "no cached targets"
            Logger.log(
                Log.WARN,
                "no stream method in cache proc=$processName reason=$reason, " +
                    "open Discover Ads Filter and tap Verify",
            )
            return false
        }
        val method =
            runCatching { streamRef.resolve(loader) }.getOrElse {
                Logger.log(Log.WARN, "failed to rehydrate $streamRef: ${it.message}")
                return false
            }
        module.hook(method).intercept(StreamListHooker)
        Logger.log(Log.INFO, "hooked $streamRef proc=$processName")
        return true
    }

    private val keysDumped = AtomicBoolean(false)

    @Volatile
    private var warnedUnreadableCard = false

    internal fun resetKeyDump() {
        keysDumped.set(false)
    }

    internal fun resetFilterState() {
        newsDecisions.clear()
        FeedContentStore.reset()
        snapshot = null
    }

    private fun dumpKeysOnce(items: List<*>) {
        if (!keysDumped.compareAndSet(false, true)) return
        Logger.debug {
            items
                .mapIndexed { i, item ->
                    val cls = item?.javaClass?.simpleName ?: "null"
                    val key = item?.let(::stableItemKey) ?: "<no-key>"
                    val card = item?.let(::cardTextFor)
                    "  [$i] $cls → $key\n" +
                        "        source=${card?.source} title=${card?.headline}"
                }.joinToString("\n", prefix = "item key dump (${items.size} items):\n")
        }
    }

    private fun buildFilteredList(items: List<*>): List<Any?>? {
        var newAds = 0
        val filtered = ArrayList<Any?>(items.size)
        for (item in items) {
            val key = item?.let(::stableItemKey)
            if (key != null && isAdItem(key)) {
                if (countedAdKeys.add(key)) {
                    newAds++
                    Logger.debug { "blocked ad key=$key" }
                }
                continue
            }
            if (item != null && key != null && hiddenByRules(item, key)) continue
            filtered += item
        }
        if (filtered.size == items.size) return null
        if (newAds > 0) HookMetrics.addAdsHidden(newAds)
        Logger.debug { "filtered ${items.size} items to ${filtered.size}" }
        return filtered
    }

    private object StreamListHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val result = chain.proceed()
            val items = result as? List<*> ?: return result
            if (items.isEmpty()) return result

            val fp = fastFingerprint(items)
            snapshot?.takeIf { it.fingerprint == fp }?.let { return it.list ?: result }

            dumpKeysOnce(items)

            val filtered = buildFilteredList(items)
            snapshot = Snapshot(fp, filtered)
            return filtered ?: result
        }
    }

    private fun cardTextFor(item: Any): CardText? {
        val names =
            ObjectStrings.collect(
                listOf(item),
                CARD_NAME_DEPTH,
                CARD_NAME_NODES,
                FeedContentStore.CARD_PREFIX,
            )
        val card = names.firstNotNullOfOrNull(FeedContentStore::cardText)
        if (card == null && names.isNotEmpty() && !warnedUnreadableCard) {
            warnedUnreadableCard = true
            Logger.log(Log.WARN, "no readable card text for $names, news rules cannot match it")
        }
        return card
    }

    private fun hiddenByRules(
        item: Any,
        key: String,
    ): Boolean {
        val rules = newsRules
        if (rules.isEmpty()) return false
        newsDecisions[key]?.let { return it }
        val card = runCatching { cardTextFor(item) }.getOrNull() ?: return false
        val hidden = NewsFilter.shouldHide(rules, card)
        if (hidden) {
            Logger.debug { "hidden news source=${card.source} title=${card.headline}" }
        }
        if (newsDecisions.size > MAX_NEWS_DECISIONS) newsDecisions.clear()
        newsDecisions[key] = hidden
        return hidden
    }

    private fun isAdItem(key: String): Boolean {
        decisionCache[key]?.let { return it }
        val lower = key.lowercase(Locale.ROOT)
        val isAd = adClusterTokens.any { it in lower }
        decisionCache[key] = isAd
        return isAd
    }

    private fun stableItemKey(item: Any): String? {
        val cls = item.javaClass
        if (cls in nonSliceClasses) return null

        contentId(item, cls)?.let { return it }

        val fields = stringFieldsCache.computeIfAbsent(cls) { resolveStringFields(cls) }
        val value =
            fields.firstNotNullOfOrNull { f ->
                runCatching { f.get(item) as? String }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        if (value == null) nonSliceClasses.add(cls)
        return value?.let { "${cls.simpleName}#$it" }
    }

    private fun contentId(
        item: Any,
        cls: Class<*>,
    ): String? {
        if (cls in noContentIdClasses) return null
        val field =
            contentIdFieldCache[cls] ?: run {
                val found = findContentIdField(cls)
                if (found == null) {
                    noContentIdClasses.add(cls)
                    return null
                }
                contentIdFieldCache[cls] = found
                found
            }
        return runCatching { field.get(item) as? String }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun findContentIdField(start: Class<*>): Field? =
        generateSequence(start) { it.superclass.takeIf { c -> c != Any::class.java } }
            .mapNotNull { cls -> cls.declaredFields.find { it.name == CONTENT_ID_FIELD } }
            .firstOrNull()
            ?.also { it.isAccessible = true }

    private fun resolveStringFields(cls: Class<*>): List<Field> =
        generateSequence(cls) { it.superclass.takeIf { c -> c != Any::class.java } }
            .flatMap { it.declaredFields.asSequence() }
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .filter { runCatching { it.isAccessible = true }.isSuccess }
            .toList()

    private fun fastFingerprint(items: List<*>): Long {
        val step = (items.size / 3).coerceAtLeast(1)
        return (0 until items.size step step)
            .take(4)
            .fold(items.size.toLong()) { acc, i ->
                acc * 31L + System.identityHashCode(items[i])
            }
    }
}
