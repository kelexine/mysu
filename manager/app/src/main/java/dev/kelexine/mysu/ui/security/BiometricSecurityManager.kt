/**
 * BiometricSecurityManager.kt
 * Author: Onogwu Franklin Kelechi (kelexine) <https://github.com/kelexine>
 * Date: 2026-08-23
 * Purpose: Coroutine-based biometric and device credential authentication gate for MySU Manager.
 */
package dev.kelexine.mysu.ui.security

import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.kelexine.mysu.R
import dev.kelexine.mysu.data.repository.SettingsRepository
import dev.kelexine.mysu.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

enum class BiometricAction(@StringRes val titleRes: Int) {
    APP_LAUNCH(R.string.biometric_prompt_subtitle_launch),
    ROOT_GRANT(R.string.biometric_prompt_subtitle_root),
    APP_PROFILE(R.string.biometric_prompt_subtitle_profile),
    SETTINGS_CHANGE(R.string.biometric_prompt_subtitle_settings),
    MODULE_ACTION(R.string.biometric_prompt_subtitle_module);
}

enum class BiometricTimeout(val value: Int, val durationMillis: Long, @StringRes val labelRes: Int) {
    ALWAYS(0, 0L, R.string.biometric_timeout_always),
    ONE_MINUTE(1, 60_000L, R.string.biometric_timeout_1min),
    FIVE_MINUTES(2, 300_000L, R.string.biometric_timeout_5min),
    SESSION(3, -1L, R.string.biometric_timeout_session);

    companion object {
        fun fromValue(value: Int): BiometricTimeout =
            entries.find { it.value == value } ?: ONE_MINUTE
    }
}

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data class Failed(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data object NotAvailable : BiometricAuthResult
}

class BiometricSecurityManager(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) {
    companion object {
        @Volatile
        private var instance: BiometricSecurityManager? = null

        fun getInstance(repo: SettingsRepository = SettingsRepositoryImpl()): BiometricSecurityManager {
            return instance ?: synchronized(this) {
                instance ?: BiometricSecurityManager(repo).also { instance = it }
            }
        }
    }

    private var lastAuthTimestamp: Long = 0L

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isAuthRequired(action: BiometricAction): Boolean {
        if (!repo.biometricEnabled) return false

        val isGuarded = when (action) {
            BiometricAction.APP_LAUNCH -> repo.biometricOnAppLaunch
            BiometricAction.ROOT_GRANT -> repo.biometricOnRootGrant
            BiometricAction.APP_PROFILE -> repo.biometricOnAppProfile
            BiometricAction.SETTINGS_CHANGE -> repo.biometricOnSettings
            BiometricAction.MODULE_ACTION -> repo.biometricOnModules
        }
        if (!isGuarded) return false

        val timeout = BiometricTimeout.fromValue(repo.biometricTimeoutMode)
        if (timeout == BiometricTimeout.ALWAYS) return true
        if (timeout == BiometricTimeout.SESSION) return lastAuthTimestamp == 0L

        val elapsed = SystemClock.elapsedRealtime() - lastAuthTimestamp
        return elapsed > timeout.durationMillis
    }

    fun resetSession() {
        lastAuthTimestamp = 0L
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        action: BiometricAction,
        targetName: String? = null
    ): BiometricAuthResult = withContext(Dispatchers.Main) {
        if (!repo.biometricEnabled || !isAuthRequired(action)) {
            return@withContext BiometricAuthResult.Success
        }

        if (!canAuthenticate(activity)) {
            return@withContext BiometricAuthResult.NotAvailable
        }

        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        val subtitle = if (targetName != null) {
            activity.getString(action.titleRes, targetName)
        } else {
            activity.getString(action.titleRes)
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        lastAuthTimestamp = SystemClock.elapsedRealtime()
                        if (continuation.isActive) {
                            continuation.resume(BiometricAuthResult.Success)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) {
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_CANCELED
                            ) {
                                continuation.resume(BiometricAuthResult.Cancelled)
                            } else {
                                continuation.resume(BiometricAuthResult.Failed(errorCode, errString))
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // Keep listening; BiometricPrompt handles transient retries internally
                    }
                }
            )

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }

            biometricPrompt.authenticate(promptInfo)
        }
    }
}
