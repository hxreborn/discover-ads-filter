package eu.hxreborn.discoveradsfilter.discovery

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.util.Locale

object DexKitResolver {
    private const val CLINIT = "<clinit>"
    private const val LIST_TYPE = "java.util.List"

    private val streamClassAnchors =
        listOf(
            "WithContent(sessionRepresentation=",
            "contentSlices=",
            "elementsRenderableData=",
        )

    private val nativeLoadFailure: String? by lazy {
        try {
            System.loadLibrary("dexkit")
            null
        } catch (error: UnsatisfiedLinkError) {
            "libdexkit.so failed to load: ${error.message}"
        }
    }

    internal fun hasNativeLoadFailure(): Boolean = nativeLoadFailure != null

    fun resolve(apkPaths: List<String>): ResolvedTargets {
        nativeLoadFailure?.let { return ResolvedTargets.Missing(it, retryable = true) }

        val failures = ArrayList<String>()
        var scanned = false
        for (apkPath in apkPaths.distinct()) {
            try {
                DexKitBridge.create(apkPath).use { bridge ->
                    resolveStreamMethod(bridge)?.let { return ResolvedTargets.Resolved(it) }
                }
                scanned = true
                failures += "$apkPath: no class using the stream literals"
            } catch (exception: Exception) {
                failures += "$apkPath: ${exception.javaClass.simpleName}: ${exception.message}"
            }
        }
        return ResolvedTargets.Missing(
            failures.joinToString(" | ").ifBlank { "no APK paths" },
            retryable = !scanned,
        )
    }

    private fun resolveStreamMethod(bridge: DexKitBridge): MethodRef? =
        bridge
            .findClass {
                matcher { usingStrings = streamClassAnchors }
            }.asSequence()
            .flatMap { candidate ->
                bridge
                    .findMethod {
                        matcher {
                            declaredClass(candidate.name)
                            returnType(LIST_TYPE)
                            paramCount(0)
                        }
                    }.asSequence()
            }.filterNot { it.name == "<init>" || it.name == CLINIT }
            .maxByOrNull(::scoreStreamMethod)
            ?.let { method ->
                MethodRef(
                    className = method.declaredClassName,
                    methodName = method.name,
                    returnTypeName = method.returnTypeName,
                    paramTypeNames = method.paramTypeNames,
                )
            }

    private fun scoreStreamMethod(method: MethodData): Int {
        val methodName = method.name.lowercase(Locale.ROOT)
        val className = method.declaredClassName.lowercase(Locale.ROOT)
        return listOf(
            40,
            if (methodName.contains("content")) 20 else 0,
            if (methodName.contains("element")) 15 else 0,
            if (methodName.contains("render")) 15 else 0,
            if (className.contains("discover")) 15 else 0,
            if (className.contains("stream")) 20 else 0,
        ).sum()
    }
}
