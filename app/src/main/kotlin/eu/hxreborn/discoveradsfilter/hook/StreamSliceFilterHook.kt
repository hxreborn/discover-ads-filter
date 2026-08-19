package eu.hxreborn.discoveradsfilter.hook

import eu.hxreborn.discoveradsfilter.Logger
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object StreamSliceFilterHook {
    private const val CONTENT_ID_FIELD = "f122746b"

    private val adClusterTokens = setOf("feedads")

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        targets: ResolvedTargets.Resolved,
    ) {
        val method = targets.streamRenderableListMethod.resolve(classLoader)
        val interceptor = StreamListInterceptor()
        module
            .hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain -> interceptor.intercept(chain) }
    }

    private class StreamListInterceptor {
        private val decisionCache = ConcurrentHashMap<String, Boolean>()
        private val contentIdFieldCache = ConcurrentHashMap<Class<*>, Field>()
        private val noContentIdClasses = ConcurrentHashMap.newKeySet<Class<*>>()
        private val stringFieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()

        @Volatile
        private var lastFingerprint: Long = Long.MIN_VALUE

        @Volatile
        private var lastFilteredSnapshot: List<Any?>? = null

        @Volatile
        private var keysDumped = false

        fun intercept(chain: XposedInterface.Chain): Any? {
            val result = chain.proceed()
            val items = result as? List<*> ?: return result
            if (items.isEmpty()) return result

            val fingerprint = fingerprint(items)
            if (fingerprint == lastFingerprint) return lastFilteredSnapshot ?: result

            dumpKeysOnce(items)

            val filtered = filter(items)
            lastFilteredSnapshot = filtered
            lastFingerprint = fingerprint
            return filtered ?: result
        }

        private fun filter(items: List<*>): List<Any?>? {
            val filtered = ArrayList<Any?>(items.size)
            for (item in items) {
                val key = item?.let(::itemKey)
                if (key != null && isAd(key)) {
                    Logger.debug { "blocked ad key=$key" }
                } else {
                    filtered += item
                }
            }
            return filtered.takeIf { it.size != items.size }
        }

        private fun isAd(key: String): Boolean =
            decisionCache.getOrPut(key) {
                val lower = key.lowercase(Locale.ROOT)
                adClusterTokens.any { it in lower }
            }

        private fun itemKey(item: Any): String? {
            val itemClass = item.javaClass

            contentId(item, itemClass)?.let { return it }

            val fields = stringFieldsCache.getOrPut(itemClass) { stringFields(itemClass) }
            val value =
                fields.firstNotNullOfOrNull { field ->
                    runCatching { field.get(item) as? String }
                        .getOrNull()
                        ?.takeIf(String::isNotBlank)
                }
            return value?.let { "${itemClass.simpleName}#$it" }
        }

        private fun contentId(
            item: Any,
            itemClass: Class<*>,
        ): String? {
            if (itemClass in noContentIdClasses) return null
            val field =
                contentIdFieldCache[itemClass] ?: run {
                    val found = contentIdField(itemClass)
                    if (found == null) {
                        noContentIdClasses.add(itemClass)
                        return null
                    }
                    contentIdFieldCache[itemClass] = found
                    found
                }
            return runCatching { field.get(item) as? String }
                .getOrNull()
                ?.takeIf(String::isNotBlank)
        }

        private fun contentIdField(start: Class<*>): Field? =
            hierarchy(start)
                .mapNotNull { itemClass ->
                    itemClass.declaredFields.find { it.name == CONTENT_ID_FIELD }
                }.firstOrNull()
                ?.apply { isAccessible = true }

        private fun stringFields(itemClass: Class<*>): List<Field> =
            hierarchy(itemClass)
                .flatMap { it.declaredFields.asSequence() }
                .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                .filter { it.type == String::class.java }
                .filter { runCatching { it.isAccessible = true }.isSuccess }
                .toList()

        private fun hierarchy(start: Class<*>): Sequence<Class<*>> =
            generateSequence(start) {
                it.superclass?.takeIf { parent ->
                    parent != Any::class.java
                }
            }

        private fun fingerprint(items: List<*>): Long {
            val step = (items.size / 3).coerceAtLeast(1)
            return (0 until items.size step step)
                .take(4)
                .fold(items.size.toLong()) { accumulator, index ->
                    accumulator * 31L + System.identityHashCode(items[index])
                }
        }

        private fun dumpKeysOnce(items: List<*>) {
            if (keysDumped) return
            keysDumped = true
            Logger.debug {
                items
                    .mapIndexed { index, item ->
                        val name = item?.javaClass?.simpleName ?: "null"
                        "  [$index] $name → ${item?.let(::itemKey) ?: "<no-key>"}"
                    }.joinToString("\n", prefix = "item key dump (${items.size} items):\n")
            }
        }
    }
}
