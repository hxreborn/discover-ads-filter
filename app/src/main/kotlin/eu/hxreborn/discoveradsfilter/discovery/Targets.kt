package eu.hxreborn.discoveradsfilter.discovery

import kotlinx.serialization.Serializable
import java.lang.reflect.Method

@Serializable
sealed interface ResolvedTargets {
    fun summary(): String

    @Serializable
    data class Resolved(
        val adMetadataClass: String? = null,
        val feedCardClass: String? = null,
        val adFlagFieldName: String? = null,
        val adLabelFieldName: String? = null,
        val adMetadataFieldName: String? = null,
        val cardProcessorMethods: List<MethodRef> = emptyList(),
        val streamRenderableListMethod: MethodRef? = null,
    ) : ResolvedTargets {
        override fun summary(): String =
            buildString {
                append("Resolved(")
                append("adMeta=${adMetadataClass?.substringAfterLast('.') ?: "–"}, ")
                append("card=${feedCardClass?.substringAfterLast('.') ?: "–"}, ")
                append("processors=${cardProcessorMethods.size}, ")
                append(
                    "stream=${
                        streamRenderableListMethod?.className?.substringAfterLast(
                            '.',
                        ) ?: "–"
                    })",
                )
            }
    }

    @Serializable
    data class Missing(
        val reason: String,
    ) : ResolvedTargets {
        override fun summary(): String = "Missing($reason)"
    }
}

@Serializable
data class MethodRef(
    val className: String,
    val methodName: String,
    val paramTypeNames: List<String>,
) {
    fun resolve(loader: ClassLoader): Method {
        val clazz = loader.loadClass(className)
        val paramTypes = paramTypeNames.map { typeFor(loader, it) }.toTypedArray()
        return try {
            clazz.getDeclaredMethod(methodName, *paramTypes)
        } catch (_: NoSuchMethodException) {
            clazz.getMethod(methodName, *paramTypes)
        }.also { it.isAccessible = true }
    }

    override fun toString(): String = "$className.$methodName(${paramTypeNames.joinToString(",")})"

    @Suppress("RemoveRedundantQualifierName") // clearer for JVM reflection
    private fun typeFor(
        loader: ClassLoader,
        name: String,
    ): Class<*> {
        // Peel array dimensions first: DexKit may return "int[]", "java.lang.String[][]", etc.
        var base = name
        var rank = 0
        while (base.endsWith("[]")) {
            base = base.removeSuffix("[]")
            rank++
        }
        val componentType =
            when (base) {
                "boolean" -> java.lang.Boolean.TYPE
                "byte" -> java.lang.Byte.TYPE
                "char" -> java.lang.Character.TYPE
                "short" -> java.lang.Short.TYPE
                "int" -> java.lang.Integer.TYPE
                "long" -> java.lang.Long.TYPE
                "float" -> java.lang.Float.TYPE
                "double" -> java.lang.Double.TYPE
                "void" -> java.lang.Void.TYPE
                else -> loader.loadClass(base)
            }
        if (rank == 0) return componentType
        val descriptor =
            buildString {
                repeat(rank) { append('[') }
                append(primitiveDescriptor(componentType) ?: "L${componentType.name};")
            }
        return Class.forName(descriptor, false, loader)
    }

    @Suppress("RemoveRedundantQualifierName") // clearer for JVM reflection
    private fun primitiveDescriptor(c: Class<*>): String? =
        when (c) {
            java.lang.Boolean.TYPE -> "Z"
            java.lang.Byte.TYPE -> "B"
            java.lang.Character.TYPE -> "C"
            java.lang.Short.TYPE -> "S"
            java.lang.Integer.TYPE -> "I"
            java.lang.Long.TYPE -> "J"
            java.lang.Float.TYPE -> "F"
            java.lang.Double.TYPE -> "D"
            java.lang.Void.TYPE -> "V"
            else -> null
        }
}
