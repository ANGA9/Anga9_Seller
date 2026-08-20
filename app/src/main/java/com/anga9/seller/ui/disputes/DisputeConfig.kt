package com.anga9.seller.ui.disputes

import androidx.annotation.DrawableRes
import com.anga9.seller.R

/**
 * Dispute item issue type configuration (single source of truth).
 */
data class DisputeTypeItem(
    val key: String,
    val label: String,
    @get:DrawableRes val iconRes: Int
)

/**
 * Dispute status visual and action configuration (single source of truth).
 */
data class DisputeStatusStyle(
    val label: String,
    val bgHex: String,
    val textHex: String,
    val borderHex: String,
    val showAction: Boolean,
    val dimmed: Boolean = false
)

object DisputeConfig {

    private val defaultType = DisputeTypeItem("other", "Other Issue", R.drawable.ic_info)

    private val typeMap = mapOf(
        "return" to DisputeTypeItem("return", "Return", R.drawable.ic_more_undo),
        "refund" to DisputeTypeItem("refund", "Refund", R.drawable.ic_rupee_24),
        "damaged" to DisputeTypeItem("damaged", "Damaged", R.drawable.ic_warning_triangle),
        "damage" to DisputeTypeItem("damage", "Damaged", R.drawable.ic_warning_triangle),
        "wrong_item" to DisputeTypeItem("wrong_item", "Wrong Item", R.drawable.ic_package),
        "not_received" to DisputeTypeItem("not_received", "Not Received", R.drawable.ic_clock),
        "other" to defaultType
    )

    private val defaultStatus = DisputeStatusStyle(
        label = "In Review",
        bgHex = "#FAEEDA",
        textHex = "#854F0B",
        borderHex = "#FAC775",
        showAction = true,
        dimmed = false
    )

    private val statusMap = mapOf(
        // Action required
        "open" to DisputeStatusStyle(
            label = "Action Required",
            bgHex = "#FBE4E1",
            textHex = "#B42318",
            borderHex = "#EA9B90",
            showAction = true,
            dimmed = false
        ),
        "action_required" to DisputeStatusStyle(
            label = "Action Required",
            bgHex = "#FBE4E1",
            textHex = "#B42318",
            borderHex = "#EA9B90",
            showAction = true,
            dimmed = false
        ),
        // In review
        "seller_responded" to DisputeStatusStyle(
            label = "In Review",
            bgHex = "#FAEEDA",
            textHex = "#854F0B",
            borderHex = "#FAC775",
            showAction = false, // Seller already replied; in admin or customer review
            dimmed = false
        ),
        "admin_review" to DisputeStatusStyle(
            label = "In Review",
            bgHex = "#FAEEDA",
            textHex = "#854F0B",
            borderHex = "#FAC775",
            showAction = false,
            dimmed = false
        ),
        "in_review" to DisputeStatusStyle(
            label = "In Review",
            bgHex = "#FAEEDA",
            textHex = "#854F0B",
            borderHex = "#FAC775",
            showAction = false,
            dimmed = false
        ),
        // Resolved
        "resolved" to DisputeStatusStyle(
            label = "Resolved",
            bgHex = "#EAF3DE",
            textHex = "#27500A",
            borderHex = "#EAECF0",
            showAction = false,
            dimmed = true
        ),
        "resolved_refund" to DisputeStatusStyle(
            label = "Resolved (Refund)",
            bgHex = "#EAF3DE",
            textHex = "#27500A",
            borderHex = "#EAECF0",
            showAction = false,
            dimmed = true
        ),
        "resolved_replace" to DisputeStatusStyle(
            label = "Resolved (Replace)",
            bgHex = "#EAF3DE",
            textHex = "#27500A",
            borderHex = "#EAECF0",
            showAction = false,
            dimmed = true
        ),
        "resolved_rejected" to DisputeStatusStyle(
            label = "Resolved (Rejected)",
            bgHex = "#F3F4F6",
            textHex = "#4B5563",
            borderHex = "#EAECF0",
            showAction = false,
            dimmed = true
        ),
        "closed" to DisputeStatusStyle(
            label = "Closed",
            bgHex = "#F3F4F6",
            textHex = "#4B5563",
            borderHex = "#EAECF0",
            showAction = false,
            dimmed = true
        )
    )

    fun getTypeConfig(type: String?): DisputeTypeItem {
        if (type == null) return defaultType
        return typeMap[type.lowercase().trim()] ?: defaultType
    }

    fun getStatusConfig(status: String?): DisputeStatusStyle {
        if (status == null) return defaultStatus
        return statusMap[status.lowercase().trim()] ?: defaultStatus
    }

    fun isActionRequired(status: String?): Boolean {
        return status?.lowercase()?.trim() == "open" || status?.lowercase()?.trim() == "action_required"
    }

    fun isResolved(status: String?): Boolean {
        val s = status?.lowercase()?.trim() ?: return false
        return s.startsWith("resolved") || s == "closed"
    }
}
