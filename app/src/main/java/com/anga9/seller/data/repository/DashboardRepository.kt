package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data.model.RecentOrderItem
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.Resource
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
 */
class DashboardRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    fun getDashboardStats(period: String = "30d"): Flow<Resource<SellerDashboardStats>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Fetch Analytics for the selected period
            val analyticsRes = try {
                apiService.getSellerAnalytics(period = period)
            } catch (e: Exception) { null }
            val analytics = analyticsRes?.body()

            // 2. Fetch Seller Stats
            val statsRes = try {
                apiService.getSellerStats()
            } catch (e: Exception) { null }
            val stats = statsRes?.body()

            // 3. Fetch Earnings / Wallet
            val earningsRes = try {
                apiService.getSellerEarnings()
            } catch (e: Exception) { null }
            val earnings = earningsRes?.body()

            // 4. Fetch Recent Orders
            val ordersRes = try {
                apiService.getSellerOrders(status = null, page = 1, limit = 5)
            } catch (e: Exception) { null }
            val ordersBody = ordersRes?.body()

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
            } ?: emptyList()

            val revenueTrendPoints = analytics?.revenueChart?.map { it.revenue } ?: emptyList()
            val topProducts = analytics?.topProducts?.take(3) ?: emptyList()

            val activeProductsCount = if (stats?.activeProducts != null && stats.activeProducts > 0) {
                stats.activeProducts
            } else {
                stats?.totalProducts ?: 0
            }

            val toFulfillCount = stats?.pendingOrders ?: analytics?.activeOrders ?: 0
            val totalRevenueVal = analytics?.totalRevenue ?: stats?.totalEarnings ?: 0.0
            val totalOrdersVal = analytics?.totalOrders ?: stats?.totalOrders ?: 0

            val dashboardStats = SellerDashboardStats(
                todayOrders = if (period == "today") totalOrdersVal else 0,
                pendingOrders = toFulfillCount,
                totalOrders = totalOrdersVal,
                activeOrders = analytics?.activeOrders ?: 0,
                totalRevenue = totalRevenueVal,
                previousOrders = 0,
                previousRevenue = 0.0,
                walletBalance = earnings?.available ?: stats?.totalEarnings ?: 0.0,
                pendingPayout = earnings?.pending ?: stats?.pendingPayout ?: 0.0,
                totalProducts = stats?.totalProducts ?: 0,
                activeProducts = activeProductsCount,
                pendingProducts = 0,
                lowStockProducts = 0,
                recentOrders = recentOrders,
                topProducts = topProducts,
                revenueTrend = revenueTrendPoints,
                sellerBadge = if (stats?.rating != null && stats.rating >= 4.5) "top_seller" else "verified",
                pendingReturns = stats?.pendingReturns ?: 0,
                openTickets = stats?.openTickets ?: 0
            )

            emit(Resource.Success(dashboardStats))
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }
}
