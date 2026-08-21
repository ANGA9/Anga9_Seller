package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.*
import com.anga9.seller.utils.AppFormatters
import com.anga9.seller.utils.Resource
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * WalletRepository — Seller App (Phase 3E)
 *
 * Endpoints (verified from user-service/src/routes/user.routes.ts):
 *   GET  /api/seller/earnings         → balance summary
 *   GET  /api/seller/earnings/history → transaction history
 *   GET  /api/seller/payouts          → payout request list
 *   POST /api/seller/payouts/request  → request payout
 *
 * Offline caching enabled: Prevents blank wallet screens on disconnect.
 */
class WalletRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("seller_wallet_cache", Context.MODE_PRIVATE)

    /** Get seller earnings summary (balance, pending, withdrawn). */
    fun getEarnings(): Flow<Resource<SellerEarningsResponse>> = flow {
        val cachedJson = prefs.getString("cached_earnings", null)
        var cachedEarnings: SellerEarningsResponse? = null
        if (!cachedJson.isNullOrBlank()) {
            try {
                cachedEarnings = gson.fromJson(cachedJson, SellerEarningsResponse::class.java)
                if (cachedEarnings != null) emit(Resource.Success(cachedEarnings))
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (cachedEarnings == null) {
            emit(Resource.Loading())
        }

        try {
            val response = apiService.getSellerEarnings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    try {
                        prefs.edit().putString("cached_earnings", gson.toJson(body)).apply()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    emit(Resource.Success(body))
                } else if (cachedEarnings == null) {
                    emit(Resource.Error("Empty response"))
                }
            } else {
                if (cachedEarnings == null) {
                    emit(Resource.Error("Failed to get earnings: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            if (cachedEarnings == null) {
                emit(Resource.Error(AppFormatters.getHumanErrorMessage(e, "Failed to get earnings")))
            }
        }
    }

    /** Get earnings transaction history. */
    fun getEarningsHistory(page: Int = 1): Flow<Resource<EarningHistoryResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEarningsHistory(page = page, limit = 50)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) emit(Resource.Success(body))
                else emit(Resource.Error("Empty response"))
            } else {
                emit(Resource.Error("Failed to get history: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppFormatters.getHumanErrorMessage(e, "Failed to get history")))
        }
    }

    /** Get payout requests list. */
    fun getPayouts(page: Int = 1): Flow<Resource<PayoutListResponse>> = flow {
        val cachedJson = prefs.getString("cached_payouts", null)
        var cachedPayouts: PayoutListResponse? = null
        if (!cachedJson.isNullOrBlank()) {
            try {
                cachedPayouts = gson.fromJson(cachedJson, PayoutListResponse::class.java)
                if (cachedPayouts != null) emit(Resource.Success(cachedPayouts))
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (cachedPayouts == null) {
            emit(Resource.Loading())
        }

        try {
            val response = apiService.getSellerPayouts(page = page, limit = 20)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    try {
                        prefs.edit().putString("cached_payouts", gson.toJson(body)).apply()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    emit(Resource.Success(body))
                } else if (cachedPayouts == null) {
                    emit(Resource.Error("Empty response"))
                }
            } else {
                if (cachedPayouts == null) {
                    emit(Resource.Error("Failed to get payouts: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            if (cachedPayouts == null) {
                emit(Resource.Error(AppFormatters.getHumanErrorMessage(e, "Failed to get payouts")))
            }
        }
    }

    /**
     * Request a payout.
     */
    suspend fun requestPayout(
        amount: Double,
        bankAccountNumber: String = "",
        bankIfsc: String = "",
        bankAccountName: String = ""
    ): Result<SellerPayoutResponse> {
        return try {
            val response = apiService.requestPayout(
                PayoutRequestBody(
                    amount = amount,
                    bankAccountNumber = bankAccountNumber,
                    bankIfsc = bankIfsc,
                    bankAccountName = bankAccountName
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Payout request failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(AppFormatters.getHumanErrorMessage(e, "Payout request failed")))
        }
    }
}
