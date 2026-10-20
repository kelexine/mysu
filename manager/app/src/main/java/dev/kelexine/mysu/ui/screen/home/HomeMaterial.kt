package dev.kelexine.mysu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kelexine.mysu.KernelVersion
import dev.kelexine.mysu.Natives
import dev.kelexine.mysu.R
import dev.kelexine.mysu.ui.component.WarningLevel
import dev.kelexine.mysu.ui.component.dialog.AppUpdateDialog
import dev.kelexine.mysu.ui.component.material.ExpressiveScaffold
import dev.kelexine.mysu.ui.component.material.TonalCard
import dev.kelexine.mysu.ui.component.material.expressiveTopAppBarColors
import dev.kelexine.mysu.ui.component.rebootlistpopup.RebootListPopup
import dev.kelexine.mysu.ui.component.statustag.StatusTag

@Composable
fun HomePagerMaterial(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.checkUpdateEnabled) {
                UpdateCard(state = state, actions = actions)
            }
            if (state.showManagerPrBuildWarning) {
                WarningCard(stringResource(id = R.string.home_pr_build_warning), level = WarningLevel.Notice)
            } else if (state.showKernelPrBuildWarning) {
                WarningCard(stringResource(id = R.string.home_pr_kernel_warning), level = WarningLevel.Notice)
            }
            if (state.showVersionMismatchWarning) {
                WarningCard(
                    stringResource(
                        id = R.string.home_version_mismatch,
                        state.currentManagerVersionCode,
                        state.mysuVersion ?: 0
                    )
                )
            }
            if (state.showGkiWarning) {
                WarningCard(stringResource(id = R.string.home_gki_warning), level = WarningLevel.Notice)
            }
            if (state.showUAPIMisMatchWarning) {
                WarningCard(
                    stringResource(
                        id = R.string.uapi_mismatch,
                        state.managerUAPIVersion,
                        state.kernelUAPIVersion ?: 0,
                    )
                )
            }
            if (state.showRequireKernelWarning) {
                if (state.currentManagerVersionCode < (state.mysuVersion ?: 0)) {
                    WarningCard(
                        stringResource(
                            id = R.string.require_manager_version,
                            state.currentManagerVersionCode,
                            state.mysuVersion ?: 0,
                        )
                    )
                } else {
                    WarningCard(
                        stringResource(
                            id = R.string.require_kernel_version,
                            state.mysuVersion ?: 0,
                            Natives.MINIMAL_SUPPORTED_KERNEL
                        )
                    )
                }
            }
            if (state.showRootWarning) {
                WarningCard(stringResource(id = R.string.grant_root_failed))
            }

            // Material 3 Expressive Hero Status Card
            HeroStatusCard(
                state = state,
                actions = actions,
            )

            // Ring-0 MYSUFS Kernel Stealth Card
            if (state.isMysufsSupported) {
                StealthEngineCard(state = state)
            }

            // Icon-Rich Segmented System Specifications Card
            SegmentedInfoCard(systemInfo = state.systemInfo)

            // Action Cards with Expressive Styling
            LearnMoreCard(onOpenUrl = actions.onOpenUrl)
            DonateCard(onOpenUrl = actions.onOpenUrl)

            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val newVersion = state.latestVersionInfo
    var showUpdateDialog by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            level = WarningLevel.Notice
        ) {
            showUpdateDialog = true
        }

        AppUpdateDialog(
            show = showUpdateDialog,
            versionInfo = newVersion,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { RebootListPopup() },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun HeroStatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val mysuActive = state.mysuVersion != null
    val notInstalled = !mysuActive && state.kernelVersion.isGKI()

    val containerColor = if (mysuActive) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    val statusIcon = when {
        mysuActive -> Icons.Outlined.CheckCircle
        notInstalled -> Icons.Outlined.Warning
        else -> Icons.Outlined.Block
    }
    val iconTint = when {
        mysuActive -> MaterialTheme.colorScheme.primary
        notInstalled -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val statusTitle = when {
        mysuActive -> stringResource(R.string.home_working)
        notInstalled -> stringResource(R.string.home_not_installed)
        else -> stringResource(R.string.home_unsupported)
    }
    val statusSummary = when {
        mysuActive -> stringResource(R.string.home_working_version, "${state.mysuVersion}-${state.kernelUAPIVersion}")
        notInstalled -> stringResource(R.string.home_click_to_install)
        else -> stringResource(R.string.home_unsupported_reason)
    }
    val workingMode = if (mysuActive) {
        when (state.lkmMode) {
            null -> ""
            true -> "LKM"
            else -> "GKI"
        }
    } else ""

    TonalCard(
        containerColor = containerColor,
        shape = RoundedCornerShape(24.dp),
        onClick = {
            if (!state.isLateLoadMode) {
                if (notInstalled) {
                    actions.onInstallClick()
                } else if (!mysuActive) {
                    actions.onOpenUrl("https://kelexine.github.io/mysu/guide/how-to-integrate-for-non-gki.html")
                } else {
                    actions.onInstallClick()
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expressive Squircle Icon Container
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusTitle,
                        tint = iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }

                if (mysuActive && workingMode.isNotEmpty()) {
                    StatusTag(
                        label = workingMode,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                } else if (notInstalled && state.isSELinuxPermissive) {
                    Button(
                        onClick = actions.onJailbreakClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.home_jailbreak))
                    }
                }
            }

            // Quick Status Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mysuActive && state.isSafeMode) {
                    StatusTag(
                        label = stringResource(id = R.string.safe_mode),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        backgroundColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
                if (mysuActive && state.isLateLoadMode) {
                    StatusTag(
                        label = stringResource(id = R.string.jailbreak_mode),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        backgroundColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
                if (!mysuActive && !state.kernelVersion.isGKI()) {
                    StatusTag(
                        label = stringResource(id = R.string.home_non_gki_hint),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
                if (mysuActive && state.isMysufsSupported) {
                    StatusTag(
                        label = stringResource(R.string.mysufs_ring0_active),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // Metrics Row for Active State
            if (mysuActive) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricCounterItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Shield,
                        label = stringResource(R.string.home_metric_superusers),
                        value = "${state.superuserCount}"
                    )
                    MetricCounterItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Extension,
                        label = stringResource(R.string.home_metric_modules),
                        value = stringResource(R.string.home_working)
                    )
                    MetricCounterItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Security,
                        label = stringResource(R.string.home_metric_stealth),
                        value = if (state.isMysufsSupported) "Ring-0" else "Standard"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCounterItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StealthEngineCard(state: HomeUiState) {
    TonalCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.mysufs_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.mysufs_status_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusTag(
                    label = if (state.mysufsVersion.isNotEmpty()) state.mysufsVersion else "v1.4",
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StealthFeaturePill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.mysufs_mount_cloaking)
                )
                StealthFeaturePill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.mysufs_kstat_spoofing)
                )
                StealthFeaturePill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.mysufs_path_cloaking)
                )
            }
        }
    }
}

@Composable
private fun StealthFeaturePill(
    modifier: Modifier = Modifier,
    label: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentedInfoCard(systemInfo: SystemInfo) {
    TonalCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_system_specs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(6.dp))

            InfoItemRow(
                icon = Icons.Filled.Android,
                label = stringResource(R.string.home_manager_version),
                value = systemInfo.managerVersion
            )
            InfoDivider()

            InfoItemRow(
                icon = Icons.Filled.DeveloperBoard,
                label = stringResource(R.string.home_kernel),
                value = systemInfo.kernelVersion
            )
            InfoDivider()

            InfoItemRow(
                icon = Icons.Filled.PhoneAndroid,
                label = stringResource(R.string.home_device_model),
                value = systemInfo.deviceModel
            )
            InfoDivider()

            InfoItemRow(
                icon = Icons.Filled.Fingerprint,
                label = stringResource(R.string.home_fingerprint),
                value = systemInfo.fingerprint
            )
            InfoDivider()

            val (selinuxDisplay, selinuxColor) = when (systemInfo.selinuxStatus) {
                "Enforcing" -> Pair(stringResource(R.string.selinux_status_enforcing), Color(0xFF4CAF50))
                "Permissive" -> Pair(stringResource(R.string.selinux_status_permissive), Color(0xFFFFA000))
                "Disabled" -> Pair(stringResource(R.string.selinux_status_disabled), Color(0xFFE53935))
                else -> Pair(stringResource(R.string.selinux_status_unknown), Color.Gray)
            }
            InfoItemRow(
                icon = Icons.Filled.Security,
                label = stringResource(R.string.home_selinux_status),
                value = selinuxDisplay,
                statusColor = selinuxColor
            )
            InfoDivider()

            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }
            InfoItemRow(
                icon = Icons.Filled.Shield,
                label = stringResource(R.string.home_seccomp_status),
                value = seccompDisplay
            )
        }
    }
}

@Composable
private fun InfoItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    statusColor: Color? = null
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (statusColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    )
}

@Composable
private fun WarningCard(
    message: String,
    level: WarningLevel = WarningLevel.Error,
    onClick: (() -> Unit)? = null
) {
    val containerColor = when (level) {
        WarningLevel.Error -> MaterialTheme.colorScheme.errorContainer
        WarningLevel.Notice -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        }
    }
    if (onClick != null) {
        TonalCard(containerColor = containerColor, onClick = onClick, content = content)
    } else {
        TonalCard(containerColor = containerColor, content = content)
    }
}

@Composable
private fun LearnMoreCard(onOpenUrl: (String) -> Unit) {
    val url = stringResource(R.string.home_learn_mysu_url)
    TonalCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        onClick = { onOpenUrl(url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_learn_mysu),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_mysu),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DonateCard(onOpenUrl: (String) -> Unit) {
    val url = stringResource(R.string.home_support_url)
    TonalCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        onClick = { onOpenUrl(url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_support_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_support_content),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Activated")
@Composable
private fun StatusCardActivatedPreview() {
    HeroStatusCard(
        state = previewHomeScreenState(mysuVersion = 12345, lkmMode = true),
        actions = HomeActions({}, {})
    )
}

@Preview(name = "Not Activated")
@Composable
private fun StatusCardNotActivatedPreview() {
    HeroStatusCard(state = previewHomeScreenState(mysuVersion = null, lkmMode = null), actions = HomeActions({}, {}))
}

@Preview(name = "Permissive")
@Composable
private fun StatusCardPermissivePreview() {
    HeroStatusCard(
        state = previewHomeScreenState(mysuVersion = null, lkmMode = null, selinuxStatus = "Permissive"),
        actions = HomeActions({}, {})
    )
}

@Preview(name = "Jailbreak")
@Composable
private fun StatusCardJailbreakPreview() {
    HeroStatusCard(
        state = previewHomeScreenState(mysuVersion = 12345, lkmMode = true, isLateLoadMode = true),
        actions = HomeActions({}, {})
    )
}

private val previewSystemInfo = SystemInfo(
    kernelVersion = "6.1.0-android14-0-g123456789000-ab12345678",
    managerVersion = "3.0.0 (30000)",
    deviceModel = "Google Pixel 6 Pro",
    fingerprint = "google/raven/raven:14/AP1A.240305.019:user/release-keys",
    selinuxStatus = "Enforcing",
    seccompStatus = 2
)

private val previewUriHandler = object : UriHandler {
    override fun openUri(uri: String) {}
}

@Composable
private fun HomeScreenPreviewContent(
    mysuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    selinuxStatus: String = "Enforcing",
) {
    CompositionLocalProvider(LocalUriHandler provides previewUriHandler) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val actions = HomeActions({}, {})
            HeroStatusCard(
                state = previewHomeScreenState(
                    mysuVersion = mysuVersion,
                    lkmMode = lkmMode,
                    isSafeMode = isSafeMode,
                    isLateLoadMode = isLateLoadMode,
                    selinuxStatus = selinuxStatus,
                ),
                actions = actions
            )
            SegmentedInfoCard(previewSystemInfo.copy(selinuxStatus = selinuxStatus))
            DonateCard(onOpenUrl = {})
            LearnMoreCard(onOpenUrl = {})
        }
    }
}

@Preview(name = "Home Activated", showBackground = true)
@Composable
private fun HomeScreenActivatedPreview() {
    HomeScreenPreviewContent(mysuVersion = 12345, lkmMode = true)
}

@Preview(name = "Home Not Activated", showBackground = true)
@Composable
private fun HomeScreenNotActivatedPreview() {
    HomeScreenPreviewContent(mysuVersion = null, lkmMode = null)
}

@Preview(name = "Home Permissive", showBackground = true)
@Composable
private fun HomeScreenPermissivePreview() {
    HomeScreenPreviewContent(mysuVersion = null, lkmMode = null, selinuxStatus = "Permissive")
}

@Preview(name = "Home Jailbreak", showBackground = true)
@Composable
private fun HomeScreenJailbreakPreview() {
    HomeScreenPreviewContent(mysuVersion = 12345, lkmMode = true, isLateLoadMode = true)
}

private fun previewHomeScreenState(
    mysuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    selinuxStatus: String = "Enforcing",
) = HomeUiState(
    kernelVersion = KernelVersion(6, 1, 0),
    mysuVersion = mysuVersion,
    lkmMode = lkmMode,
    isManager = true,
    isManagerPrBuild = false,
    isKernelPrBuild = false,
    requiresNewKernel = false,
    isRootAvailable = mysuVersion != null,
    isSafeMode = isSafeMode,
    isLateLoadMode = isLateLoadMode,
    checkUpdateEnabled = false,
    latestVersionInfo = dev.kelexine.mysu.ui.util.module.LatestVersionInfo(),
    currentManagerVersionCode = 10000,
    systemInfo = previewSystemInfo.copy(selinuxStatus = selinuxStatus),
    kernelUAPIVersion = 1,
    managerUAPIVersion = 1,
    uapiMismatch = false,
    isMysufsSupported = true,
    mysufsVersion = "v1.4.0",
    superuserCount = 3,
    moduleCount = 5
)
