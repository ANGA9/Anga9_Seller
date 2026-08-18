package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.AuthVerifyRequest
import com.anga9.seller.network.model.UserProfileResponse
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Auth repository for Seller App.
 *
 * Flow:
 * 1. Seller enters phone number
 * 2. Supabase SDK sends OTP (via Twilio/MSG91 — DLT required for India)
 * 3. Seller enters OTP → Supabase verifies → returns access token
 * 4. Call verifyTokenWithBackend() to sync user with backend
 * 5. Backend returns user profile + kyc_status
 * 6. Save token, navigate based on kyc_status
 *
 * NOTE: Firebase Auth has been removed. Supabase SDK handles OTP authentication.
 */
class AuthRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    /**
     * Verify Supabase token with backend after OTP login.
     * Backend syncs user to public.users table.
     */
    suspend fun verifyTokenWithBackend(supabaseToken: String, refreshToken: String? = null): Result<UserProfileResponse> {
        return try {
            val response = apiService.verifyToken(AuthVerifyRequest(accessToken = supabaseToken))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    TokenManager.saveTokens(context, supabaseToken, refreshToken)
                    TokenManager.saveUserInfo(
                        context,
                        userId = body.user.id,
                        role = body.user.role,
                        phone = body.user.phone ?: ""
                    )
                    val updatedUser = body.user.copy(
                        kycStatus = body.sellerProfile?.kycStatus ?: body.user.kycStatus ?: "not_submitted"
                    )
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorCode = response.code()
                val errorMsg = when (errorCode) {
                    401 -> "Invalid or expired token"
                    403 -> "Access denied"
                    else -> "Server error: $errorCode"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /**
     * Get current user profile from backend.
     * Includes kyc_status for routing decisions.
     */
    suspend fun getCurrentUser(): Result<UserProfileResponse> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    fun isLoggedIn(): Boolean = TokenManager.isLoggedIn(context)

    fun getSavedUserId(): String? = TokenManager.getUserId(context)

    fun signOut() {
        TokenManager.clearAll(context)
        kotlinx.coroutines.GlobalScope.launch {
            try {
                com.anga9.seller.network.SupabaseClient.auth.signOut()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
