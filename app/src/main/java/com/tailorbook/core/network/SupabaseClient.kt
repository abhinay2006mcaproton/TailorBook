package com.tailorbook.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance, zero-dependency Supabase REST and Storage client (TASK-14-02).
 * Handles authentication header injection, PostgREST upsert (with on_conflict),
 * delete, query, and binary photo uploads to Supabase Storage.
 */
interface SupabaseClient {
    suspend fun upsert(table: String, payloadJson: String, onConflict: String = "id"): Result<String>
    suspend fun query(table: String, queryParams: String = ""): Result<String>
    suspend fun delete(table: String, idKey: String, idValue: String): Result<String>
    suspend fun uploadStorage(bucket: String, path: String, bytes: ByteArray, mimeType: String = "image/jpeg"): Result<String>
}

class SupabaseClientImpl(
    private val config: SupabaseConfig = SupabaseConfig
) : SupabaseClient {

    override suspend fun upsert(table: String, payloadJson: String, onConflict: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.restEndpoint(table)}?on_conflict=$onConflict"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", config.anonKey)
                setRequestProperty("Authorization", "Bearer ${config.authToken ?: config.anonKey}")
                // Prefer: resolution=merge-duplicates makes it an idempotent upsert in PostgREST
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payloadJson)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = readResponse(connection, responseCode)
            if (responseCode in 200..299) {
                Timber.d("Supabase upsert success on table $table: HTTP $responseCode")
                responseBody
            } else {
                val errorMsg = "Supabase upsert failed on $table: HTTP $responseCode - $responseBody"
                Timber.e(errorMsg)
                throw SupabaseException(responseCode, errorMsg)
            }
        }
    }

    override suspend fun query(table: String, queryParams: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val urlString = if (queryParams.isNotBlank()) {
                "${config.restEndpoint(table)}?$queryParams"
            } else {
                config.restEndpoint(table)
            }
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("apikey", config.anonKey)
                setRequestProperty("Authorization", "Bearer ${config.authToken ?: config.anonKey}")
            }

            val responseCode = connection.responseCode
            val responseBody = readResponse(connection, responseCode)
            if (responseCode in 200..299) {
                responseBody
            } else {
                val errorMsg = "Supabase query failed on $table: HTTP $responseCode - $responseBody"
                Timber.e(errorMsg)
                throw SupabaseException(responseCode, errorMsg)
            }
        }
    }

    override suspend fun delete(table: String, idKey: String, idValue: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.restEndpoint(table)}?$idKey=eq.$idValue"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("apikey", config.anonKey)
                setRequestProperty("Authorization", "Bearer ${config.authToken ?: config.anonKey}")
            }

            val responseCode = connection.responseCode
            val responseBody = readResponse(connection, responseCode)
            if (responseCode in 200..299) {
                responseBody
            } else {
                val errorMsg = "Supabase delete failed on $table: HTTP $responseCode - $responseBody"
                Timber.e(errorMsg)
                throw SupabaseException(responseCode, errorMsg)
            }
        }
    }

    override suspend fun uploadStorage(bucket: String, path: String, bytes: ByteArray, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = config.storageEndpoint(bucket, path)
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
                setRequestProperty("Content-Type", mimeType)
                setRequestProperty("apikey", config.anonKey)
                setRequestProperty("Authorization", "Bearer ${config.authToken ?: config.anonKey}")
                setRequestProperty("x-upsert", "true")
            }

            connection.outputStream.use { os ->
                os.write(bytes)
                os.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = readResponse(connection, responseCode)
            if (responseCode in 200..299) {
                Timber.d("Supabase storage upload success: $endpoint")
                endpoint
            } else {
                val errorMsg = "Supabase storage upload failed: HTTP $responseCode - $responseBody"
                Timber.e(errorMsg)
                throw SupabaseException(responseCode, errorMsg)
            }
        }
    }

    private fun readResponse(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        } ?: return ""

        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { reader ->
            reader.readText()
        }
    }
}

class SupabaseException(val statusCode: Int, message: String) : Exception(message)
