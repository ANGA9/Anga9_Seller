package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

data class DealResponse(
    @SerializedName("deals")
    val deals: List<DealItem>
)

data class DealItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("deal_price")
    val dealPrice: Double,
    @SerializedName("starts_at")
    val startsAt: String,
    @SerializedName("ends_at")
    val endsAt: String,
    @SerializedName("stock_cap")
    val stockCap: Int?,
    @SerializedName("quantity_threshold")
    val quantityThreshold: Int,
    @SerializedName("active")
    val isActive: Boolean,
    @SerializedName("products")
    val products: DealProductInfo?,
    @SerializedName("product")
    val productObj: DealProductInfo?
) {
    val displayProduct: DealProductInfo? get() = products ?: productObj
    /**
     * Determines the current status: Active, Scheduled, Expired, or Paused.
     */
    val currentStatus: DealStatus
        get() {
            if (!isActive) return DealStatus.PAUSED
            
            val now = java.util.Date()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            
            return try {
                val start = format.parse(startsAt)
                val end = format.parse(endsAt)
                when {
                    now.before(start) -> DealStatus.SCHEDULED
                    now.after(end) -> DealStatus.EXPIRED
                    else -> DealStatus.ACTIVE
                }
            } catch (e: Exception) {
                DealStatus.ACTIVE
            }
        }
}

data class DealProductInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown Product"
}

enum class DealStatus {
    ACTIVE,
    SCHEDULED,
    EXPIRED,
    PAUSED
}

data class CreateDealRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("type") val type: String,
    @SerializedName("deal_price") val dealPrice: Double,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    @SerializedName("quantity_threshold") val quantityThreshold: Int = 1
)

data class UpdateDealRequest(
    @SerializedName("active") val active: Boolean
)
