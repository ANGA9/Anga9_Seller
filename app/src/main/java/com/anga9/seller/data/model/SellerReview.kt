package com.anga9.seller.data.model

data class SellerReview(
    val reviewId: String = "",
    val productId: String = "",
    val productName: String = "",
    val sellerId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val reviewText: String = "",
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
