package com.tailorbook.feature.auth.domain

import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.device.DeviceManager
import com.tailorbook.core.network.AuthSession
import com.tailorbook.core.network.SupabaseAuthService
import com.tailorbook.core.network.SupabaseConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Provider-independent authentication repository interface (REQ-AUTH-01).
 * UI screens interact solely with this abstraction, keeping business logic
 * fully isolated from underlying providers (Supabase, SMS gateways, etc.).
 */
interface AuthRepository {
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String? = null,
        upiId: String? = null
    ): Result<AuthSession>

    suspend fun signInWithEmail(email: String, password: String): Result<AuthSession>
    suspend fun requestPasswordResetOtp(email: String): Result<Unit>
    suspend fun resetPasswordWithOtp(email: String, otp: String, newPassword: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun isUserLoggedIn(): Flow<Boolean>
    fun getSavedUserId(): Flow<String?>
}

class AuthRepositoryImpl(
    private val authService: SupabaseAuthService,
    private val preferencesManager: UserPreferencesManager,
    private val deviceManager: DeviceManager? = null
) : AuthRepository {

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String?,
        upiId: String?
    ): Result<AuthSession> {
        val result = authService.signUpWithEmail(email, password, shopName, ownerName, phone)
        return result.onSuccess { session ->
            if (session.accessToken.isNotBlank()) {
                preferencesManager.setLoggedIn(true, session.userId, session.email)
                preferencesManager.saveShopProfile(
                    shopName = shopName,
                    ownerName = ownerName,
                    ownerPhone = phone.orEmpty(),
                    businessType = "BOUTIQUE",
                    upiId = upiId.orEmpty()
                )
                SupabaseConfig.authToken = session.accessToken
                SupabaseConfig.activeShopId = session.userId

                // Bind device to registered email for device quota tracking
                deviceManager?.recordDeviceRegistration(session.email, shopName)
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthSession> {
        val result = authService.signInWithEmail(email, password)
        return result.onSuccess { session ->
            preferencesManager.setLoggedIn(true, session.userId, session.email)
            if (!session.shopName.isNullOrBlank() || !session.ownerName.isNullOrBlank()) {
                preferencesManager.saveShopProfile(
                    shopName = session.shopName ?: "My Boutique",
                    ownerName = session.ownerName ?: "Owner",
                    ownerPhone = "",
                    businessType = "BOUTIQUE"
                )
            }
            SupabaseConfig.authToken = session.accessToken
            SupabaseConfig.activeShopId = session.userId

            // Record device binding on login to ensure existing devices are registered in Supabase
            deviceManager?.recordDeviceRegistration(session.email, session.shopName)
        }
    }

    override suspend fun requestPasswordResetOtp(email: String): Result<Unit> {
        return authService.requestPasswordResetOtp(email)
    }

    override suspend fun resetPasswordWithOtp(
        email: String,
        otp: String,
        newPassword: String
    ): Result<Unit> {
        return authService.verifyPasswordResetOtp(email, otp, newPassword)
    }

    override suspend fun signOut(): Result<Unit> {
        val currentToken = SupabaseConfig.authToken
        if (!currentToken.isNullOrBlank()) {
            authService.signOut(currentToken)
        }
        preferencesManager.clearSession()
        SupabaseConfig.authToken = null
        SupabaseConfig.activeShopId = null
        return Result.success(Unit)
    }

    override fun isUserLoggedIn(): Flow<Boolean> = preferencesManager.isLoggedInFlow

    override fun getSavedUserId(): Flow<String?> = preferencesManager.userIdFlow
}
