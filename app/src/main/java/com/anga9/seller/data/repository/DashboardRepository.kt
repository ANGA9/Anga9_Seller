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
            var productsBody: com.anga9.seller.network.model.ProductListResponse? = null

            kotlinx.coroutines.coroutineScope {
                val analyticsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val backendPeriod = if (period == "today") "7d" else period
                        val res = apiService.getSellerAnalytics(period = backendPeriod)
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
                        val res = apiService.getSellerOrders(status = null, page = 1, limit = 50)
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }
                val productsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val res = apiService.getSellerProducts(sellerId = sellerId, status = "active", limit = 1)
                        if (res.isSuccessful) res.body() else null
                    } catch (e: Exception) { null }
                }

                analytics = analyticsDeferred.await()
                stats = statsDeferred.await()
                earnings = earningsDeferred.await()
                ordersBody = ordersDeferred.await()
                productsBody = productsDeferred.await()
            }

            anyCallSucceeded = analytics != null || stats != null || earnings != null || ordersBody != null || productsBody != null

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

            val recentOrders = ordersBody?.getList()?.take(5)?.map { order ->
                val parsedTime = try {
                    order.placedAt?.let { isoFormat.parse(it)?.time }
                        ?: order.createdAt?.let { isoFormat.parse(it)?.time }
                        ?: 0L
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
                    amount = order.getSellerTotal(),
                    status = order.getEffectiveStatus(),
                    createdAt = parsedTime,
                    itemCount = order.items.size
                )
            } ?: cachedStats?.recentOrders ?: emptyList()

            val topProducts = analytics?.topProducts?.take(5)
                ?: cachedStats?.topProducts
                ?: emptyList()

            val activeProductsCount = productsBody?.total
                ?: productsBody?.getList()?.size
                ?: stats?.activeProducts
                ?: cachedStats?.activeProducts
                ?: 0

            val toFulfillCount = ordersBody?.getList()?.count { o ->
                val s = o.getEffectiveStatus().lowercase()
                s == "confirmed" || s == "processing" || s == "pending"
            } ?: stats?.pendingOrders ?: cachedStats?.pendingOrders ?: 0

            val (totalOrdersVal, totalRevenueVal, revenueTrendPoints) = when (period) {
                "today" -> {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val startOfDay = cal.timeInMillis

                    val todayOrdersList = ordersBody?.getList()?.filter { order ->
                        val t = try {
                            order.placedAt?.let { isoFormat.parse(it)?.time }
                                ?: order.createdAt?.let { isoFormat.parse(it)?.time }
                                ?: 0L
                        } catch (e: Exception) { 0L }
                        t >= startOfDay
                    } ?: emptyList()

                    val rev = todayOrdersList.sumOf { it.getSellerTotal() }
                    val count = todayOrdersList.size
                    Triple(count, rev, listOf(0.0, rev))
                }
                "7d" -> {
                    val count = analytics?.totalOrders ?: 0
                    val rev = analytics?.totalRevenue ?: 0.0
                    val trend = analytics?.revenueChart?.map { it.revenue } ?: emptyList()
                    Triple(count, rev, trend)
                }
                else -> { // "30d"
                    val count = ordersBody?.total
                        ?: ordersBody?.getList()?.size
                        ?: analytics?.totalOrders
                        ?: 0
                    val rev = analytics?.totalRevenue ?: stats?.totalEarnings ?: 0.0
                    val trend = analytics?.revenueChart?.map { it.revenue } ?: emptyList()
                    Triple(count, rev, trend)
                }
            }

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
