package com.anga9.seller.data.model

data class SellerDispute(
    val disputeId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val sellerId: String = "",
    val orderAmount: Double = 0.0,
    // DEFECTIVE, WRONG_ITEM, QUALITY
    val reason: String = "",
    val description: String = "",
    // PENDING, SELLER_ACCEPTED, SELLER_REJECTED, ADMIN_REVIEW, RESOLVED
    val status: String = "PENDING",
    val photos: List<String> = listOf(),
    // Seller response
    val sellerResponse: String = "",
    // REPLACEMENT, REFUND, REJECTED
    val sellerResolution: String = "",
    // Admin decision
    val adminDecision: String = "",
    val refundAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
