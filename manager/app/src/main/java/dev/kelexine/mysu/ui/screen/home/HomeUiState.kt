package dev.kelexine.mysu.ui.screen.home

import androidx.compose.runtime.Immutable
import dev.kelexine.mysu.KernelVersion
import dev.kelexine.mysu.ui.util.module.LatestVersionInfo

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion,
    val mysuVersion: Int?,
    val managerUAPIVersion: Int,
    val kernelUAPIVersion: Int?,
    val lkmMode: Boolean?,
    val isManager: Boolean,
    val isManagerPrBuild: Boolean,
    val isKernelPrBuild: Boolean,
    val requiresNewKernel: Boolean,
    val uapiMismatch: Boolean,
    val isRootAvailable: Boolean,
    val isSafeMode: Boolean,
    val isLateLoadMode: Boolean,
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionCode: Long,
    val systemInfo: SystemInfo,
) {
    val isSELinuxPermissive: Boolean
        get() = systemInfo.selinuxStatus == "Permissive"

    val isFullFeatured: Boolean
        get() = isManager && !requiresNewKernel && isRootAvailable

    val showGkiWarning: Boolean
        get() = mysuVersion != null && lkmMode == false

    val showRequireKernelWarning: Boolean
        get() = isManager && requiresNewKernel

    val showUAPIMisMatchWarning: Boolean
        get() = isManager && showRequireKernelWarning && uapiMismatch

    val showRootWarning: Boolean
        get() = mysuVersion != null && !isRootAvailable

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val showVersionMismatchWarning: Boolean
        get() = mysuVersion != null && mysuVersion.toLong() != currentManagerVersionCode

    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentManagerVersionCode
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onJailbreakClick: () -> Unit = {},
)
