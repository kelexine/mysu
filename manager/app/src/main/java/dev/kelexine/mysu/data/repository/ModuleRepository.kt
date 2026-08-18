package dev.kelexine.mysu.data.repository

import dev.kelexine.mysu.data.model.Module
import dev.kelexine.mysu.data.model.ModuleUpdateInfo

interface ModuleRepository {
    suspend fun getModules(): Result<List<Module>>
    suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo>
}
