package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.EarningHistoryResponse
import com.anga9.seller.network.model.PayoutListResponse
import com.anga9.seller.network.model.PayoutRequestBody
import com.anga9.seller.network.model.SellerEarningsResponse
import com.anga9.seller.network.model.SellerPayoutResponse
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * WalletRepository — Seller App (Phase 3E)
 *
 * Endpoints (verified from payment-service/src/routes/payment.routes.ts):
 * Gateway proxy: /api/seller/earnings → payment-service:4008
 *
 *   GET  /api/seller/earnings             → earnings summary
 *   GET  /api/seller/earnings/history     → transaction history
 *   GET  /api/seller/payouts              → payout list
 *   POST /api/seller/payouts/request      → request payout
 *
 * Firebase has been removed. All data comes from the ANGA9 backend.
 */
class WalletRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    /** Get seller earnings summary (balance, pending, withdrawn). */
    fun getEarnings(): Flow<Resource<SellerEarningsResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSellerEarnings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) emit(Resource.Success(body))
                else emit(Resource.Error("Empty response"))
            } else {
                emit(Resource.Error("Failed to get earnings: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
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
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    /** Get payout requests list. */
    fun getPayouts(page: Int = 1): Flow<Resource<PayoutListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSellerPayouts(page = page, limit = 20)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) emit(Resource.Success(body))
                else emit(Resource.Error("Empty response"))
            } else {
                emit(Resource.Error("Failed to get payouts: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    /**
     * Request a payout.
     * @param amount Amount to withdraw (must be <= available balance)
     * @param bankAccountNumber Seller's bank account number
     * @param bankIfsc IFSC code
     * @param bankAccountName Account holder name
     */
    suspend fun requestPayout(
        amount: Double,
        bankAccountNumber: String? = null,
        bankIfsc: String? = null,
        bankAccountName: String? = null
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
                val errorMsg = when (response.code()) {
                    400 -> "Insufficient balance or invalid amount"
                    422 -> "Bank details required for payout"
                    else -> "Payout request failed: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
