package dev.kelexine.mysu.ui.component.choosekmidialog

import androidx.compose.runtime.Composable

@Composable
fun ChooseKmiDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (String?) -> Unit
) {
    ChooseKmiDialogMaterial(show, onDismissRequest, onSelected)
}
