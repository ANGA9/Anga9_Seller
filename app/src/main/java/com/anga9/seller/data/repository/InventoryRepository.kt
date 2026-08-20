package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.BulkStockUpdateRequest
import com.anga9.seller.network.model.InventoryResponse
import com.anga9.seller.network.model.StockUpdateItem
import com.anga9.seller.network.model.UpdateStockRequest
import com.anga9.seller.utils.Resource
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * InventoryRepository — Seller App
 *
 * Endpoints:
 *   GET  /api/inventory/:productId          → get stock for a product (returns Array or Object)
 *   PATCH /api/inventory/:productId         → update stock
 *   GET  /api/inventory/low-stock           → low stock alerts
 *   POST /api/inventory/bulk-update         → bulk stock update
 */
class InventoryRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val gson = Gson()

    fun getStock(productId: String): Flow<Resource<InventoryResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStock(productId)
            if (response.isSuccessful) {
                val json = response.body()
                if (json != null) {
                    val stockObj: InventoryResponse? = when {
                        json.isJsonArray && json.asJsonArray.size() > 0 -> {
                            gson.fromJson(json.asJsonArray[0], InventoryResponse::class.java)
                        }
                        json.isJsonObject -> {
                            gson.fromJson(json.asJsonObject, InventoryResponse::class.java)
                        }
                        else -> null
                    }
                    if (stockObj != null) {
                        emit(Resource.Success(stockObj))
                    } else {
                        emit(Resource.Success(InventoryResponse(productId = productId, quantity = 0)))
                    }
                } else {
                    emit(Resource.Success(InventoryResponse(productId = productId, quantity = 0)))
                }
            } else {
                emit(Resource.Error("Failed to get stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    suspend fun updateStock(
        productId: String,
        newQuantity: Int,
        threshold: Int = 10,
        reason: String? = null
    ): Result<InventoryResponse> {
        return try {
            val response = apiService.updateStock(
                productId = productId,
                request = UpdateStockRequest(
                    quantity = newQuantity,
                    lowStockThreshold = threshold,
                    lowStockThresholdCamel = threshold,
                    stock = newQuantity,
                    reason = reason
                )
            )
            if (response.isSuccessful) {
                val json = response.body()
                val stockObj: InventoryResponse? = when {
                    json != null && json.isJsonArray && json.asJsonArray.size() > 0 -> {
                        gson.fromJson(json.asJsonArray[0], InventoryResponse::class.java)
                    }
                    json != null && json.isJsonObject -> {
                        gson.fromJson(json.asJsonObject, InventoryResponse::class.java)
                    }
                    else -> null
                }
                Result.success(
                    stockObj ?: InventoryResponse(
                        productId = productId,
                        quantity = newQuantity,
                        stock = newQuantity,
                        lowStockThreshold = threshold
                    )
                )
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
