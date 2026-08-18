package com.anga9.seller.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase client singleton for Seller App.
 *
 * Used for authentication (Phone OTP login via Supabase -> MSG91 -> DLT SMS).
 * Same Supabase project as Customer App.
 *
 * The anon key is safe to include in the app — it is a publishable key.
 * Never include the service_role key in the app.
 */
object SupabaseClient {

    private const val SUPABASE_URL = "https://plfaugkadavxenpqawzw.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Let Supabase handle session saving
            autoSaveToStorage = true
        }
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(io.github.jan.supabase.storage.Storage)
    }

    /**
     * Convenience accessor for the Auth plugin.
     */
    val auth get() = client.auth

    /**
     * Convenience accessor for the Storage plugin.
     */
    val storage get() = client.storage
}
