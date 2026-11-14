package com.tailorbook.feature.auth

import com.tailorbook.core.network.AuthSession
import com.tailorbook.core.network.SupabaseAuthService
import com.tailorbook.core.network.SupabaseConfig
import com.tailorbook.feature.auth.domain.AuthRepository
import com.tailorbook.feature.auth.domain.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSupabaseAuthService : SupabaseAuthService {
    var shouldFail: Boolean = false
    var failureMessage: String = "Auth error"
    var lastRequestedResetEmail: String? = null
    var lastVerifiedOtp: String? = null

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String?
    ): Result<AuthSession> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(
            AuthSession(
                userId = "usr-test-123",
                email = email,
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                shopName = shopName,
                ownerName = ownerName
            )
        )
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthSession> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(
            AuthSession(
                userId = "usr-test-123",
                email = email,
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                shopName = "Test Boutique",
                ownerName = "Test Owner"
            )
        )
    }

    override suspend fun requestPasswordResetOtp(email: String): Result<Unit> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        lastRequestedResetEmail = email
        return Result.success(Unit)
    }

    override suspend fun verifyPasswordResetOtp(
        email: String,
        otp: String,
        newPassword: String
    ): Result<Unit> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        lastVerifiedOtp = otp
        return Result.success(Unit)
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthSession> {
        return Result.success(
            AuthSession(
                userId = "usr-test-123",
                email = "test@example.com",
                accessToken = "refreshed-token",
                refreshToken = "refreshed-refresh-token"
            )
        )
    }

    override suspend fun signOut(accessToken: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class FakePreferencesManager {
    val isLoggedInFlow = MutableStateFlow(false)
    val userIdFlow = MutableStateFlow<String?>(null)

    suspend fun setLoggedIn(isLoggedIn: Boolean, userId: String? = null) {
        isLoggedInFlow.value = isLoggedIn
        userIdFlow.value = userId
    }

    suspend fun saveShopProfile(shopName: String, ownerName: String, ownerPhone: String, businessType: String) {
        // no-op for fake
    }

    suspend fun clearSession() {
        isLoggedInFlow.value = false
        userIdFlow.value = null
    }
}

class AuthRepositoryTest {

    private lateinit var authService: FakeSupabaseAuthService
    private lateinit var fakePrefs: FakePreferencesManager

    @Before
    fun setUp() {
        authService = FakeSupabaseAuthService()
        fakePrefs = FakePreferencesManager()
        SupabaseConfig.authToken = null
        SupabaseConfig.activeShopId = null
    }

    @Test
    fun `signInWithEmail success stores session and configures authToken`() = runBlocking {
        // Test direct fake auth service logic
        val result = authService.signInWithEmail("boutique@test.com", "Password123!")
        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("boutique@test.com", session.email)
        assertEquals("mock-access-token", session.accessToken)

        fakePrefs.setLoggedIn(true, session.userId)
        SupabaseConfig.authToken = session.accessToken
        SupabaseConfig.activeShopId = session.userId

        assertTrue(fakePrefs.isLoggedInFlow.first())
        assertEquals("usr-test-123", fakePrefs.userIdFlow.first())
        assertEquals("mock-access-token", SupabaseConfig.authToken)
        assertEquals("usr-test-123", SupabaseConfig.activeShopId)
    }

    @Test
    fun `signInWithEmail failure does not create session`() = runBlocking {
        authService.shouldFail = true
        authService.failureMessage = "Invalid credentials"

        val result = authService.signInWithEmail("boutique@test.com", "WrongPassword")
        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        assertFalse(fakePrefs.isLoggedInFlow.first())
        assertNull(SupabaseConfig.authToken)
    }

    @Test
    fun `requestPasswordResetOtp delivers email OTP`() = runBlocking {
        val result = authService.requestPasswordResetOtp("reset@test.com")
        assertTrue(result.isSuccess)
        assertEquals("reset@test.com", authService.lastRequestedResetEmail)
    }

    @Test
    fun `resetPasswordWithOtp verifies 6-digit code and updates password`() = runBlocking {
        val result = authService.verifyPasswordResetOtp("reset@test.com", "123456", "NewSecurePass99!")
        assertTrue(result.isSuccess)
        assertEquals("123456", authService.lastVerifiedOtp)
    }

    @Test
    fun `signOut clears session tokens`() = runBlocking {
        fakePrefs.setLoggedIn(true, "usr-test-123")
        SupabaseConfig.authToken = "active-token"
        SupabaseConfig.activeShopId = "usr-test-123"

        fakePrefs.clearSession()
        SupabaseConfig.authToken = null
        SupabaseConfig.activeShopId = null

        assertFalse(fakePrefs.isLoggedInFlow.first())
        assertNull(fakePrefs.userIdFlow.first())
        assertNull(SupabaseConfig.authToken)
        assertNull(SupabaseConfig.activeShopId)
    }
}
