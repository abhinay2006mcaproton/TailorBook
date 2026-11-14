package com.tailorbook.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Domain model representing an authenticated user session.
 */
data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 3600,
    val shopName: String? = null,
    val ownerName: String? = null
)

/**
 * High-performance, zero-dependency Supabase GoTrue Auth service.
 * Handles Email/Password Sign-up, Sign-in, Password Recovery via 6-digit OTP,
 * Token Refresh, and Session Termination.
 */
interface SupabaseAuthService {
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String? = null
    ): Result<AuthSession>

    suspend fun signInWithEmail(email: String, password: String): Result<AuthSession>
    suspend fun requestPasswordResetOtp(email: String): Result<Unit>
    suspend fun verifyPasswordResetOtp(email: String, otp: String, newPassword: String): Result<Unit>
    suspend fun refreshToken(refreshToken: String): Result<AuthSession>
    suspend fun signOut(accessToken: String): Result<Unit>
}

class SupabaseAuthServiceImpl(
    private val config: SupabaseConfig = SupabaseConfig
) : SupabaseAuthService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String?
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("email", email)
                put("password", password)
                putJsonObject("data") {
                    put("shop_name", shopName)
                    put("owner_name", ownerName)
                    if (!phone.isNullOrBlank()) {
                        put("phone", phone)
                    }
                }
                putJsonObject("options") {
                    put("emailRedirectTo", "tailorbook://auth-callback")
                }
            }.toString()

            val response = executePost(
                url = "${config.authEndpoint("signup")}?redirect_to=tailorbook://auth-callback",
                payload = payload
            )
            val parsed = json.parseToJsonElement(response).jsonObject
            val id = parsed["id"]?.jsonPrimitive?.content
                ?: (parsed["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content.orEmpty())
            val userEmail = parsed["email"]?.jsonPrimitive?.content
                ?: (parsed["user"]?.jsonObject?.get("email")?.jsonPrimitive?.content ?: email)
            val accessToken = parsed["access_token"]?.jsonPrimitive?.content.orEmpty()
            val refreshToken = parsed["refresh_token"]?.jsonPrimitive?.content.orEmpty()

            AuthSession(
                userId = id,
                email = userEmail,
                accessToken = accessToken,
                refreshToken = refreshToken,
                shopName = shopName,
                ownerName = ownerName
            )
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("email", email)
                put("password", password)
            }.toString()

            val response = executePost(
                url = "${config.authEndpoint("token")}?grant_type=password",
                payload = payload
            )
            val parsed = json.parseToJsonElement(response).jsonObject
            val accessToken = parsed["access_token"]?.jsonPrimitive?.content.orEmpty()
            val refreshToken = parsed["refresh_token"]?.jsonPrimitive?.content.orEmpty()
            val expiresIn = parsed["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
            val userObj = parsed["user"]?.jsonObject
            val userId = userObj?.get("id")?.jsonPrimitive?.content.orEmpty()
            val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: email
            val userMeta = userObj?.get("user_metadata")?.jsonObject
            val shopName = userMeta?.get("shop_name")?.jsonPrimitive?.content
            val ownerName = userMeta?.get("owner_name")?.jsonPrimitive?.content

            AuthSession(
                userId = userId,
                email = userEmail,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                shopName = shopName,
                ownerName = ownerName
            )
        }
    }

    override suspend fun requestPasswordResetOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("email", email)
            }.toString()
            executePost(
                url = "${config.authEndpoint("recover")}?redirect_to=tailorbook://auth-callback",
                payload = payload
            )
            Unit
        }
    }

    override suspend fun verifyPasswordResetOtp(
        email: String,
        otp: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Verify 6-digit recovery OTP against Supabase GoTrue
            val verifyPayload = buildJsonObject {
                put("type", "recovery")
                put("email", email)
                put("token", otp.trim())
            }.toString()

            val verifyResponse = executePost(config.authEndpoint("verify"), verifyPayload)
            val verifyJson = json.parseToJsonElement(verifyResponse).jsonObject
            val recoveryAccessToken = verifyJson["access_token"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Verification succeeded but no recovery access token returned.")

            // Step 2: Update password using the acquired recovery access token
            val updatePayload = buildJsonObject {
                put("password", newPassword)
            }.toString()

            executePut(
                url = config.authEndpoint("user"),
                payload = updatePayload,
                bearerToken = recoveryAccessToken
            )
            Unit
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("refresh_token", refreshToken)
            }.toString()

            val response = executePost(
                url = "${config.authEndpoint("token")}?grant_type=refresh_token",
                payload = payload
            )
            val parsed = json.parseToJsonElement(response).jsonObject
            val accessToken = parsed["access_token"]?.jsonPrimitive?.content.orEmpty()
            val newRefreshToken = parsed["refresh_token"]?.jsonPrimitive?.content.orEmpty()
            val userObj = parsed["user"]?.jsonObject
            val userId = userObj?.get("id")?.jsonPrimitive?.content.orEmpty()
            val userEmail = userObj?.get("email")?.jsonPrimitive?.content.orEmpty()

            AuthSession(
                userId = userId,
                email = userEmail,
                accessToken = accessToken,
                refreshToken = newRefreshToken
            )
        }
    }

    override suspend fun signOut(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            executePost(
                url = config.authEndpoint("logout"),
                payload = "{}",
                bearerToken = accessToken
            )
            Unit
        }
    }

    private fun executePost(url: String, payload: String, bearerToken: String? = null): String {
        val targetUrl = URL(url)
        val conn = (targetUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", config.anonKey)
            setRequestProperty("Authorization", "Bearer ${bearerToken ?: config.anonKey}")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(payload)
            it.flush()
        }

        val code = conn.responseCode
        val body = readBody(conn, code)
        if (code in 200..299) {
            return body
        } else {
            Timber.e("GoTrue Auth POST $url failed: HTTP $code - $body")
            throw AuthException(code, parseErrorMessage(body, code))
        }
    }

    private fun executePut(url: String, payload: String, bearerToken: String): String {
        val targetUrl = URL(url)
        val conn = (targetUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", config.anonKey)
            setRequestProperty("Authorization", "Bearer $bearerToken")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(payload)
            it.flush()
        }

        val code = conn.responseCode
        val body = readBody(conn, code)
        if (code in 200..299) {
            return body
        } else {
            Timber.e("GoTrue Auth PUT $url failed: HTTP $code - $body")
            throw AuthException(code, parseErrorMessage(body, code))
        }
    }

    private fun readBody(conn: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: return ""
        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val msg = root["msg"]?.jsonPrimitive?.content
                ?: root["message"]?.jsonPrimitive?.content
                ?: root["error_description"]?.jsonPrimitive?.content
            when {
                msg != null -> msg
                code == 400 -> "Invalid credentials or request format."
                code == 401 -> "Unauthorized request."
                code == 422 -> "Email already exists or invalid password format."
                code == 429 -> "Too many requests. Please wait before retrying."
                else -> "Authentication failed with status $code."
            }
        } catch (_: Exception) {
            if (code == 429) "Too many requests. Please wait a moment." else "Network authentication failed ($code)."
        }
    }
}

class AuthException(val httpCode: Int, message: String) : Exception(message)
