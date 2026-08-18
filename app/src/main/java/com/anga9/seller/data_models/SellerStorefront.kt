package com.anga9.seller.data_models

/**
 * Seller Storefront - customer app mein dikhne wala seller shop page data
 * Firestore: sellers/{sellerId} se read hota hai
 */
data class SellerStorefront(
    val sellerId: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val businessDescription: String = "",
    val businessType: String = "",
    val city: String = "",
    val state: String = "",
    val profilePhotoUrl: String? = null,
    val bannerImageUrl: String? = null,
    val badgeType: String = "new",
    val gstNumber: String = "",
    val totalProducts: Int = 0,
    val totalOrders: Int = 0,
    val avgRating: Float = 0f,
    val totalReviews: Int = 0,
    val memberSince: Long = 0L,
    val isStorefrontEnabled: Boolean = true,
    val storefrontBio: String = "",
    val certifications: List<String> = emptyList(),  // FSSAI, ISO, etc.
    val responseTimeHours: Int = 24,
    val whatsappNumber: String = "",
    val businessEmail: String = ""
)

/**
 * Repeat Buyer - seller ke orders se aggregate kiya hua buyer data
 * Koi alag collection nahi - orders se calculate hota hai
 */
data class RepeatBuyer(
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val buyerEmail: String = "",
    val totalOrders: Int = 0,
    val totalOrderValue: Double = 0.0,
    val lastOrderDate: Long = 0L,
    val lastOrderId: String = "",
    val outstandingAmount: Double = 0.0,   // COD delivered but not marked paid
    val pendingOrdersCount: Int = 0,
    val firstOrderDate: Long = 0L,
    val avgOrderValue: Double = 0.0,
    val isRegular: Boolean = false          // 3+ orders = regular buyer
)
