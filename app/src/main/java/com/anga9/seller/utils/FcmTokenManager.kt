package com.anga9.seller.utils

import android.content.Context
import android.util.Log
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.DeviceTokenRequest
import com.anga9.seller.network.model.UnregisterTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manages FCM device token lifecycle for the Seller App.
 *
 * Phase 5 - Option A (Backend endpoint):
 *   POST   /api/notifications/device-tokens  -> register token after login
 *   DELETE /api/notifications/device-tokens  -> unregister token on logout
 */
object FcmTokenManager {

    private const val TAG = "SellerFcmTokenManager"
    private const val PREF_FCM_TOKEN = "fcm_token"
    private const val PREFS_NAME = "anga_seller_fcm_prefs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fetch FCM token and register with backend. Call after successful login. */
    fun refreshAndRegisterToken(context: Context) {
        if (!TokenManager.isLoggedIn(context)) {
            Log.d(TAG, "Not logged in - skipping token registration")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotEmpty()) {
                    saveTokenLocally(context, token)
                    sendTokenToBackend(context, token)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to get FCM token: ${e.message}")
            }
    }

    /** Called when FCM issues a new token (app reinstall, token rotation). */
    fun onNewToken(context: Context, newToken: String) {
        Log.d(TAG, "FCM token refreshed: ${newToken.take(20)}...")
        saveTokenLocally(context, newToken)
        if (TokenManager.isLoggedIn(context)) {
            sendTokenToBackend(context, newToken)
        }
    }

    /** Unregister token from backend on logout. Call BEFORE clearing JWT. */
    fun clearToken(context: Context) {
        val token = getSavedToken(context) ?: run {
            Log.d(TAG, "No saved token to unregister")
            return
        }
        scope.launch {
            try {
                val apiService = ApiClient.getApiService(context)
                apiService.unregisterDeviceToken(UnregisterTokenRequest(token = token))
                Log.i(TAG, "Device token unregistered from backend")
            } catch (e: Exception) {
                Log.w(TAG, "Network error unregistering token: ${e.message}")
            } finally {
                clearSavedToken(context)
            }
        }
    }

    fun getSavedToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_FCM_TOKEN, null)

    private fun sendTokenToBackend(context: Context, token: String) {
        scope.launch {
            try {
                val apiService = ApiClient.getApiService(context)
                val response = apiService.registerDeviceToken(
                    DeviceTokenRequest(token = token, platform = "android", appType = "seller")
                )
                if (response.isSuccessful) {
                    Log.i(TAG, "Device token registered with backend")
                } else {
                    Log.w(TAG, "Backend rejected token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error registering token: ${e.message}")
            }
        }
    }

    private fun saveTokenLocally(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_FCM_TOKEN, token).apply()
    }

    private fun clearSavedToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREF_FCM_TOKEN).apply()
    }
}