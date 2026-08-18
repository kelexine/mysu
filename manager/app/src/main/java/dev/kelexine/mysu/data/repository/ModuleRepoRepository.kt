package dev.kelexine.mysu.data.repository

import dev.kelexine.mysu.data.model.RepoModule

interface ModuleRepoRepository {
    suspend fun fetchModules(): Result<List<RepoModule>>
}
