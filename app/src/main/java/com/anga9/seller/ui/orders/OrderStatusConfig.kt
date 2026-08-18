package com.anga9.seller.ui.orders

import android.graphics.Color

data class OrderUIStyle(
    val label: String,
    val bg: String,
    val text: String,
    val border: String?
) {
    fun getBgColor() = Color.parseColor(bg)
    fun getTextColor() = Color.parseColor(text)
    fun getBorderColor() = border?.let { Color.parseColor(it) } ?: Color.TRANSPARENT
}

object OrderStatusConfig {
    val config = mapOf(
        "all" to OrderUIStyle("All Orders", "#111318", "#FFFFFF", null),
        "pending_payment" to OrderUIStyle("Pending", "#F5F5F5", "#6B7280", "#D1D5DB"),
        "pending" to OrderUIStyle("Pending", "#F5F5F5", "#6B7280", "#D1D5DB"),
        "confirmed" to OrderUIStyle("Confirmed", "#E6F1FB", "#0C447C", "#85B7EB"),
        "processing" to OrderUIStyle("Processing", "#F3E8FD", "#6B21A8", "#C6A6EE"),
        "shipped" to OrderUIStyle("Shipped", "#FAEEDA", "#854F0B", "#FAC775"),
        "delivered" to OrderUIStyle("Delivered", "#EAF3DE", "#27500A", "#97C459"),
        "cancelled" to OrderUIStyle("Cancelled", "#FBE4E1", "#B42318", "#EA9B90")
    )
}
