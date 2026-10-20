package dev.kelexine.mysu.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.kelexine.mysu.ui.navigation3.Navigator
import dev.kelexine.mysu.ui.navigation3.Route
import dev.kelexine.mysu.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = {},
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetVfsHideEnabled = viewModel::setVfsHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetUseSoftReboot = viewModel::setUseSoftReboot,
        onSetBiometricEnabled = viewModel::setBiometricEnabled,
        onSetBiometricOnRootGrant = viewModel::setBiometricOnRootGrant,
        onSetBiometricOnAppProfile = viewModel::setBiometricOnAppProfile,
        onSetBiometricOnSettings = viewModel::setBiometricOnSettings,
        onSetBiometricOnModules = viewModel::setBiometricOnModules,
        onSetBiometricOnAppLaunch = viewModel::setBiometricOnAppLaunch,
        onSetBiometricTimeoutMode = viewModel::setBiometricTimeoutMode,
        onOpenAbout = { navigator.push(Route.About) },
        onSetMysufsHideMountsEnabled = viewModel::setMysufsHideMountsEnabled,
        onSetMysufsAvcSpoofingEnabled = viewModel::setMysufsAvcSpoofingEnabled,
    )

    SettingPagerMaterial(uiState, actions, bottomInnerPadding)
}
