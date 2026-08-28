package eu.hxreborn.discoveradsfilter.util

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

object ObjectStrings {
    private val fieldCache = ConcurrentHashMap<Class<*>, List<Field>>()

    fun collect(
        roots: List<Any?>,
        maxDepth: Int,
        maxNodes: Int,
        prefix: String? = null,
    ): List<String> {
        val found = LinkedHashSet<String>()
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val queue = ArrayDeque<Pair<Any, Int>>()
        roots.filterNotNull().forEach { queue += it to 0 }
        var nodes = 0
        while (queue.isNotEmpty() && nodes < maxNodes) {
            val (value, depth) = queue.removeFirst()
            if (!seen.add(value)) continue
            nodes++
            when {
                value is String -> {
                    if (prefix == null || value.startsWith(prefix)) found += value
                    continue
                }

                value is CharSequence -> {
                    val text = value.toString()
                    if (prefix == null || text.startsWith(prefix)) found += text
                    continue
                }

                depth >= maxDepth -> {
                    continue
                }

                value is Number || value is Boolean || value is Char -> {
                    continue
                }
            }
            if (value is Iterable<*>) {
                value.forEach { child -> child?.let { queue += it to depth + 1 } }
                continue
            }
            if (value is Map<*, *>) {
                value.values.forEach { child -> child?.let { queue += it to depth + 1 } }
                continue
            }
            if (value is Array<*>) {
                value.forEach { child -> child?.let { queue += it to depth + 1 } }
                continue
            }
            val cls = value.javaClass
            if (cls.isArray || isRuntimeClass(cls)) continue
            fieldCache.computeIfAbsent(cls, ::declaredFields).forEach { field ->
                runCatching { field.get(value) }.getOrNull()?.let { queue += it to depth + 1 }
            }
        }
        return found.toList()
    }

    private fun isRuntimeClass(cls: Class<*>): Boolean =
        cls.name.startsWith("java.") ||
            cls.name.startsWith("kotlin.") ||
            cls.name.startsWith("android.") ||
            cls.name.startsWith("androidx.")

    private fun declaredFields(cls: Class<*>): List<Field> =
        generateSequence(cls) { it.superclass.takeIf { c -> c != Any::class.java } }
            .flatMap { it.declaredFields.asSequence() }
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .filter { runCatching { it.isAccessible = true }.isSuccess }
            .toList()
}
