package eu.hxreborn.discoveradsfilter.discovery

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TargetCache {
    fun load(
        dataDir: String,
        targetVersionCode: Long,
        moduleVersionCode: Long,
    ): ResolvedTargets? {
        val file = cacheFile(dataDir, targetVersionCode, moduleVersionCode)
        if (!file.isFile) return null
        return try {
            val root = JSONObject(file.readText())
            if (root.getLong("targetVersionCode") != targetVersionCode ||
                root.getLong("moduleVersionCode") != moduleVersionCode
            ) {
                null
            } else if (root.getString("status") == "missing") {
                ResolvedTargets.Missing(root.getString("reason"))
            } else {
                ResolvedTargets.Resolved(decodeMethod(root.getJSONObject("streamMethod")))
            }
        } catch (_: Exception) {
            null
        }
    }

    fun store(
        dataDir: String,
        targetVersionCode: Long,
        moduleVersionCode: Long,
        targets: ResolvedTargets,
    ) {
        val file = cacheFile(dataDir, targetVersionCode, moduleVersionCode)
        val root =
            JSONObject()
                .put("targetVersionCode", targetVersionCode)
                .put("moduleVersionCode", moduleVersionCode)
        when (targets) {
            is ResolvedTargets.Missing -> {
                if (targets.retryable) return
                root.put("status", "missing")
                root.put("reason", targets.reason)
            }

            is ResolvedTargets.Resolved -> {
                root.put("status", "resolved")
                root.put("streamMethod", encodeMethod(targets.streamRenderableListMethod))
            }
        }
        try {
            val directory = file.parentFile
            directory?.mkdirs()
            directory
                ?.listFiles { candidate ->
                    candidate.name.startsWith(FILE_PREFIX) && candidate.name != file.name
                }?.forEach(File::delete)
            val temporary = File(directory, "${file.name}.tmp")
            temporary.writeText(root.toString())
            if (!temporary.renameTo(file)) temporary.delete()
        } catch (_: Exception) {
        }
    }

    private const val FILE_PREFIX = "targets-"

    private fun cacheFile(
        dataDir: String,
        targetVersionCode: Long,
        moduleVersionCode: Long,
    ): File =
        File(
            dataDir,
            "files/discover-adsfilter/$FILE_PREFIX$targetVersionCode-$moduleVersionCode.json",
        )

    private fun encodeMethod(ref: MethodRef): JSONObject =
        JSONObject()
            .put("className", ref.className)
            .put("methodName", ref.methodName)
            .put("returnTypeName", ref.returnTypeName)
            .put("paramTypeNames", JSONArray(ref.paramTypeNames))

    private fun decodeMethod(json: JSONObject): MethodRef {
        val params = json.getJSONArray("paramTypeNames")
        return MethodRef(
            className = json.getString("className"),
            methodName = json.getString("methodName"),
            returnTypeName = json.getString("returnTypeName"),
            paramTypeNames = List(params.length()) { params.getString(it) },
        )
    }
}
