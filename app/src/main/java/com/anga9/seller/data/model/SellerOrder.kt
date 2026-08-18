package com.anga9.seller.data.model

/**
 * Seller-scoped Order model - mirrors admin Order.kt
 * Seller sirf apne orders dekhta hai (sellerId filter se)
 */
data class SellerOrder(
    var orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val sellerId: String = "",
    val sellerName: String = "",

    // Items
    val items: List<SellerOrderItem> = listOf(),

    // Pricing
    val itemsTotal: Double = 0.0,
    val bulkDiscount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val deliveryCharges: Double = 0.0,
    val totalAmount: Double = 0.0,

    // B2B Payment
    val paymentMethod: String = "COD",
    val paymentStatus: String = "PENDING",
    val paymentTerms: String = "COD",
    val poNumber: String = "",

    // Delivery
    val deliveryAddress: String = "",
    val trackingNumber: String = "",
    val courierName: String = "",
    val estimatedDeliveryDays: Int = 5,
    // B2B Delivery Instructions
    val deliveryAddressType: String = "",
    val saturdayDelivery: Boolean = false,
    val sundayDelivery: Boolean = false,
    val deliveryInstructions: String = "",

    // Status
    val orderStatus: String = "pending",
    val statusHistory: List<SellerStatusUpdate> = listOf(),
    val cancelReason: String = "",

    // Invoice
    val invoiceNumber: String = "",
    val invoiceUrl: String = "",
    val gstInvoiceUrl: String = "",

    // Revenue
    val sellerEarnings: Double = 0.0,
    val platformFees: Double = 0.0,
    val payoutEligible: Boolean = false,
    val payoutCompleted: Boolean = false,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long = 0,
    val shippedAt: Long = 0,
    val deliveredAt: Long = 0
)

data class SellerOrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "unit",
    val subtotal: Double = 0.0,
    val bulkDiscountPercent: Int = 0,
    val gstPercent: Int = 18,
    // Set ordering fields
    val orderType: String = "per_piece",
    val setSize: Int = 1,
    val setLabel: String = "",
    // Variant fields
    val hasVariants: Boolean = false,
    val variantSelections: Map<String, Int> = emptyMap(),
    val variantSummary: String = ""
)

data class SellerStatusUpdate(
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedBy: String = "",
    val notes: String = ""
)
