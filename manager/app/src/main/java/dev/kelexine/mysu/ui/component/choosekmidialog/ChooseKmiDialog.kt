package dev.kelexine.mysu.ui.component.choosekmidialog

import androidx.compose.runtime.Composable
import dev.kelexine.mysu.ui.LocalUiMode
import dev.kelexine.mysu.ui.UiMode

@Composable
fun ChooseKmiDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (String?) -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChooseKmiDialogMiuix(show, onDismissRequest, onSelected)
        UiMode.Material -> ChooseKmiDialogMaterial(show, onDismissRequest, onSelected)
    }
}
