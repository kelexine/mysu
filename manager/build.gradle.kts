plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

fun getGitCommitCount(): Int {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
        val out = process.inputStream.bufferedReader().use { it.readText().trim() }
        out.toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
}

fun getGitDescribe(): String {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
        val out = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (out.isNotEmpty()) out else "v1.0.0"
    } catch (e: Exception) {
        "v1.0.0"
    }
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    return 30000 + commitCount
}

fun getVersionName(): String {
    return getGitDescribe()
}
