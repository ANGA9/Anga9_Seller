package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.SellerOrderResponse
import com.anga9.seller.network.model.UpdateOrderStatusRequest
import com.anga9.seller.utils.AppFormatters
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * OrderRepository — Seller App (Phase 3D)
 *
 * Endpoints:
 *   GET   /api/orders/seller              → list seller orders
 *   GET   /api/orders/seller/:orderId     → order detail
 *   PATCH /api/orders/:id/status          → update order status
 *
 * Offline caching enabled: Retains orders on disk so connection loss
 * does not cause blank screen or raw network exceptions.
 */
class OrderRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("seller_orders_cache", Context.MODE_PRIVATE)

    /** Get all orders for this seller. Optionally filter by status. */
    fun getSellerOrders(statusFilter: String = "all"): Flow<Resource<List<SellerOrderResponse>>> = flow {
        val sellerId = TokenManager.getEffectiveSellerId(context)
            ?: TokenManager.getUserId(context)
            ?: "default"
        val cacheKey = "orders_${sellerId}_$statusFilter"

        // 1. Emit cached orders if present
        var cachedOrders: List<SellerOrderResponse>? = null
        val cachedJson = prefs.getString(cacheKey, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<SellerOrderResponse>>() {}.type
                cachedOrders = gson.fromJson(cachedJson, type)
                if (!cachedOrders.isNullOrEmpty()) {
                    emit(Resource.Success(cachedOrders))
                }
            } catch (e: Exception) {
                // Ignore cache parsing errors
            }
        }

        if (cachedOrders == null) {
            emit(Resource.Loading())
        }

        try {
            val status = if (statusFilter == "all") null else statusFilter
            val response = apiService.getSellerOrders(
                status = status,
                page = 1,
                limit = 100
            )
            if (response.isSuccessful) {
                val orders = response.body()?.getList() ?: emptyList()

                // Cache fresh orders immediately
                try {
                    prefs.edit().putString(cacheKey, gson.toJson(orders)).apply()
                } catch (e: Exception) {
                    // Ignore cache write error
                }

                // 1. Emit orders instantly so user sees data without waiting
                emit(Resource.Success(orders))

                // 2. Enrich images in background if needed
                if (orders.isNotEmpty() && orders.any { o -> o.items.any { it.productImage.isNullOrEmpty() } }) {
                    try {
                        val currentSellerId = orders[0].sellerId
                        if (currentSellerId.isNotEmpty()) {
                            val productsRes = apiService.getSellerProducts(sellerId = currentSellerId, limit = 100)
                            if (productsRes.isSuccessful) {
                                val products = productsRes.body()?.data ?: emptyList()
                                val imageMap = products.associate { it.id to it.images?.firstOrNull() }
                                var updated = false
                                for (order in orders) {
                                    for (item in order.items) {
                                        if (item.productImage.isNullOrEmpty() && imageMap.containsKey(item.productId)) {
                                            item.productImage = imageMap[item.productId]
                                            updated = true
                                        }
                                    }
                                }
                                if (updated) {
                                    prefs.edit().putString(cacheKey, gson.toJson(orders)).apply()
                                    emit(Resource.Success(orders))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Non-critical background enrichment
                    }
                }
            } else {
                if (cachedOrders != null) {
                    // Keep showing cached data
                    return@flow
                }
                emit(Resource.Error("Failed to load orders: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (cachedOrders == null) {
                emit(Resource.Error(AppFormatters.getHumanErrorMessage(e, "Failed to load orders")))
            }
        }
    }

    /** Get single order detail. */
    fun getOrderDetail(orderId: String): Flow<Resource<SellerOrderResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSellerOrderDetail(orderId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    try {
                        val sellerId = body.sellerId
                        if (sellerId.isNotEmpty()) {
                            val productsRes = apiService.getSellerProducts(sellerId = sellerId, limit = 100)
                            if (productsRes.isSuccessful) {
                                val products = productsRes.body()?.data ?: emptyList()
                                val imageMap = products.associate { it.id to it.images?.firstOrNull() }
                                for (item in body.items) {
                                    if (item.productImage.isNullOrEmpty()) {
                                        item.productImage = imageMap[item.productId]
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Non-critical
                    }
                    emit(Resource.Success(body))
                } else {
                    emit(Resource.Error("Order not found"))
                }
            } else {
                emit(Resource.Error("Failed to load order: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppFormatters.getHumanErrorMessage(e, "Failed to load order details")))
        }
    }

    /** Update order status. */
    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        trackingNumber: String? = null,
        courierName: String? = null,
        note: String? = null
    ): Result<SellerOrderResponse> {
        return try {
            val response = apiService.updateOrderStatus(
                orderId = orderId,
                request = UpdateOrderStatusRequest(
                    status = status,
                    trackingNumber = trackingNumber,
                    courierName = courierName,
                    note = note
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(AppFormatters.getHumanErrorMessage(e, "Failed to update order status")))
        }
    }
}
