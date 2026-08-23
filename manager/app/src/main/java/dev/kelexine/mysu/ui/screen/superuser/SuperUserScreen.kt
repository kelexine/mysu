package dev.kelexine.mysu.ui.screen.superuser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import dev.kelexine.mysu.ui.LocalUiMode
import dev.kelexine.mysu.ui.UiMode
import dev.kelexine.mysu.ui.navigation3.Navigator
import dev.kelexine.mysu.ui.navigation3.Route
import dev.kelexine.mysu.ui.security.BiometricAction
import dev.kelexine.mysu.ui.security.BiometricAuthResult
import dev.kelexine.mysu.ui.security.BiometricSecurityManager
import dev.kelexine.mysu.ui.util.ownerNameForUid
import dev.kelexine.mysu.ui.viewmodel.SuperUserViewModel

@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val viewModel = viewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasActivated by remember { mutableStateOf(false) }
    if (isCurrentPage) hasActivated = true

    if (hasActivated) {
        LaunchedEffect(Unit) {
            if (uiState.groupedApps.isEmpty()) {
                viewModel.initializePreferences()
                viewModel.loadAppList().join()
            } else if (viewModel.isNeedRefresh) {
                viewModel.loadAppList(resort = false).join()
            }
        }
    }

    val onSearchTextChange: (String) -> Unit = viewModel::updateSearchText
    val onToggleShowSystemApps: () -> Unit = {
        viewModel.toggleShowSystemApps()
    }
    val onToggleShowOnlyPrimaryUserApps: () -> Unit = {
        viewModel.toggleShowOnlyPrimaryUserApps()
    }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val biometricManager = remember { BiometricSecurityManager.getInstance() }

    val onOpenProfile: (GroupedApps) -> Unit = { group ->
        if (activity != null && biometricManager.isAuthRequired(BiometricAction.APP_PROFILE)) {
            scope.launch {
                val label = if (group.apps.size > 1) ownerNameForUid(group.uid) else group.primary.label
                val result = biometricManager.authenticate(activity, BiometricAction.APP_PROFILE, label)
                if (result is BiometricAuthResult.Success) {
                    navigator.push(Route.AppProfile(group.uid))
                    viewModel.markNeedRefresh()
                }
            }
        } else {
            navigator.push(Route.AppProfile(group.uid))
            viewModel.markNeedRefresh()
        }
    }
    val actions = SuperUserActions(
        onRefresh = { viewModel.loadAppList(force = true) },
        onOpenSulog = { navigator.push(Route.Sulog) },
        onSearchTextChange = onSearchTextChange,
        onSearchStatusChange = viewModel::updateSearchStatus,
        onClearSearch = { onSearchTextChange("") },
        onToggleShowSystemApps = onToggleShowSystemApps,
        onToggleShowOnlyPrimaryUserApps = onToggleShowOnlyPrimaryUserApps,
        onUpdateSortConfig = { viewModel.updateSortConfig(it) },
        onOpenProfile = onOpenProfile,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SuperUserPagerMiuix(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> SuperUserPagerMaterial(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
