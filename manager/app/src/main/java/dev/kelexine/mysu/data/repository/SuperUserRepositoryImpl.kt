package dev.kelexine.mysu.data.repository

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import dev.kelexine.mysu.IMySuInterface
import dev.kelexine.mysu.Natives
import dev.kelexine.mysu.data.model.AppInfo
import dev.kelexine.mysu.mysuApp
import dev.kelexine.mysu.ui.MySuService
import dev.kelexine.mysu.ui.util.MySuCli
import kotlin.coroutines.resume

private const val PARALLEL_CHUNK_SIZE = 32

class SuperUserRepositoryImpl : SuperUserRepository {

    companion object {
        private const val TAG = "SuperUserRepository"
    }

    override suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = connectMySuService {
                Log.w(TAG, "MySuService disconnected")
            }

            var currentBinder = result.first
            var currentConnection = result.second

            try {
                suspend fun reconnect(): IMySuInterface {
                    withContext(Dispatchers.Main) {
                        RootService.unbind(currentConnection)
                    }
                    val retry = connectMySuService { Log.w(TAG, "MySuService disconnected") }
                    currentBinder = retry.first
                    currentConnection = retry.second
                    return IMySuInterface.Stub.asInterface(currentBinder)
                }

                val pm = mysuApp.packageManager
                val start = SystemClock.elapsedRealtime()

                var iface = IMySuInterface.Stub.asInterface(currentBinder)
                val idsArray = try {
                    iface.userIds
                } catch (_: Exception) {
                    iface = reconnect()
                    iface.userIds
                }

                val slice = try {
                    iface.getPackages(0)
                } catch (_: Exception) {
                    iface = reconnect()
                    iface.getPackages(0)
                }

                val packages = slice.list.filter {
                    val ai = it.applicationInfo ?: return@filter false
                    (ai.flags and ApplicationInfo.FLAG_HAS_CODE) != 0
                }

                val newApps = withContext(Dispatchers.Default) {
                    packages
                        .chunked(PARALLEL_CHUNK_SIZE)
                        .flatMap { chunk ->
                            chunk.map { pkgInfo ->
                                async {
                                    val appInfo = pkgInfo.applicationInfo!!
                                    val profile = Natives.getAppProfile(pkgInfo.packageName, appInfo.uid)
                                    AppInfo(
                                        label = appInfo.loadLabel(pm).toString(),
                                        packageInfo = pkgInfo,
                                        profile = profile,
                                    )
                                }
                            }.awaitAll()
                        }
                }

                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}ms (parallel chunks of $PARALLEL_CHUNK_SIZE)")
                Pair(newApps, idsArray.toList())
            } finally {
                withContext(Dispatchers.Main) {
                    RootService.unbind(currentConnection)
                }
            }
        }
    }

    override suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            if (currentApps.isEmpty()) return@runCatching emptyList()

            currentApps.map {
                val profile = Natives.getAppProfile(it.packageName, it.uid)
                it.copy(profile = profile)
            }
        }
    }

    private suspend inline fun connectMySuService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnect()
                }

                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (cont.isActive) {
                        cont.resume(binder as IBinder to this)
                    }
                }
            }

            cont.invokeOnCancellation {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    RootService.unbind(connection)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        RootService.unbind(connection)
                    }
                }
            }

            val intent = Intent(mysuApp, MySuService::class.java)

            val task = RootService.bindOrTask(
                intent,
                Shell.EXECUTOR,
                connection,
            )
            val shell = MySuCli.SHELL
            task?.let { shell.execTask(it) }
        }
    }
}
