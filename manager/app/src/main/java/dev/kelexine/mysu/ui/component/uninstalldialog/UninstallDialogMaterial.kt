package dev.kelexine.mysu.ui.component.uninstalldialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.kelexine.mysu.R
import dev.kelexine.mysu.ui.component.dialog.rememberConfirmDialog
import dev.kelexine.mysu.ui.component.material.SegmentedColumn
import dev.kelexine.mysu.ui.component.material.SegmentedListItem
import dev.kelexine.mysu.ui.navigation3.LocalNavigator
import dev.kelexine.mysu.ui.navigation3.Route
import dev.kelexine.mysu.ui.screen.flash.FlashIt
import dev.kelexine.mysu.ui.screen.flash.UninstallType
import dev.kelexine.mysu.ui.screen.flash.UninstallType.PERMANENT
import dev.kelexine.mysu.ui.screen.flash.UninstallType.RESTORE_STOCK_IMAGE

@Composable
fun UninstallDialogMaterial(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val options = listOf(
        // TEMPORARY,
        PERMANENT,
        RESTORE_STOCK_IMAGE
    )
    val runType = remember { mutableStateOf<UninstallType?>(null) }

    val run = { type: UninstallType ->
        when (type) {
            PERMANENT -> navigator.push(Route.Flash(FlashIt.FlashUninstall))
            RESTORE_STOCK_IMAGE -> navigator.push(Route.Flash(FlashIt.FlashRestore))
            else -> Unit
        }
    }

    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            onDismissRequest()
            runType.value?.let { type ->
                run(type)
            }
        }
    )

    if (show) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.settings_uninstall)) },
            text = {
                SegmentedColumn(
                    modifier = Modifier,
                    content = options.map { type ->
                        {
                            SegmentedListItem(
                                onClick = {
                                    runType.value = type
                                    confirmDialog.showConfirm(
                                        title = context.getString(type.title),
                                        content = context.getString(type.message)
                                    )
                                },
                                headlineContent = { Text(stringResource(type.title)) },
                                supportingContent = { Text(stringResource(type.message)) },
                                leadingContent = {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
