package com.anga9.seller.data.model

/**
 * Analytics data models for Seller App - Phase 7
 * Seller-scoped analytics from revenues collection
 */
data class SellerAnalyticsSummary(
    val totalRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val avgOrderValue: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val pendingEarnings: Double = 0.0,
    val paidEarnings: Double = 0.0,
    val fulfillmentRate: Double = 0.0,
    val returnRate: Double = 0.0,
    val totalProductsSold: Int = 0,
    val activeProducts: Int = 0,
    val gst5Amount: Double = 0.0,
    val gst12Amount: Double = 0.0,
    val gst18Amount: Double = 0.0,
    val platformFeesTotal: Double = 0.0,
    val netEarnings: Double = 0.0
)

data class RevenueChartPoint(
    val label: String = "",        // "Mon", "Jan", "Week 1"
    val amount: Double = 0.0,
    val orderCount: Int = 0,
    val timestamp: Long = 0L
)

data class TopProduct(
    val productId: String = "",
    val productName: String = "",
    val imageUrl: String = "",
    val totalSold: Int = 0,
    val totalRevenue: Double = 0.0,
    val avgRating: Double = 0.0
)

data class CategoryRevenue(
    val categoryName: String = "",
    val revenue: Double = 0.0,
    val orderCount: Int = 0,
    val percentage: Float = 0f
)

data class GstBreakdown(
    val gst5: Double = 0.0,
    val gst12: Double = 0.0,
    val gst18: Double = 0.0,
    val totalGst: Double = 0.0,
    val taxableAmount: Double = 0.0
)

enum class AnalyticsPeriod(val label: String, val days: Int) {
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    QUARTER("3 Months", 90),
    YEAR("This Year", 365)
}
