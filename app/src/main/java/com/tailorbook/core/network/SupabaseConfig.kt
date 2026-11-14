package com.tailorbook.core.network

/**
 * Configuration and endpoints for Supabase Cloud integration (TASK-14-02 / REQ-DATA-01).
 * Adheres to NFR-05: Only the public Anon Key is used on client devices.
 * Service-role keys are strictly prohibited in the client binary.
 */
object SupabaseConfig {

    /** Live Supabase project URL */
    var projectUrl: String = "https://hibhfkwvjrtmbkjthjlk.supabase.co"

    /** Public Publishable / Anon Key */
    var anonKey: String = "sb_publishable_w4DT713bFAfxDKurVozspw_1FV9e-6Y"

    /** Active authenticated JWT session token (null if offline / unauthenticated) */
    var authToken: String? = null

    /** Active tenant shop ID */
    var activeShopId: String? = null

    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    fun restEndpoint(table: String): String {
        val base = projectUrl.trimEnd('/')
        return "$base/rest/v1/$table"
    }

    fun storageEndpoint(bucket: String, path: String): String {
        val base = projectUrl.trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$base/storage/v1/object/$bucket/$cleanPath"
    }

    fun authEndpoint(endpoint: String): String {
        val base = projectUrl.trimEnd('/')
        val cleanEndpoint = endpoint.trimStart('/')
        return "$base/auth/v1/$cleanEndpoint"
    }
}
