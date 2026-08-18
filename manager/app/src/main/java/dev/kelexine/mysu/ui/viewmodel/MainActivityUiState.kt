package dev.kelexine.mysu.ui.viewmodel

import androidx.compose.runtime.Immutable
import dev.kelexine.mysu.ui.UiMode
import dev.kelexine.mysu.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val enableNavigationBadge: Boolean,
    val uiMode: UiMode,
)
