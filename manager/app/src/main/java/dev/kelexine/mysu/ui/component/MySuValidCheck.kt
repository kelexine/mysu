package dev.kelexine.mysu.ui.component

import androidx.compose.runtime.Composable
import dev.kelexine.mysu.Natives

@Composable
fun MySuIsValid(
    content: @Composable () -> Unit
) {
    val isManager = Natives.isManager
    val mysuVersion = if (isManager) Natives.version else null

    if (mysuVersion != null) {
        content()
    }
}
