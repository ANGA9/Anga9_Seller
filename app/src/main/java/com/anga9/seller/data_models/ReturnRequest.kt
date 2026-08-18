package com.anga9.seller.data_models

data class ReturnRequest(
    val returnId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val type: String = TYPE_RETURN,
    val items: List<ReturnItem> = emptyList(),
    val reason: String = "",
    val reasonDescription: String = "",
    val photos: List<String> = emptyList(),
    val exchangeVariant: Map<String, String> = emptyMap(),
    val refundMethod: String = REFUND_WALLET,
    val refundAmount: Double = 0.0,
    val status: String = STATUS_REQUESTED,
    val rejectionReason: String = "",
    val inspectionResult: String = "",
    val inspectionPhotos: List<String> = emptyList(),
    val policySnapshot: ReturnPolicy = ReturnPolicy(),
    val requestedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long = 0,
    val rejectedAt: Long = 0,
    val itemReceivedAt: Long = 0,
    val refundedAt: Long = 0
) {
    companion object {
        const val TYPE_RETURN = "return"
        const val TYPE_EXCHANGE = "exchange"
        const val REASON_WRONG_ITEM = "wrong_item"
        const val REASON_SIZE_ISSUE = "size_issue"
        const val REASON_QUALITY_DEFECT = "quality_defect"
        const val REASON_DAMAGED = "damaged"
        const val REASON_NOT_AS_DESCRIBED = "not_as_described"
        const val REASON_OTHER = "other"
        const val REFUND_WALLET = "wallet"
        const val REFUND_ORIGINAL_PAYMENT = "original_payment"
        const val STATUS_REQUESTED = "requested"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_ITEM_RECEIVED = "item_received"
        const val STATUS_REFUND_PROCESSED = "refund_processed"
        const val STATUS_CLOSED = "closed"
        const val STATUS_CLOSED_NO_REFUND = "closed_no_refund"
        const val STATUS_DISPUTED = "disputed"
        const val INSPECTION_GOOD = "good"
        const val INSPECTION_DAMAGED_BY_CUSTOMER = "damaged_by_customer"
        const val INSPECTION_WRONG_ITEM_RETURNED = "wrong_item_returned"
        const val COLLECTION = "returns"

        fun getStatusLabel(status: String): String = when (status) {
            STATUS_REQUESTED -> "Pending Approval"
            STATUS_APPROVED -> "Approved"
            STATUS_REJECTED -> "Rejected"
            STATUS_ITEM_RECEIVED -> "Item Received"
            STATUS_REFUND_PROCESSED -> "Refund Processed"
            STATUS_CLOSED -> "Closed"
            STATUS_CLOSED_NO_REFUND -> "Closed (No Refund)"
            STATUS_DISPUTED -> "Disputed"
            else -> status
        }

        fun getReasonLabel(reason: String): String = when (reason) {
            REASON_WRONG_ITEM -> "Wrong item received"
            REASON_SIZE_ISSUE -> "Size/fit issue"
            REASON_QUALITY_DEFECT -> "Quality defect"
            REASON_DAMAGED -> "Damaged in transit"
            REASON_NOT_AS_DESCRIBED -> "Not as described"
            REASON_OTHER -> "Other"
            else -> reason
        }

        fun getStatusColor(status: String): String = when (status) {
            STATUS_REQUESTED -> "#FF9800"
            STATUS_APPROVED -> "#2196F3"
            STATUS_REJECTED -> "#F44336"
            STATUS_ITEM_RECEIVED -> "#9C27B0"
            STATUS_REFUND_PROCESSED -> "#4CAF50"
            STATUS_CLOSED -> "#607D8B"
            STATUS_CLOSED_NO_REFUND -> "#795548"
            STATUS_DISPUTED -> "#FF5722"
            else -> "#607D8B"
        }
    }

    fun isActionableBySeller(): Boolean = status == STATUS_REQUESTED
    fun isRefundPending(): Boolean = status == STATUS_ITEM_RECEIVED || status == STATUS_DISPUTED
    fun isClosed(): Boolean = status in listOf(STATUS_CLOSED, STATUS_CLOSED_NO_REFUND, STATUS_REFUND_PROCESSED)
}

data class ReturnItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val refundAmount: Double = 0.0
)