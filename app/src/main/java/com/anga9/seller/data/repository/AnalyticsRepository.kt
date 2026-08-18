package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data.model.*
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AnalyticsRepository — Seller App (Phase 3F)
 *
 * Backend has no dedicated seller analytics endpoint.
 * Data is calculated client-side from:
 *   GET /api/users/seller-stats   → basic stats
 *   GET /api/orders/seller        → orders list (for revenue calculation)
 *   GET /api/seller/earnings      → earnings data
 *
 * Firebase has been removed. All data comes from the ANGA9 backend.
 * TODO Phase 6: Add GET /api/seller/analytics?period=30d to backend.
 */
class AnalyticsRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    fun getAnalyticsSummary(period: AnalyticsPeriod): Flow<Resource<SellerAnalyticsSummary>> = flow {
        emit(Resource.Loading())
        try {
            val statsResp = apiService.getSellerStats()
            val stats = if (statsResp.isSuccessful) statsResp.body() else null

            val earningsResp = apiService.getSellerEarnings()
            val earnings = if (earningsResp.isSuccessful) earningsResp.body() else null

            val ordersResp = apiService.getSellerOrders(status = null, page = 1, limit = 200)
            val orders = if (ordersResp.isSuccessful) ordersResp.body()?.orders ?: emptyList() else emptyList()

            val deliveredOrders = orders.filter { it.status == "DELIVERED" }
            val totalRevenue = deliveredOrders.sumOf { it.totalAmount }
            val avgOrderValue = if (deliveredOrders.isNotEmpty()) totalRevenue / deliveredOrders.size else 0.0
            val fulfillmentRate = if (orders.isNotEmpty()) {
                (deliveredOrders.size.toDouble() / orders.size) * 100
            } else 0.0

            emit(Resource.Success(SellerAnalyticsSummary(
                totalRevenue = totalRevenue,
                totalOrders = stats?.totalOrders ?: orders.size,
                avgOrderValue = avgOrderValue,
                totalEarnings = earnings?.total ?: 0.0,
                pendingEarnings = earnings?.pending ?: 0.0,
                paidEarnings = earnings?.paid ?: 0.0,
                fulfillmentRate = fulfillmentRate,
                returnRate = 0.0,
                totalProductsSold = deliveredOrders.sumOf { it.items.sumOf { item -> item.quantity } },
                activeProducts = stats?.totalProducts ?: 0,
                gst5Amount = 0.0,
                gst12Amount = 0.0,
                gst18Amount = 0.0,
                platformFeesTotal = 0.0,
                netEarnings = earnings?.total ?: 0.0
            )))
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    fun getRevenueChart(period: AnalyticsPeriod): Flow<Resource<List<RevenueChartPoint>>> = flow {
        emit(Resource.Loading())
        try {
            val ordersResp = apiService.getSellerOrders(status = "DELIVERED", page = 1, limit = 200)
            val orders = if (ordersResp.isSuccessful) ordersResp.body()?.orders ?: emptyList() else emptyList()

            // Group by date — client-side aggregation
            val grouped = orders.groupBy { order ->
                order.createdAt?.take(10) ?: "Unknown"
            }
            val chartPoints = grouped.entries.take(period.days).map { (date, dayOrders) ->
                RevenueChartPoint(
                    label = date.takeLast(5), // MM-DD
                    amount = dayOrders.sumOf { it.totalAmount },
                    orderCount = dayOrders.size,
                    timestamp = 0L
                )
            }.sortedBy { it.label }

            emit(Resource.Success(chartPoints))
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    fun getTopProducts(period: AnalyticsPeriod): Flow<Resource<List<TopProduct>>> = flow {
        emit(Resource.Loading())
        try {
            val ordersResp = apiService.getSellerOrders(status = "DELIVERED", page = 1, limit = 200)
            val orders = if (ordersResp.isSuccessful) ordersResp.body()?.orders ?: emptyList() else emptyList()

            // Aggregate product sales client-side
            val productMap = mutableMapOf<String, Triple<String, Int, Double>>()
            orders.forEach { order ->
                order.items.forEach { item ->
                    val existing = productMap[item.productId]
                    if (existing != null) {
                        productMap[item.productId] = Triple(
                            item.productName,
                            existing.second + item.quantity,
                            existing.third + (item.price * item.quantity)
                        )
                    } else {
                        productMap[item.productId] = Triple(
                            item.productName,
                            item.quantity,
                            item.price * item.quantity
                        )
                    }
                }
            }

            val topProducts = productMap.entries
                .sortedByDescending { it.value.second }
                .take(10)
                .map { (productId, data) ->
                    TopProduct(
                        productId = productId,
                        productName = data.first,
                        imageUrl = "",
                        totalSold = data.second,
                        totalRevenue = data.third,
                        avgRating = 0.0
                    )
                }

            emit(Resource.Success(topProducts))
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    fun getCategoryRevenue(period: AnalyticsPeriod): Flow<Resource<List<CategoryRevenue>>> = flow {
        emit(Resource.Loading())
        // Category revenue requires product category data joined with order items
        // Full implementation needs Phase 6 backend endpoint
        emit(Resource.Success(emptyList()))
    }
}