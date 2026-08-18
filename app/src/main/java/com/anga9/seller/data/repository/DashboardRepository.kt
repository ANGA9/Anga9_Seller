package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data.model.RecentOrderItem
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * DashboardRepository — Seller App (Phase 3F)
 *
 * Backend has no dedicated seller analytics endpoint.
 * Data assembled from:
 *   GET /api/users/seller-stats   → basic stats
 *   GET /api/orders/seller        → recent orders
 *
 * Firebase has been removed. All data comes from the ANGA9 backend.
 */
class DashboardRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    fun getDashboardStats(): Flow<Resource<SellerDashboardStats>> = flow {
        emit(Resource.Loading())
        try {
            val statsResponse = apiService.getSellerStats()
            if (!statsResponse.isSuccessful) {
                emit(Resource.Error("Failed to load stats: ${statsResponse.code()}"))
                return@flow
            }
            val stats = statsResponse.body()

            val ordersResponse = apiService.getSellerOrders(status = null, page = 1, limit = 10)
            val recentOrders = if (ordersResponse.isSuccessful) {
                ordersResponse.body()?.orders?.take(5)?.map { order ->
                    RecentOrderItem(
                        orderId = order.id,
                        customerName = order.customerName ?: "Customer",
                        amount = order.totalAmount,
                        status = order.status,
                        createdAt = 0L,   // backend returns ISO string; parse if needed
                        itemCount = order.items.size
                    )
                } ?: emptyList()
            } else emptyList()

            emit(Resource.Success(SellerDashboardStats(
                todayOrders = 0,          // not in seller-stats; calculate from orders if needed
                pendingOrders = stats?.pendingOrders ?: 0,
                totalOrders = stats?.totalOrders ?: 0,
                totalRevenue = stats?.totalEarnings ?: 0.0,
                walletBalance = 0.0,      // fetch separately from WalletRepository
                pendingPayout = stats?.pendingPayout ?: 0.0,
                totalProducts = stats?.totalProducts ?: 0,
                pendingProducts = 0,
                lowStockProducts = 0,
                recentOrders = recentOrders,
                sellerBadge = "new",
                pendingReturns = stats?.pendingReturns ?: 0,
                openTickets = stats?.openTickets ?: 0
            )))
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }
}
