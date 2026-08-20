package com.anga9.seller.data.model

import com.anga9.seller.network.model.TopProductItem

data class SellerDashboardStats(
    val todayOrders: Int = 0,
    val pendingOrders: Int = 0,
    val totalOrders: Int = 0,
    val activeOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val previousOrders: Int = 0,
    val previousRevenue: Double = 0.0,
    val walletBalance: Double = 0.0,
    val pendingPayout: Double = 0.0,
    val totalProducts: Int = 0,
    val activeProducts: Int = 0,
    val pendingProducts: Int = 0,
    val lowStockProducts: Int = 0,
    val recentOrders: List<RecentOrderItem> = emptyList(),
    val topProducts: List<TopProductItem> = emptyList(),
    val revenueTrend: List<Double> = emptyList(),
    val sellerBadge: String = "new",
    val pendingReturns: Int = 0,
    val openTickets: Int = 0
)

data class RecentOrderItem(
    val orderId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val status: String = "",
    val createdAt: Long = 0L,
    val itemCount: Int = 0
)
