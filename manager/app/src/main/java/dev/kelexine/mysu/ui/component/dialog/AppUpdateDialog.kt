package dev.kelexine.mysu.ui.component.dialog

import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kelexine.mysu.R
import dev.kelexine.mysu.ui.LocalUiMode
import dev.kelexine.mysu.ui.UiMode
import dev.kelexine.mysu.ui.component.markdown.MarkdownContent
import dev.kelexine.mysu.ui.component.material.ExpressiveDialog
import dev.kelexine.mysu.ui.util.ApkInstaller
import dev.kelexine.mysu.ui.util.DownloadManager
import dev.kelexine.mysu.ui.util.download
import dev.kelexine.mysu.ui.util.module.LatestVersionInfo
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File

@Composable
fun AppUpdateDialog(
    show: Boolean,
    versionInfo: LatestVersionInfo,
    onDismiss: () -> Unit,
) {
    if (!show) return

    when (LocalUiMode.current) {
        UiMode.Miuix -> AppUpdateDialogMiuix(versionInfo, onDismiss)
        UiMode.Material -> AppUpdateDialogMaterial(versionInfo, onDismiss)
    }
}

@Composable
private fun AppUpdateDialogMaterial(
    versionInfo: LatestVersionInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads by DownloadManager.downloads.collectAsState()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val startDownload: () -> Unit = {
        isDownloading = true
        downloadError = null
        val fileName = "MySU_${versionInfo.versionCode}.apk"
        scope.launch {
            try {
                download(
                    url = versionInfo.downloadUrl,
                    fileName = fileName,
                    onDownloading = {
                        isDownloading = true
                    },
                    onProgress = { progress ->
                        downloadProgress = progress
                    },
                    onDownloaded = { uri ->
                        isDownloading = false
                        downloadProgress = 100
                        val target = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        val actualFile = if (target.exists()) target else File(uri.path ?: "")
                        downloadedFile = actualFile
                        ApkInstaller.installApk(context, actualFile)
                    }
                )
            } catch (e: Exception) {
                isDownloading = false
                downloadError = e.localizedMessage ?: "Download failed"
            }
        }
    }

    ExpressiveDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        title = {
            Column {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Build ${versionInfo.versionCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (versionInfo.changelog.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MarkdownContent(
                            content = versionInfo.changelog,
                            isMarkdown = true
                        )
                    }
                }

                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.update_downloading, downloadProgress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (downloadError != null) {
                    Text(
                        text = stringResource(R.string.update_download_failed, downloadError.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (downloadedFile != null && !isDownloading) {
                TextButton(onClick = { ApkInstaller.installApk(context, downloadedFile!!) }) {
                    Text(stringResource(R.string.update_install))
                }
            } else if (!isDownloading) {
                TextButton(onClick = startDownload) {
                    Text(stringResource(R.string.update_now))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AppUpdateDialogMiuix(
    versionInfo: LatestVersionInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val startDownload: () -> Unit = {
        isDownloading = true
        downloadError = null
        val fileName = "MySU_${versionInfo.versionCode}.apk"
        scope.launch {
            try {
                download(
                    url = versionInfo.downloadUrl,
                    fileName = fileName,
                    onDownloading = {
                        isDownloading = true
                    },
                    onProgress = { progress ->
                        downloadProgress = progress
                    },
                    onDownloaded = { uri ->
                        isDownloading = false
                        downloadProgress = 100
                        val target = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        val actualFile = if (target.exists()) target else File(uri.path ?: "")
                        downloadedFile = actualFile
                        ApkInstaller.installApk(context, actualFile)
                    }
                )
            } catch (e: Exception) {
                isDownloading = false
                downloadError = e.localizedMessage ?: "Download failed"
            }
        }
    }

    WindowDialog(
        show = true,
        title = "${stringResource(R.string.update_dialog_title)} (Build ${versionInfo.versionCode})",
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (versionInfo.changelog.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MarkdownContent(
                            content = versionInfo.changelog,
                            isMarkdown = true
                        )
                    }
                }

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.update_downloading, downloadProgress),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (downloadError != null) {
                    Text(
                        text = stringResource(R.string.update_download_failed, downloadError.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    top.yukonga.miuix.kmp.basic.TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        enabled = !isDownloading
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    if (downloadedFile != null && !isDownloading) {
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = stringResource(R.string.update_install),
                            onClick = { ApkInstaller.installApk(context, downloadedFile!!) },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    } else if (!isDownloading) {
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = stringResource(R.string.update_now),
                            onClick = startDownload,
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
    )
}
