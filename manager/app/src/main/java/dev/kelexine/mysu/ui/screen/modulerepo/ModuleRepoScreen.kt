package dev.kelexine.mysu.ui.screen.modulerepo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.kelexine.mysu.ui.LocalUiMode
import dev.kelexine.mysu.ui.UiMode
import dev.kelexine.mysu.ui.navigation3.LocalNavigator
import dev.kelexine.mysu.ui.navigation3.Route
import dev.kelexine.mysu.ui.screen.flash.FlashIt
import dev.kelexine.mysu.ui.util.module.fetchModuleDetail
import dev.kelexine.mysu.ui.viewmodel.ModuleRepoViewModel
import dev.kelexine.mysu.ui.viewmodel.ModuleViewModel

@Composable
fun ModuleRepoScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<ModuleRepoViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedVm = viewModel<ModuleViewModel>()
    val installedUiState by installedVm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState.modules.isEmpty()) {
            viewModel.refresh()
        }
        if (installedUiState.moduleList.isEmpty()) {
            installedVm.fetchModuleList()
        }
    }

    val actions = ModuleRepoActions(
        onBack = { navigator.pop() },
        onRefresh = viewModel::refresh,
        onSearchTextChange = viewModel::updateSearchText,
        onClearSearch = { viewModel.updateSearchText("") },
        onSearchStatusChange = viewModel::updateSearchStatus,
        onSetSortOrder = viewModel::setSortOrder,
        onOpenRepoDetail = { module ->
            val args = RepoModuleArg(
                moduleId = module.moduleId,
                moduleName = module.moduleName,
                authors = module.authors,
                authorsList = module.authorList.map { AuthorArg(it.name, it.link) },
                latestRelease = module.latestRelease,
                latestReleaseTime = module.latestReleaseTime,
                releases = emptyList()
            )
            navigator.push(Route.ModuleRepoDetail(args))
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ModuleRepoScreenMiuix(uiState, actions)
        UiMode.Material -> ModuleRepoScreenMaterial(uiState, actions)
    }
}

@Composable
fun ModuleRepoDetailScreen(module: RepoModuleArg) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    var readmeHtml by remember(module.moduleId) { mutableStateOf<String?>(null) }
    var readmeLoaded by remember(module.moduleId) { mutableStateOf(false) }
    var detailReleases by remember(module.moduleId) { mutableStateOf<List<ReleaseArg>>(emptyList()) }
    var webUrl by remember(module.moduleId) { mutableStateOf("https://modules.mysu.org/module/${module.moduleId}") }
    var sourceUrl by remember(module.moduleId) { mutableStateOf("https://github.com/MySU-Modules-Repo/${module.moduleId}") }

    LaunchedEffect(module.moduleId) {
        if (module.moduleId.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val detail = fetchModuleDetail(module.moduleId)
                    if (detail != null) {
                        readmeHtml = detail.readmeHtml
                        if (detail.url.isNotEmpty() && detail.url != "null") {
                            webUrl = detail.url
                        }
                        if (detail.sourceUrl.isNotEmpty() && detail.sourceUrl != "null") {
                            sourceUrl = detail.sourceUrl
                        }
                        detailReleases = detail.releases.map { r ->
                            ReleaseArg(
                                tagName = r.tagName,
                                name = r.name,
                                publishedAt = r.publishedAt,
                                assets = r.assets.map { a -> ReleaseAssetArg(a.name, a.downloadUrl, a.size, a.downloadCount) },
                                descriptionHTML = r.descriptionHTML
                            )
                        }
                    } else {
                        detailReleases = emptyList()
                    }
                }.onSuccess {
                    readmeLoaded = true
                }.onFailure {
                    readmeLoaded = true
                    detailReleases = emptyList()
                }
            }
        } else {
            readmeLoaded = true
        }
    }

    val state = ModuleRepoDetailUiState(
        module = module,
        readmeHtml = readmeHtml,
        readmeLoaded = readmeLoaded,
        detailReleases = detailReleases,
        webUrl = webUrl,
        sourceUrl = sourceUrl,
    )
    val actions = ModuleRepoDetailActions(
        onBack = { navigator.pop() },
        onOpenWebUrl = { if (webUrl.isNotEmpty()) uriHandler.openUri(webUrl) },
        onOpenUrl = uriHandler::openUri,
        onInstallModule = { uri -> navigator.push(Route.Flash(FlashIt.FlashModules(listOf(uri)))) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ModuleRepoDetailScreenMiuix(state, actions)
        UiMode.Material -> ModuleRepoDetailScreenMaterial(state, actions)
    }
}
