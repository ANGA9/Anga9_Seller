package com.anga9.seller.data.model

// Local transaction model for displaying wallet history
// Built from revenues + payouts collections
data class WalletTransaction(
    val transactionId: String = "",
    val type: String = "CREDIT", // CREDIT, DEBIT
    val amount: Double = 0.0,
    val description: String = "",
    val orderId: String = "",
    val payoutId: String = "",
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val referenceNumber: String = ""
) {
    fun formatCurrency(): String = "₹${String.format("%,.2f", amount)}"
    fun isCredit(): Boolean = type == "CREDIT"
}
