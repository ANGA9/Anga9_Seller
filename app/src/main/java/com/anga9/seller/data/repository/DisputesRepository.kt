package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.DisputeItem
import com.anga9.seller.network.model.DisputeListResponse
import com.anga9.seller.network.model.DisputeRespondRequest
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DisputesRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    /**
     * Fetch disputes for the current verified seller.
     * @param status optional status filter (e.g., 'open', 'seller_responded', 'resolved_refund', etc.)
     */
    fun getSellerDisputes(status: String? = null, page: Int = 1, limit: Int = 50): Flow<Resource<List<DisputeItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSellerDisputes(
                page = page,
                limit = limit,
                status = if (status.isNullOrBlank() || status == "all") null else status
            )
            if (response.isSuccessful) {
                val body = response.body()
                val list = body?.data ?: emptyList()
                emit(Resource.Success(list))
            } else {
                emit(Resource.Error("Failed to load disputes: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error loading disputes"))
        }
    }

    /**
     * Submit seller response & QC status for a dispute.
     */
    suspend fun respondToDispute(
        orderId: String,
        disputeId: String,
        responseText: String,
        qcStatus: String? = null
    ): Result<DisputeItem> {
        return try {
            val response = apiService.respondToDispute(
                orderId = orderId,
                disputeId = disputeId,
                request = DisputeRespondRequest(
                    sellerResponse = responseText.trim(),
                    qcStatus = qcStatus
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                val dispute = body?.dispute ?: body?.disputes?.firstOrNull() ?: body?.items?.firstOrNull()
                if (dispute != null) {
                    Result.success(dispute)
                } else {
                    Result.failure(Exception("Empty dispute response"))
                }
            } else {
                val err = response.errorBody()?.string() ?: "Failed to submit response (${response.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
