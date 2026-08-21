package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.SellerStatsResponse
import com.anga9.seller.network.model.SubmitKycRequest
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.TokenManager

/**
 * ProfileRepository — Seller App (Phase 3A)
 *
 * Endpoints (verified from user-service/src/routes/user.routes.ts):
 *   GET  /api/users/seller-profile          → get seller profile
 *   POST /api/users/seller-profile          → create seller profile (onboarding)
 *   PATCH /api/users/seller-profile         → update seller profile
 *   POST /api/users/seller-profile/submit   → submit KYC documents
 *   GET  /api/users/seller-stats            → basic stats
 *
 * Firebase has been removed. All data comes from the ANGA9 backend.
 */
class ProfileRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val gson = com.google.gson.Gson()
    private val prefs = context.getSharedPreferences("seller_profile_cache", Context.MODE_PRIVATE)

    suspend fun getSellerProfile(): Result<SellerProfileResponse> {
        return try {
            val response = apiService.getSellerProfile()
            if (response.isSuccessful) {
                val body = response.body()
                val profile = body?.sellerProfile
                if (profile != null) {
                    prefs.edit().putString("cached_seller_profile", gson.toJson(profile)).apply()
                    Result.success(profile)
                } else {
                    getCachedProfile() ?: Result.failure(Exception("Empty profile in response"))
                }
            } else {
                getCachedProfile() ?: Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            getCachedProfile() ?: Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    private fun getCachedProfile(): Result<SellerProfileResponse>? {
        val json = prefs.getString("cached_seller_profile", null) ?: return null
        return try {
            val profile = gson.fromJson(json, SellerProfileResponse::class.java)
            if (profile != null) Result.success(profile) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createSellerProfile(request: UpdateSellerProfileRequest): Result<SellerProfileResponse> {
        return try {
            val response = apiService.createSellerProfile(request)
            if (response.isSuccessful) {
                val body = response.body()
                val profile = body?.sellerProfile
                if (profile != null) Result.success(profile)
                else Result.failure(Exception("Empty profile in response"))
            } else {
                Result.failure(Exception("Failed to create profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun updateSellerProfile(request: UpdateSellerProfileRequest): Result<SellerProfileResponse> {
        return try {
            val response = apiService.updateSellerProfile(request)
            if (response.isSuccessful) {
                val body = response.body()
                val profile = body?.sellerProfile
                if (profile != null) Result.success(profile)
                else Result.failure(Exception("Empty profile in response"))
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun submitKyc(request: SubmitKycRequest): Result<SellerProfileResponse> {
        return try {
            val response = apiService.submitKyc(request)
            if (response.isSuccessful) {
                val body = response.body()
                val profile = body?.sellerProfile
                if (profile != null) Result.success(profile)
                else Result.failure(Exception("Empty profile in response"))
            } else {
                Result.failure(Exception("KYC submission failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getSellerStats(): Result<SellerStatsResponse> {
        return try {
            val response = apiService.getSellerStats()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = response.errorBody()?.string()?.let {
                    try {
                        org.json.JSONObject(it).optString("error", "Failed to delete account (${response.code()})")
                    } catch (e: Exception) {
                        "Failed to delete account (${response.code()})"
                    }
                } ?: "Failed to delete account (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun lookupIfsc(ifsc: String): Result<Pair<String, String>> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cleanIfsc = ifsc.trim().uppercase()
                if (cleanIfsc.length != 11) {
                    return@withContext Result.failure(Exception("IFSC must be 11 characters"))
                }
                val url = java.net.URL("https://ifsc.razorpay.com/$cleanIfsc")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode == 200) {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()
                    val json = org.json.JSONObject(responseStr)
                    val bank = json.optString("BANK", "")
                    val branch = json.optString("BRANCH", "")
                    Result.success(Pair(bank, branch))
                } else {
                    Result.failure(Exception("Invalid IFSC code"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getSavedSellerId(): String? = TokenManager.getUserId(context)
}
