package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data.model.RecentOrderItem
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * DashboardRepository — Seller App
 *
 * Parallel backend data fetching from:
 *   - GET /api/users/seller-stats
 *   - GET /api/users/seller-analytics?period={period}
 *   - GET /api/seller/earnings
 *   - GET /api/orders/seller
 *
 * Features:
 *   - Offline caching: Persists latest dashboard numbers so loss of connection
 *     never replaces real data with 0s or empty widgets.
 */
class DashboardRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("seller_dashboard_cache", Context.MODE_PRIVATE)

    fun getDashboardStats(period: String = "30d"): Flow<Resource<SellerDashboardStats>> = flow {
        val sellerId = TokenManager.getEffectiveSellerId(context)
            ?: TokenManager.getUserId(context)
            ?: "default"
        val cacheKey = "stats_${sellerId}_$period"

        // 1. Emit cached stats immediately if available
        var cachedStats: SellerDashboardStats? = null
        val cachedJson = prefs.getString(cacheKey, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                cachedStats = gson.fromJson(cachedJson, SellerDashboardStats::class.java)
                if (cachedStats != null) {
                    emit(Resource.Success(cachedStats))
                }
            } catch (e: Exception) {
                // Ignore corrupted cache
            }
        }

        if (cachedStats == null) {
            emit(Resource.Loading())
        }

        // 2. Fetch fresh data from network in PARALLEL for maximum speed
        try {
            var anyCallSucceeded = false

            var analytics: com.anga9.seller.network.model.SellerAnalyticsResponse? = null
            var stats: com.anga9.seller.network.model.SellerStatsResponse? = null
            var earnings: com.anga9.seller.network.model.SellerEarningsResponse? = null
            var ordersBody: com.anga9.seller.network.model.SellerOrderListResponse? = null

            kotlinx.coroutines.coroutineScope {
                val analyticsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val res = apiService.getSellerAnalytics(period = period)
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }
                val statsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val res = apiService.getSellerStats()
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }
                val earningsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val res = apiService.getSellerEarnings()
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }
                val ordersDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val res = apiService.getSellerOrders(status = null, page = 1, limit = 5)
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }

                analytics = analyticsDeferred.await()
                stats = statsDeferred.await()
                earnings = earningsDeferred.await()
                ordersBody = ordersDeferred.await()
            }

            anyCallSucceeded = analytics != null || stats != null || earnings != null || ordersBody != null

            // If ALL network requests failed (e.g. offline / connection lost)
            if (!anyCallSucceeded) {
                if (cachedStats != null) {
                    // Do NOT overwrite valid cached data with zeroes
                    return@flow
                } else {
                    emit(Resource.Error("No internet connection"))
                    return@flow
                }
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val recentOrders = ordersBody?.getList()?.take(3)?.map { order ->
                val parsedTime = try {
                    order.placedAt?.let { isoFormat.parse(it)?.time } ?: 0L
                } catch (e: Exception) { 0L }

                val customerName = if (!order.deliveryAddress?.name.isNullOrBlank()) {
                    order.deliveryAddress!!.name!!
                } else if (!order.customerName.isNullOrBlank()) {
                    order.customerName!!
                } else {
                    order.items.firstOrNull()?.productName ?: "Customer"
                }

                RecentOrderItem(
                    orderId = order.id,
                    orderNumber = order.orderNumber ?: order.id.takeLast(6).uppercase(),
                    customerName = customerName,
                    amount = order.totalAmount,
                    status = order.items.firstOrNull()?.status ?: order.status,
                    createdAt = parsedTime,
                    itemCount = order.items.size
                )
            } ?: cachedStats?.recentOrders ?: emptyList()

            val revenueTrendPoints = analytics?.revenueChart?.map { it.revenue }
                ?: cachedStats?.revenueTrend
                ?: emptyList()

            val topProducts = analytics?.topProducts?.take(3)
                ?: cachedStats?.topProducts
                ?: emptyList()

            val activeProductsCount = if (stats?.activeProducts != null && stats.activeProducts > 0) {
                stats.activeProducts
            } else if (stats?.totalProducts != null) {
                stats.totalProducts
            } else {
                cachedStats?.activeProducts ?: 0
            }

            val toFulfillCount = stats?.pendingOrders
                ?: analytics?.activeOrders
                ?: cachedStats?.pendingOrders
                ?: 0

            val totalRevenueVal = analytics?.totalRevenue
                ?: stats?.totalEarnings
                ?: cachedStats?.totalRevenue
                ?: 0.0

            val totalOrdersVal = analytics?.totalOrders
                ?: stats?.totalOrders
                ?: cachedStats?.totalOrders
                ?: 0

            val freshStats = SellerDashboardStats(
                todayOrders = if (period == "today") totalOrdersVal else cachedStats?.todayOrders ?: 0,
                pendingOrders = toFulfillCount,
                totalOrders = totalOrdersVal,
                activeOrders = analytics?.activeOrders ?: cachedStats?.activeOrders ?: 0,
                totalRevenue = totalRevenueVal,
                previousOrders = cachedStats?.previousOrders ?: 0,
                previousRevenue = cachedStats?.previousRevenue ?: 0.0,
                walletBalance = earnings?.available ?: stats?.totalEarnings ?: cachedStats?.walletBalance ?: 0.0,
                pendingPayout = earnings?.pending ?: stats?.pendingPayout ?: cachedStats?.pendingPayout ?: 0.0,
                totalProducts = stats?.totalProducts ?: cachedStats?.totalProducts ?: 0,
                activeProducts = activeProductsCount,
                pendingProducts = cachedStats?.pendingProducts ?: 0,
                lowStockProducts = cachedStats?.lowStockProducts ?: 0,
                recentOrders = recentOrders,
                topProducts = topProducts,
                revenueTrend = revenueTrendPoints,
                sellerBadge = if (stats?.rating != null && stats.rating >= 4.5) "top_seller" else cachedStats?.sellerBadge ?: "verified",
                pendingReturns = stats?.pendingReturns ?: cachedStats?.pendingReturns ?: 0,
                openTickets = stats?.openTickets ?: cachedStats?.openTickets ?: 0
            )

            // Save fresh stats into cache
            try {
                prefs.edit().putString(cacheKey, gson.toJson(freshStats)).apply()
            } catch (e: Exception) {
                // Ignore caching write errors
            }

            emit(Resource.Success(freshStats))
        } catch (e: Exception) {
            if (cachedStats == null) {
                emit(Resource.Error("Network error: ${e.message}"))
            }
        }
    }
}
