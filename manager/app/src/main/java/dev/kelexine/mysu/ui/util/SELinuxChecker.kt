package dev.kelexine.mysu.ui.util

import android.os.Build
import com.topjohnwu.superuser.ShellUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

/**
 * Returns the raw SELinux status string ("Enforcing", "Permissive", "Disabled", or "Unknown").
 * Safe to call from any thread (IO recommended).
 */
fun getSELinuxStatusRaw(): String {
    // 1. Framework android.os.SELinux via reflection (bypassing hidden API restrictions)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/os/SELinux;")
        }
        val selinuxClass = Class.forName("android.os.SELinux")
        val isEnabled = selinuxClass.getMethod("isSELinuxEnabled").invoke(null) as? Boolean ?: true
        if (!isEnabled) {
            return "Disabled"
        }
        val isEnforced = selinuxClass.getMethod("isSELinuxEnforced").invoke(null) as? Boolean
        if (isEnforced != null) {
            return if (isEnforced) "Enforcing" else "Permissive"
        }
    } catch (_: Throwable) {
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

    // 3. Check via root shell if already available
    if (rootAvailable()) {
        try {
            val out = ShellUtils.fastCmd("getenforce").trim()
            if (out in listOf("Enforcing", "Permissive", "Disabled")) {
                return out
            }
        } catch (_: Throwable) {
        }
    }

    return "Unknown"
}
