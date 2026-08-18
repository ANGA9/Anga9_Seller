package com.anga9.seller.data.model

// Same model as admin - same Firestore collection "seller_wallets"
data class SellerWallet(
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val sellerPhone: String = "",
    val sellerGSTIN: String = "",
    val sellerBusinessName: String = "",

    // Balances
    val totalEarnings: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val availableBalance: Double = 0.0,
    val paidAmount: Double = 0.0,
    val onHoldAmount: Double = 0.0,

    // Bank Details
    val bankAccountNumber: String = "",
    val bankAccountHolderName: String = "",
    val bankIFSC: String = "",
    val bankName: String = "",
    val bankBranch: String = "",
    val bankAccountType: String = "CURRENT",
    val bankVerified: Boolean = false,
    val bankVerifiedAt: Long = 0,

    // UPI
    val upiId: String = "",
    val upiVerified: Boolean = false,

    // Stats
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalPlatformFees: Double = 0.0,
    val totalGST: Double = 0.0,
    val totalPayouts: Int = 0,
    val lastPayoutAmount: Double = 0.0,
    val lastPayoutDate: Long = 0,

    // Payout Preferences
    val preferredPayoutMethod: String = "BANK_TRANSFER",
    val preferredPayoutMode: String = "NEFT",
    val minimumPayoutAmount: Double = 5000.0,

    // Compliance
    val kycVerified: Boolean = false,
    val gstVerified: Boolean = false,
    val tdsApplicable: Boolean = false,
    val tdsPercent: Double = 0.0,
    val totalTDSDeducted: Double = 0.0,

    // Status
    val walletStatus: String = "ACTIVE",
    val suspensionReason: String = "",

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastTransactionAt: Long = 0
) {
    fun canRequestPayout(): Boolean =
        walletStatus == "ACTIVE" && availableBalance > 0 && bankVerified && kycVerified

    fun getNetAvailable(): Double = availableBalance - onHoldAmount

    fun formatCurrency(amount: Double): String = "₹${String.format("%,.2f", amount)}"

    fun getFormattedAvailableBalance(): String = formatCurrency(availableBalance)
    fun getFormattedTotalEarnings(): String = formatCurrency(totalEarnings)
    fun getFormattedPendingAmount(): String = formatCurrency(pendingAmount)
    fun getFormattedPaidAmount(): String = formatCurrency(paidAmount)
}
