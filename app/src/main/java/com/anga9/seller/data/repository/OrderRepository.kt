package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.SellerOrderListResponse
import com.anga9.seller.network.model.SellerOrderResponse
import com.anga9.seller.network.model.UpdateOrderStatusRequest
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * OrderRepository — Seller App (Phase 3D)
 *
 * Endpoints (verified from order-service/src/routes/order.routes.ts):
 *   GET   /api/orders/seller              → list seller orders
 *   GET   /api/orders/seller/:orderId     → order detail
 *   PATCH /api/orders/:id/status          → update order status
 *
 * Firebase real-time listeners have been replaced with Retrofit calls.
 * For real-time updates, use periodic polling or WebSocket (future).
 */
class OrderRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    /** Get all orders for this seller. Optionally filter by status. */
    fun getSellerOrders(statusFilter: String = "all"): Flow<Resource<List<SellerOrderResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val status = if (statusFilter == "all") null else statusFilter
            android.util.Log.d("OrderRepo", "Fetching seller orders, status=$status")
            val response = apiService.getSellerOrders(
                status = status,
                page = 1,
                limit = 100
            )
            android.util.Log.d("OrderRepo", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d("OrderRepo", "Response body null? ${body == null}")
                if (body != null) {
                    android.util.Log.d("OrderRepo", "body.orders size: ${body.orders?.size}, body.data size: ${body.data?.size}")
                    android.util.Log.d("OrderRepo", "getList() size: ${body.getList().size}")
                }
                val orders = body?.getList() ?: emptyList()
                android.util.Log.d("OrderRepo", "Final orders count: ${orders.size}")
                if (orders.isNotEmpty()) {
                    val first = orders[0]
                    android.util.Log.d("OrderRepo", "First order: id=${first.id}, status=${first.status}, items=${first.items.size}, orderNumber=${first.orderNumber}")
                    
                    // -- Fetch products to enrich images since backend doesn't send them --
                    try {
                        val sellerId = first.sellerId
                        if (sellerId.isNotEmpty()) {
                            val productsRes = apiService.getSellerProducts(sellerId = sellerId, limit = 100)
                            if (productsRes.isSuccessful) {
                                val products = productsRes.body()?.data ?: emptyList()
                                val imageMap = products.associate { it.id to it.images?.firstOrNull() }
                                for (order in orders) {
                                    for (item in order.items) {
                                        if (item.productImage.isNullOrEmpty()) {
                                            item.productImage = imageMap[item.productId]
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OrderRepo", "Failed to enrich product images", e)
                    }
                }
                emit(Resource.Success(orders))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("OrderRepo", "Error ${response.code()}: $errorBody")
                emit(Resource.Error("Failed to load orders: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepo", "Network error", e)
            emit(Resource.Error("Network error: ${e.message}"))
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
                    // -- Fetch products to enrich images --
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
                        android.util.Log.e("OrderRepo", "Failed to enrich product images", e)
                    }
                    emit(Resource.Success(body))
                } else {
                    emit(Resource.Error("Order not found"))
                }
            } else {
                emit(Resource.Error("Failed to get order: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    /**
     * Update order status.
     * Valid transitions: PENDING → CONFIRMED → SHIPPED → DELIVERED
     * @param status One of: CONFIRMED, SHIPPED, DELIVERED, CANCELLED
     * @param trackingNumber Required when status = SHIPPED
     * @param courierName Required when status = SHIPPED
     */
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
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
