package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

data class AdListResponse(
    @SerializedName("ads") val ads: List<AdCampaignResponse>
)

data class AdCampaignResponse(
    @SerializedName("id") val id: String,
    @SerializedName("seller_id") val sellerId: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("placement") val placement: String,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    @SerializedName("banner_url") val bannerUrl: String,
    @SerializedName("headline") val headline: String,
    @SerializedName("cta_text") val ctaText: String?,
    @SerializedName("budget_inr") val budgetInr: Double,
    @SerializedName("approved_fee_inr") val approvedFeeInr: Double?,
    @SerializedName("status") val status: String,
    @SerializedName("reject_reason") val rejectReason: String?,
    @SerializedName("impressions") val impressions: Long,
    @SerializedName("clicks") val clicks: Long,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("products") val products: AdProductInfo?
)

data class AdProductInfo(
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

data class AdRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("placement") val placement: String,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    @SerializedName("banner_url") val bannerUrl: String,
    @SerializedName("headline") val headline: String,
    @SerializedName("cta_text") val ctaText: String,
    @SerializedName("budget_inr") val budgetInr: Double
)
