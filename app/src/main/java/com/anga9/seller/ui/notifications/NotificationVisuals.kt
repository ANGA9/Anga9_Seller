package com.anga9.seller.ui.notifications

import android.graphics.Color
import com.anga9.seller.R

data class NotificationVisual(
    val iconResId: Int,
    val iconTintColor: Int,
    val badgeBgColor: Int
)

object NotificationVisuals {

    fun getVisualForType(type: String): NotificationVisual {
        val normalized = type.lowercase().replace(".", "_")
        return when {
            normalized.contains("cancel") || normalized.contains("reject") || normalized.contains("fail") -> NotificationVisual(
                iconResId = R.drawable.ic_baseline_close_24,
                iconTintColor = Color.parseColor("#DC2626"), // Red
                badgeBgColor = Color.parseColor("#FEF2F2") // Soft Red
            )
            normalized.contains("order") || normalized.contains("placed") -> NotificationVisual(
                iconResId = R.drawable.ic_orders,
                iconTintColor = Color.parseColor("#1A6FD4"), // ANGA9 Royal Blue
                badgeBgColor = Color.parseColor("#EFF6FF") // Soft Blue
            )
            normalized.contains("dispute") || normalized.contains("return") || normalized.contains("warning") -> NotificationVisual(
                iconResId = R.drawable.ic_privacy_shield,
                iconTintColor = Color.parseColor("#D97706"), // Amber
                badgeBgColor = Color.parseColor("#FFFBEB") // Soft Amber
            )
            normalized.contains("payout") || normalized.contains("payment") || normalized.contains("wallet") -> NotificationVisual(
                iconResId = R.drawable.ic_wallet,
                iconTintColor = Color.parseColor("#1A6FD4"), // Royal Blue
                badgeBgColor = Color.parseColor("#EFF6FF") // Soft Blue
            )
            normalized.contains("product") || normalized.contains("approve") || normalized.contains("seller") -> NotificationVisual(
                iconResId = R.drawable.ic_check_circle,
                iconTintColor = Color.parseColor("#16A34A"), // Green
                badgeBgColor = Color.parseColor("#F0FDF4") // Soft Green
            )
            else -> NotificationVisual(
                iconResId = R.drawable.ic_notifications,
                iconTintColor = Color.parseColor("#1A6FD4"), // Royal Blue
                badgeBgColor = Color.parseColor("#EFF6FF") // Soft Blue
            )
        }
    }
}
