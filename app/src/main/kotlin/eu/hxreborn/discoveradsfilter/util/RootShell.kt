package eu.hxreborn.discoveradsfilter.util

import com.topjohnwu.superuser.Shell

object RootShell {
    init {
        Shell.setDefaultBuilder(
            Shell.Builder
                .create()
                .setTimeout(10),
        )
    }

    fun forceStop(pkg: String): Result<List<String>> =
        runCatching {
            var result = Shell.cmd("am force-stop $pkg").exec()
            if (!result.isSuccess) {
                dropCachedShell()
                result = Shell.cmd("am force-stop $pkg").exec()
            }
            if (!result.isSuccess) error("su exit=${result.code} out=${result.out}")
            result.out
        }

    fun probe(): Result<Boolean> =
        runCatching {
            if (Shell.getShell().isRoot) return@runCatching true
            dropCachedShell()
            Shell.getShell().isRoot
        }

    private fun dropCachedShell() {
        runCatching { Shell.getCachedShell()?.close() }
    }
}
