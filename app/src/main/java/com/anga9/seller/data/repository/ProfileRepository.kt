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

    suspend fun getSellerProfile(): Result<SellerProfileResponse> {
        return try {
            val response = apiService.getSellerProfile()
            if (response.isSuccessful) {
                val body = response.body()
                val profile = body?.sellerProfile
                if (profile != null) Result.success(profile)
                else Result.failure(Exception("Empty profile in response"))
            } else {
                Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
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

    fun getSavedSellerId(): String? = TokenManager.getUserId(context)
}
