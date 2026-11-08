package dev.kelexine.mysu.ui.util

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import java.io.File

/**
 * Returns the raw SELinux status string ("Enforcing", "Permissive", "Disabled", or "Unknown").
 * Safe to call from any thread (IO recommended).
 */
fun getSELinuxStatusRaw(): String {
    // 1. If root is active, query directly for the authoritative kernel status
    if (rootAvailable()) {
        try {
            val out = ShellUtils.fastCmd("getenforce").trim()
            if (out in listOf("Enforcing", "Permissive", "Disabled")) {
                return out
            }
        } catch (_: Throwable) {
        }
    }

    // 2. Direct read of /sys/fs/selinux/enforce if accessible
    try {
        val enforceFile = File("/sys/fs/selinux/enforce")
        if (enforceFile.exists() && enforceFile.canRead()) {
            val content = enforceFile.readText().trim()
            if (content == "1") return "Enforcing"
            if (content == "0") return "Permissive"
        }
    } catch (_: Throwable) {
    }

    // 3. Fallback to unprivileged shell
    // When SELinux is Enforcing, unprivileged execution is denied ("Permission denied").
    // When SELinux is Permissive, getenforce succeeds and prints "Permissive".
    try {
        val shell = Shell.Builder.create().build("sh")
        val stdoutList = ArrayList<String>()
        val stderrList = ArrayList<String>()
        val result = shell.use {
            it.newJob().add("getenforce").to(stdoutList, stderrList).exec()
        }
        val stdout = stdoutList.joinToString("\n").trim()
        val stderr = stderrList.joinToString("\n").trim()

        if (result.isSuccess) {
            return when (stdout) {
                "Enforcing", "Permissive", "Disabled" -> stdout
                else -> "Unknown"
            }
        }

        if (stderr.contains("Permission denied", ignoreCase = true)) {
            return "Enforcing"
        }
    } catch (_: Throwable) {
    }

    return "Unknown"
}
