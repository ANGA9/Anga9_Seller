package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.BulkStockUpdateRequest
import com.anga9.seller.network.model.InventoryResponse
import com.anga9.seller.network.model.StockUpdateItem
import com.anga9.seller.network.model.UpdateStockRequest
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * InventoryRepository — Seller App (Phase 3C)
 *
 * Endpoints (verified from inventory-service/src/routes/inventory.routes.ts):
 *   GET  /api/inventory/:productId          → get stock for a product
 *   PATCH /api/inventory/:productId         → update stock
 *   GET  /api/inventory/low-stock           → low stock alerts
 *   POST /api/inventory/bulk-update         → bulk stock update
 *
 * Firebase has been removed. All data comes from the ANGA9 backend.
 */
class InventoryRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    fun getStock(productId: String): Flow<Resource<InventoryResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStock(productId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) emit(Resource.Success(body))
                else emit(Resource.Error("Stock data not found"))
            } else {
                emit(Resource.Error("Failed to get stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    suspend fun updateStock(productId: String, newStock: Int, reason: String? = null): Result<InventoryResponse> {
        return try {
            val response = apiService.updateStock(
                productId = productId,
                request = UpdateStockRequest(stock = newStock, reason = reason)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to update stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    fun getLowStockProducts(): Flow<Resource<List<InventoryResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getLowStockProducts()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to get low stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    suspend fun bulkUpdateStock(updates: List<StockUpdateItem>): Result<Boolean> {
        return try {
            val response = apiService.bulkUpdateStock(BulkStockUpdateRequest(updates))
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("Bulk update failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
