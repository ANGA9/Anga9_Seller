package com.anga9.seller.ui.deals

import com.anga9.seller.R

data class DealTypeItem(
    val id: String,
    val title: String,
    val iconRes: Int,
    val iconTint: Int
)

object DealTypeConfig {
    val DEAL_TYPES = listOf(
        DealTypeItem(
            id = "Flash Sale",
            title = "Flash Sale",
            iconRes = R.drawable.ic_clock,
            iconTint = 0xFFD98E04.toInt() // Amber
        ),
        DealTypeItem(
            id = "Deal of the Day",
            title = "Deal of the Day",
            iconRes = R.drawable.ic_star_filled,
            iconTint = 0xFF2851C4.toInt() // Blue
        ),
        DealTypeItem(
            id = "Bundle Offer",
            title = "Bundle Offer",
            iconRes = R.drawable.ic_package,
            iconTint = 0xFF10B981.toInt() // Green
        ),
        DealTypeItem(
            id = "Clearance",
            title = "Clearance",
            iconRes = R.drawable.ic_tag,
            iconTint = 0xFFEF4444.toInt() // Red
        )
    )
}
