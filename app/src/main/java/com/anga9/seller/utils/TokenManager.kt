package com.anga9.seller.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Secure token storage using EncryptedSharedPreferences.
 * Stores Supabase JWT access token and user metadata.
 *
 * Phase 1 (Multi-Brand): Added active_brand_id storage.
 * active_brand_id is stored in the same EncryptedSharedPreferences file.
 * It is NOT a secret (it is a UUID), but we store it here for convenience
 * alongside the other seller session data.
 */
object TokenManager {

    private const val PREFS_FILE = "anga9_seller_secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    // Multi-Brand: active child brand ID (null = operating as parent/self)
    private const val KEY_ACTIVE_BRAND_ID = "active_brand_id"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String?) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun getToken(context: Context): String? {
        val supabaseToken = com.anga9.seller.network.SupabaseClient.auth.currentAccessTokenOrNull()
        return supabaseToken ?: getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserInfo(context: Context, userId: String, role: String, phone: String) {
        getPrefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    fun getUserId(context: Context): String? =
        getPrefs(context).getString(KEY_USER_ID, null)

    fun getUserRole(context: Context): String? =
        getPrefs(context).getString(KEY_USER_ROLE, null)

    fun getUserPhone(context: Context): String? =
        getPrefs(context).getString(KEY_USER_PHONE, null)

    fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("seller_profile_cache", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("anga9_seller_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        kotlinx.coroutines.GlobalScope.launch {
            try {
                com.anga9.seller.network.SupabaseClient.auth.signOut()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // ── Multi-Brand Management — Phase 1 ─────────────────────────────────

    /**
     * Persists the active child brand ID.
     * When set, every API call will carry X-Brand-ID: <brandId> via AuthInterceptor.
     */
    fun setActiveBrandId(context: Context, brandId: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_BRAND_ID, brandId).apply()
    }

    /**
     * Returns the currently active brand ID, or null if operating as self (parent).
     */
    fun getActiveBrandId(context: Context): String? =
        getPrefs(context).getString(KEY_ACTIVE_BRAND_ID, null)

    /**
     * Clears the active brand ID so the seller operates as their own account again.
     * Called on logout and on explicit "switch back to self" action.
     */
    fun clearActiveBrandId(context: Context) {
        getPrefs(context).edit().remove(KEY_ACTIVE_BRAND_ID).apply()
    }

    /**
     * Returns the effective seller ID to use as a seller_id query parameter
     * or as a Supabase Storage folder root.
     *
     * Returns activeBrandId if a child brand is currently selected,
     * otherwise falls back to the authenticated user's own ID.
     *
     * Usage:
     *   val sellerId = TokenManager.getEffectiveSellerId(context)
     *   api.getSellerProducts(sellerId = sellerId)
     *   val uploadPath = "${sellerId}/products/$fileName"
     */
    fun getEffectiveSellerId(context: Context): String? =
        getActiveBrandId(context) ?: getUserId(context)
}
