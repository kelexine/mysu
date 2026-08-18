package dev.kelexine.mysu.ui.component.uninstalldialog

import androidx.compose.runtime.Composable
import dev.kelexine.mysu.ui.LocalUiMode
import dev.kelexine.mysu.ui.UiMode

@Composable
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> UninstallDialogMiuix(show, onDismissRequest)
        UiMode.Material -> UninstallDialogMaterial(show, onDismissRequest)
    }
}
