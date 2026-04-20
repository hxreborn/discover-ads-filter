package eu.hxreborn.discoveradsfilter.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import java.util.Locale

object DexKitResolver {
    // stable across obfuscation: proto wire-format field number in fwml's <clinit>
    private const val AD_METADATA_EXTENSION_FIELD_NUMBER = 393053250L
    private const val MAX_CARD_PROCESSOR_METHODS = 120
    private const val CLINIT = "<clinit>"
    private const val TYPE_STRING = "java.lang.String"

    // DexKit 2.x requires explicit System.loadLibrary, no static initializer
    private val nativeLoaded: Boolean by lazy {
        runCatching { System.loadLibrary("dexkit") }.isSuccess
    }

    @Suppress("kotlin:S6310")
    suspend fun resolveAll(agsaApkPath: String): ResolvedTargets =
        withContext(Dispatchers.IO) {
            if (!nativeLoaded) {
                return@withContext ResolvedTargets.Missing("libdexkit.so failed to load")
            }
            runCatching {
                DexKitBridge.create(agsaApkPath).use { bridge ->
                    resolve(bridge)
                }
            }.getOrElse { t ->
                ResolvedTargets.Missing("DexKit threw ${t.javaClass.simpleName}: ${t.message}")
            }
        }

    private fun resolve(bridge: DexKitBridge): ResolvedTargets {
        val adMetaClass =
            findAdMetadataClass(bridge) ?: return ResolvedTargets.Missing(diagnostic(bridge))
        val adMetaName = adMetaClass.name

        val feedCardClass =
            findFeedCardClass(bridge, adMetaName) ?: return ResolvedTargets.Missing(
                "found ad-metadata class $adMetaName but no feed-card class referencing it",
            )
        val feedCardName = feedCardClass.name

        val adFlagField = findAdFlagField(adMetaClass)
        val adLabelField = findAdLabelField(adMetaClass)
        val adMetaField = findAdMetadataField(feedCardClass, adMetaName)

        if (adFlagField == null && adLabelField == null) {
            return ResolvedTargets.Missing(
                "found classes ($adMetaName, $feedCardName) but no ad flag/label fields",
            )
        }
        if (adMetaField == null) {
            return ResolvedTargets.Missing(
                "found classes but no field of type $adMetaName on $feedCardName",
            )
        }

        val processors = findCardProcessorMethods(bridge, feedCardName, adMetaName, adMetaField)
        val streamRenderableListMethod = findStreamRenderableListMethod(bridge)

        if (processors.isEmpty()) {
            return ResolvedTargets.Missing(
                "found proto classes and fields but no card-processor methods reading $adMetaField",
            )
        }

        return ResolvedTargets.Resolved(
            adMetadataClass = adMetaName,
            feedCardClass = feedCardName,
            adFlagFieldName = adFlagField,
            adLabelFieldName = adLabelField,
            adMetadataFieldName = adMetaField,
            cardProcessorMethods = processors,
            streamRenderableListMethod = streamRenderableListMethod,
        )
    }

    private fun findAdMetadataClass(bridge: DexKitBridge): ClassData? {
        val candidates =
            runCatching {
                bridge.findClass {
                    matcher {
                        methods {
                            add {
                                name(CLINIT)
                                usingNumbers(AD_METADATA_EXTENSION_FIELD_NUMBER)
                            }
                        }
                    }
                }
            }.getOrDefault(emptyList())

        return candidates.firstOrNull { c ->
            val fields = runCatching { c.fields }.getOrDefault(emptyList())
            val nonStatic = fields.filter { !it.isStatic }
            nonStatic.any { it.typeName == TYPE_STRING } &&
                nonStatic.any { it.typeName == "boolean" } &&
                nonStatic.any { it.typeName == "long" }
        }
    }

    private fun findFeedCardClass(
        bridge: DexKitBridge,
        adMetaName: String,
    ): ClassData? {
        val candidates =
            runCatching {
                bridge.findClass {
                    matcher {
                        fields {
                            add { type(adMetaName) }
                        }
                    }
                }
            }.getOrDefault(emptyList())

        return candidates.firstOrNull { c ->
            val fields = runCatching { c.fields }.getOrDefault(emptyList())
            val nonStatic = fields.filter { !it.isStatic }
            nonStatic.count { it.typeName == TYPE_STRING } >= 5
        }
    }

    private fun findAdFlagField(adMetaClass: ClassData): String? =
        runCatching {
            adMetaClass.fields.find { !it.isStatic && it.typeName == "boolean" }?.name
        }.getOrNull()

    private fun findAdLabelField(adMetaClass: ClassData): String? =
        runCatching {
            adMetaClass.fields.find { !it.isStatic && it.typeName == TYPE_STRING }?.name
        }.getOrNull()

    private fun findAdMetadataField(
        feedCardClass: ClassData,
        adMetaName: String,
    ): String? =
        runCatching {
            feedCardClass.fields.singleOrNull { !it.isStatic && it.typeName == adMetaName }?.name
        }.getOrNull()

    private fun findCardProcessorMethods(
        bridge: DexKitBridge,
        feedCardName: String,
        adMetaName: String,
        adMetaFieldName: String,
    ): List<MethodRef> {
        val fieldReaders =
            runCatching {
                val field =
                    bridge
                        .findField {
                            matcher {
                                declaredClass(feedCardName)
                                type(adMetaName)
                                name(adMetaFieldName)
                            }
                        }.singleOrNull()
                field?.readers ?: emptyList()
            }.getOrDefault(emptyList())

        // exclude serialization internals that also read this field
        val fromReaders =
            fieldReaders
                .filter { isHookableMethod(it, feedCardName) }
                .sortedByDescending(::scoreCardProcessorCandidate)
                .map(::toMethodRef)

        // callers catch the active render path when the reader itself is inlined or bypassed
        val fromReaderCallers =
            fieldReaders
                .flatMap { reader ->
                    runCatching { reader.callers.toList() }.getOrDefault(emptyList())
                }.filter { isHookableMethod(it, feedCardName) }
                .sortedByDescending(::scoreCardProcessorCandidate)
                .map(::toMethodRef)

        val byParam =
            runCatching {
                bridge.findMethod {
                    matcher {
                        paramTypes(feedCardName)
                        paramCount(range = 1..8)
                    }
                }
            }.getOrDefault(emptyList())

        val fromByParam =
            byParam
                .filter { isHookableMethod(it, feedCardName) }
                .sortedByDescending(::scoreCardProcessorCandidate)
                .map(::toMethodRef)

        return (fromReaders + fromReaderCallers + fromByParam)
            .distinctBy {
                "${it.className}.${it.methodName}(${it.paramTypeNames.joinToString(",")})"
            }.take(MAX_CARD_PROCESSOR_METHODS)
    }

    private fun isHookableMethod(
        method: MethodData,
        feedCardName: String,
    ): Boolean {
        if (method.name == "<init>" || method.name == CLINIT) return false
        if (method.declaredClassName == feedCardName) return false
        if (method.declaredClassName.startsWith("java.") ||
            method.declaredClassName.startsWith("kotlin.") ||
            method.declaredClassName.startsWith(
                "android.",
            )
        ) {
            return false
        }
        return true
    }

    private fun toMethodRef(method: MethodData): MethodRef =
        MethodRef(
            className = method.declaredClassName,
            methodName = method.name,
            paramTypeNames = method.paramTypeNames,
        )

    private fun scoreCardProcessorCandidate(method: MethodData): Int {
        var score = 0
        val methodName = method.name
        val className = method.declaredClassName.lowercase(Locale.ROOT)
        if (methodName == "onBindViewHolder" || methodName.contains("bind")) score += 35
        if (className.contains("discover")) score += 25
        if (className.contains("stream")) score += 15
        if (className.contains("card")) score += 10
        if (method.paramTypeNames.size <= 3) score += 5
        return score
    }

    // anchor: toString literal "WithContent(sessionRepresentation=..." is stable across renames
    private fun findStreamRenderableListMethod(bridge: DexKitBridge): MethodRef? {
        val streamClasses =
            runCatching {
                bridge.findClass {
                    matcher {
                        usingStrings =
                            listOf(
                                "WithContent(sessionRepresentation=",
                                "contentSlices=",
                                "elementsRenderableData=",
                            )
                    }
                }
            }.getOrDefault(emptyList())

        streamClasses.forEach { candidate ->
            val methods =
                runCatching {
                    bridge.findMethod {
                        matcher {
                            declaredClass(candidate.name)
                            returnType("java.util.List")
                            paramCount(0)
                        }
                    }
                }.getOrDefault(emptyList())

            val target =
                methods.firstOrNull { m ->
                    m.name != "<init>" && m.name != CLINIT
                } ?: return@forEach

            return MethodRef(
                className = target.declaredClassName,
                methodName = target.name,
                paramTypeNames = target.paramTypeNames,
            )
        }

        return null
    }

    private val FieldData.isStatic: Boolean
        get() =
            runCatching {
                java.lang.reflect.Modifier
                    .isStatic(modifiers)
            }.getOrDefault(false)

    private fun diagnostic(bridge: DexKitBridge): String =
        buildString {
            append("no ad-metadata class found. Diagnostic:\n")
            append("  searched for <clinit> using number $AD_METADATA_EXTENSION_FIELD_NUMBER\n")

            val protoClasses =
                runCatching {
                    bridge
                        .findClass {
                            matcher {
                                methods {
                                    add { name("dynamicMethod") }
                                }
                            }
                        }.size
                }.getOrDefault(0)
            append("  protobuf-lite classes (have dynamicMethod): $protoClasses\n")

            val sponsoredLabelUsers =
                runCatching {
                    bridge.findClass {
                        matcher { usingStrings = listOf("sponsored_label") }
                    }
                }.getOrDefault(emptyList())
            append("  classes using 'sponsored_label' string: ${sponsoredLabelUsers.size}\n")
            sponsoredLabelUsers.take(3).forEach { c ->
                append("    ${c.name}\n")
            }

            val sponsoredEnums =
                runCatching {
                    bridge
                        .findClass {
                            matcher { usingStrings = listOf("SPONSORED") }
                        }.filter {
                            runCatching { it.superClass?.name == "java.lang.Enum" }.getOrDefault(
                                false,
                            )
                        }
                }.getOrDefault(emptyList())
            append("  enums with SPONSORED literal: ${sponsoredEnums.size}\n")
            sponsoredEnums.take(2).forEach { c ->
                append("    ${c.name}\n")
            }
        }
}
