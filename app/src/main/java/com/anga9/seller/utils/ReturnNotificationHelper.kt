package com.anga9.seller.utils

import android.util.Log

/**
 * Helper to send return-related notifications.
 *
 * NOTE: Firebase Firestore removed. Notifications are now queued via
 * the backend REST API. This helper currently logs only — wire up
 * the API call when the backend notification endpoint is ready.
 *
 * Notification types:
 * - "return_approved"  -> sent to customer when seller approves
 * - "return_rejected"  -> sent to customer when seller rejects
 * - "return_received"  -> sent to customer when seller marks item received
 * - "return_override"  -> sent to seller when admin overrides rejection
 */
object ReturnNotificationHelper {

    private const val TAG = "ReturnNotificationHelper"

    fun notifyCustomerReturnApproved(customerId: String, orderId: String, returnId: String) {
        sendNotification(
            recipientId = customerId,
            type = "return_approved",
            title = "Return Request Approved",
            body = "Your return request has been approved. Please ship the item back.",
            data = mapOf("orderId" to orderId, "returnId" to returnId)
        )
    }

    fun notifyCustomerReturnRejected(customerId: String, orderId: String, returnId: String, reason: String) {
        sendNotification(
            recipientId = customerId,
            type = "return_rejected",
            title = "Return Request Rejected",
            body = reason.ifEmpty { "Your return request has been rejected by the seller." },
            data = mapOf("orderId" to orderId, "returnId" to returnId, "reason" to reason)
        )
    }

    fun notifyCustomerItemReceived(customerId: String, orderId: String, returnId: String) {
        sendNotification(
            recipientId = customerId,
            type = "return_received",
            title = "Item Received by Seller",
            body = "Seller has received your returned item. Processing exchange/refund.",
            data = mapOf("orderId" to orderId, "returnId" to returnId)
        )
    }

    private fun sendNotification(recipientId: String, type: String, title: String, body: String, data: Map<String, String>) {
        if (recipientId.isEmpty()) return
        // TODO: Send via backend API when notification endpoint is ready
        // For now, just log — Firebase Firestore has been removed from this project
        Log.d(TAG, "Notification queued: $type to $recipientId | title=$title")
    }
}
