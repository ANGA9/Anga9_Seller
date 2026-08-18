package com.anga9.seller.data.model

// Same model as admin - same Firestore collection "seller_payouts"
data class SellerPayout(
    val payoutId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val sellerPhone: String = "",
    val sellerGSTIN: String = "",

    // Seller Category (Tiered Payout System)
    val sellerCategory: String = "NEW",
    val payoutDays: Int = 3,

    // Bank Details
    val bankAccountNumber: String = "",
    val bankAccountHolderName: String = "",
    val bankIFSC: String = "",
    val bankName: String = "",
    val bankBranch: String = "",
    val upiId: String = "",

    // Payout Details
    val payoutAmount: Double = 0.0,
    val payoutCurrency: String = "INR",
    val ordersIncluded: List<String> = emptyList(),
    val revenuesIncluded: List<String> = emptyList(),
    val orderCount: Int = 0,

    // Breakdown
    val totalProductAmount: Double = 0.0,
    val totalPlatformFees: Double = 0.0,
    val totalDeliveryFees: Double = 0.0,
    val totalGST: Double = 0.0,

    // Payment Method
    val paymentMethod: String = "BANK_TRANSFER",
    val paymentMode: String = "NEFT",
    val transactionId: String = "",
    val utrNumber: String = "",
    val referenceNumber: String = "",

    // Status
    val payoutStatus: String = "PENDING",
    val failureReason: String = "",
    val retryCount: Int = 0,

    // Timestamps
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long = 0,
    val processedAt: Long = 0,
    val completedAt: Long = 0,
    val failedAt: Long = 0,
    val cancelledAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Admin Actions
    val requestedBy: String = "",
    val approvedBy: String = "",
    val adminNotes: String = "",
    val rejectionReason: String = "",

    // Compliance
    val kycVerified: Boolean = false,
    val gstVerified: Boolean = false,
    val bankVerified: Boolean = false,
    val tdsApplicable: Boolean = false,
    val tdsPercent: Double = 0.0,
    val tdsAmount: Double = 0.0,
    val tdsDeducted: Boolean = false,

    // Status History
    val statusHistory: List<PayoutStatusUpdate> = emptyList()
) {
    fun getNetPayoutAmount(): Double =
        if (tdsApplicable && tdsDeducted) payoutAmount - tdsAmount else payoutAmount

    fun formatCurrency(amount: Double): String = "₹${String.format("%,.2f", amount)}"

    fun getFormattedPayoutAmount(): String = formatCurrency(payoutAmount)
    fun getFormattedNetAmount(): String = formatCurrency(getNetPayoutAmount())

    fun getStatusColor(): Int = when (payoutStatus) {
        "PENDING" -> 0xFFFFA500.toInt()
        "APPROVED" -> 0xFF2196F3.toInt()
        "PROCESSING" -> 0xFF9C27B0.toInt()
        "COMPLETED" -> 0xFF4CAF50.toInt()
        "FAILED" -> 0xFFF44336.toInt()
        "CANCELLED" -> 0xFF757575.toInt()
        else -> 0xFF000000.toInt()
    }
}

data class PayoutStatusUpdate(
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedBy: String = "",
    val notes: String = "",
    val metadata: Map<String, String> = emptyMap()
)

object SellerPayoutStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val PROCESSING = "PROCESSING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}
