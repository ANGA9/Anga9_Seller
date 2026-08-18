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
        return when (type) {
            "order_cancelled" -> NotificationVisual(
                iconResId = R.drawable.ic_baseline_close_24,
                iconTintColor = Color.parseColor("#DC2626"), // red-600
                badgeBgColor = Color.parseColor("#FEF2F2") // red-50
            )
            "order_new" -> NotificationVisual(
                iconResId = R.drawable.ic_baseline_shopping_cart_24,
                iconTintColor = Color.parseColor("#16A34A"), // green-600
                badgeBgColor = Color.parseColor("#F0FDF4") // green-50
            )
            "dispute" -> NotificationVisual(
                iconResId = R.drawable.ic_baseline_warning_24,
                iconTintColor = Color.parseColor("#D97706"), // amber-600
                badgeBgColor = Color.parseColor("#FFFBEB") // amber-50
            )
            "payout" -> NotificationVisual(
                iconResId = R.drawable.ic_rupee_24,
                iconTintColor = Color.parseColor("#2563EB"), // blue-600
                badgeBgColor = Color.parseColor("#EFF6FF") // blue-50
            )
            else -> NotificationVisual(
                iconResId = R.drawable.ic_baseline_notifications_24,
                iconTintColor = Color.parseColor("#4B5563"), // gray-600
                badgeBgColor = Color.parseColor("#F9FAFB") // gray-50
            )
        }
    }
}
