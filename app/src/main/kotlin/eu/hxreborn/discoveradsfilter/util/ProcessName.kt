package eu.hxreborn.discoveradsfilter.util

import java.io.File

object ProcessName {
    fun current(): String? =
        runCatching {
            File(
                "/proc/self/cmdline",
            ).readText().substringBefore('\u0000').takeIf { it.isNotBlank() }
        }.getOrNull()
}
