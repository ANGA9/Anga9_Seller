package com.anga9.seller.utils

import java.text.NumberFormat
import java.util.Locale

data class DeltaResult(
    val text: String,
    val isPositive: Boolean? = null
)

object AppFormatters {

    fun formatINR(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("INR", "₹").trim()
    }

    fun formatINRWithDecimals(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return format.format(amount).replace("INR", "₹").trim()
    }

    fun formatINRShort(amount: Double): String {
        return when {
            amount >= 100000 -> String.format(Locale.US, "₹%.1fL", amount / 100000)
            amount >= 1000 -> String.format(Locale.US, "₹%.1fK", amount / 1000)
            else -> String.format(Locale.US, "₹%.0f", amount)
        }
    }

    /**
     * Shared zero-state-safe delta formatter:
     * - If current value is 0 -> neutral message (never show misleading +X% on zero base).
     * - If previous value was 0 -> neutral "No prior period data" or "First period".
     * - If unchanged -> neutral "No change".
     * - Otherwise -> "↑X% vs last period" / "↓X% vs last period".
     */
    fun formatDelta(
        current: Double,
        previous: Double,
        metricName: String = "orders",
        periodLabel: String = "today"
    ): DeltaResult {
        if (current <= 0.0) {
            val periodText = if (periodLabel.equals("today", ignoreCase = true)) "today" else "in this period"
            return DeltaResult(text = "No $metricName yet $periodText", isPositive = null)
        }
        if (previous <= 0.0) {
            return DeltaResult(text = "First recorded $metricName", isPositive = null)
        }
        val diff = current - previous
        val pct = (diff / previous) * 100.0
        if (Math.abs(pct) < 0.1) {
            return DeltaResult(text = "No change vs last period", isPositive = null)
        }
        val isUp = pct > 0
        val sign = if (isUp) "↑" else "↓"
        val formatted = String.format(Locale.US, "%s%.0f%% vs last period", sign, Math.abs(pct))
        return DeltaResult(text = formatted, isPositive = isUp)
    }

    data class StatusBadgeConfig(
        val label: String,
        val textColor: Int,
        val bgColor: Int
    )

    fun getStatusConfig(status: String?): StatusBadgeConfig {
        val s = status?.lowercase() ?: "pending"
        return when (s) {
            "delivered" -> StatusBadgeConfig("Delivered", 0xFF15803D.toInt(), 0xFFDCFCE7.toInt())
            "shipped" -> StatusBadgeConfig("Shipped", 0xFF1D4ED8.toInt(), 0xFFDBEAFE.toInt())
            "processing", "confirmed" -> StatusBadgeConfig(
                if (s == "confirmed") "Confirmed" else "Processing",
                0xFFB45309.toInt(),
                0xFFFEF3C7.toInt()
            )
            "cancelled", "rejected" -> StatusBadgeConfig("Cancelled", 0xFFB91C1C.toInt(), 0xFFFEE2E2.toInt())
            "return_requested", "returned" -> StatusBadgeConfig("Returned", 0xFFC2410C.toInt(), 0xFFFFEDD5.toInt())
            else -> StatusBadgeConfig(s.replaceFirstChar { it.uppercase() }, 0xFF4B5563.toInt(), 0xFFF3F4F6.toInt())
        }
    }

    /**
     * Sanitizes raw exceptions, socket dumps, and DNS failures into clean human-friendly messages.
     * Prevents raw "Unable to resolve host" dumps from displaying on user screens.
     */
    fun getHumanErrorMessage(e: Throwable?, defaultMsg: String = "Unable to complete request"): String {
        if (e == null) return defaultMsg
        val msg = e.message ?: ""
        if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.ConnectException ||
            e is java.io.IOException || msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) || msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("Network error", ignoreCase = true)) {
            return "No internet connection. Please check your network."
        }
        if (msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized", ignoreCase = true)) {
            return "Session expired. Please log in again."
        }
        if (msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")) {
            return "Server is temporarily busy. Please try again shortly."
        }
        return if (msg.length > 80) defaultMsg else msg.ifEmpty { defaultMsg }
    }
}
