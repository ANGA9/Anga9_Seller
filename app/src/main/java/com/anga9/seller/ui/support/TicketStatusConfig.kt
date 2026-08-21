package com.anga9.seller.ui.support

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.anga9.seller.R

data class TicketUIStyle(
    val label: String,
    @ColorInt val bgColor: Int,
    @ColorInt val textColor: Int,
    @DrawableRes val iconRes: Int,
    val dimmed: Boolean = false
)

object TicketStatusConfig {
    // We map the API status string to its UI representation.
    // 'pending_user' is mapped to 'Needs reply' as per the web seller logic.
    val statusMap = mapOf(
        "pending_user" to TicketUIStyle(
            label = "Needs reply",
            bgColor = Color.parseColor("#FAEEDA"),
            textColor = Color.parseColor("#854F0B"),
            iconRes = R.drawable.ic_clock
        ),
        "in_progress" to TicketUIStyle(
            label = "In progress",
            bgColor = Color.parseColor("#E6F1FB"),
            textColor = Color.parseColor("#0C447C"),
            iconRes = R.drawable.ic_refresh
        ),
        "resolved" to TicketUIStyle(
            label = "Resolved",
            bgColor = Color.parseColor("#EAF3DE"),
            textColor = Color.parseColor("#27500A"),
            iconRes = R.drawable.ic_check,
            dimmed = true
        ),
        "open" to TicketUIStyle(
            label = "Open",
            bgColor = Color.parseColor("#FFFFFF"),
            textColor = Color.parseColor("#444441"),
            iconRes = R.drawable.ic_circle
        ),
        "closed" to TicketUIStyle(
            label = "Closed",
            bgColor = Color.parseColor("#FFFFFF"),
            textColor = Color.parseColor("#444441"),
            iconRes = R.drawable.ic_circle_x,
            dimmed = true
        )
    )

    fun getStyle(status: String?): TicketUIStyle {
        return statusMap[status?.lowercase()] ?: statusMap["open"]!!
    }

    fun getFilterStyle(filter: String): TicketUIStyle {
        if (filter.equals("all", ignoreCase = true)) {
            return TicketUIStyle(
                label = "All tickets",
                bgColor = Color.parseColor("#FFFFFF"),
                textColor = Color.parseColor("#1A6FD4"),
                iconRes = R.drawable.ic_circle
            )
        }
        return getStyle(filter)
    }
}
