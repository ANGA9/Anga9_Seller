package com.anga9.seller.data.model

/**
 * AdCampaign — Complete production model for Ad Campaign System
 *
 * 3 Creation Modes:
 *  - MODE 1: createdBy = "seller"         → Self-serve, standard pricing
 *  - MODE 2: createdBy = "seller_request" → Admin managed, premium pricing
 *  - MODE 3: createdBy = "admin"          → Platform/admin created, free
 *
 * Status Flow:
 *  Mode 1: draft → pending_payment → pending_review → active / rejected
 *  Mode 2: request_submitted → pending_design → preview_sent →
 *          preview_feedback_sent → preview_approved → active
 *  Mode 3: active (direct)
 */
data class AdCampaign(

    // ── Identity ──────────────────────────────────────────────────────────────
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",

    // ── Product Info ──────────────────────────────────────────────────────────
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val productPrice: Double = 0.0,
    val productCategory: String = "",

    // ── Ad Content ────────────────────────────────────────────────────────────
    val adType: String = AdType.BANNER,           // "BANNER" / "IN_FEED"
    val bannerImageUrl: String = "",              // seller uploaded banner image
    val headline: String = "",                    // ad headline text
    val ctaText: String = "Shop Now",             // call-to-action button text
    val description: String = "",                 // optional ad description

    // ── Targeting ─────────────────────────────────────────────────────────────
    val targetCategories: List<String> = emptyList(), // e.g. ["vegetables","fruits"]
    val targetCities: List<String> = emptyList(),     // optional geo targeting

    // ── Creation Mode ─────────────────────────────────────────────────────────
    val createdBy: String = CreatedBy.SELLER,     // "seller" / "seller_request" / "admin"
    val managedBy: String = ManagedBy.SELLER,     // "seller" / "admin"
    val isPlatformAd: Boolean = false,            // true = admin's own platform ad

    // ── Pricing ───────────────────────────────────────────────────────────────
    val baseAmount: Double = 0.0,                 // standard rate from adPricing config
    val serviceCharge: Double = 0.0,              // 0 for self-serve, >0 for managed
    val totalAmount: Double = 0.0,                // baseAmount + serviceCharge
    val dailyBudget: Double = 0.0,                // optional daily spend cap
    val todaySpend: Double = 0.0,                 // resets daily via Firebase Function
    val currentSpend: Double = 0.0,               // total spend so far

    // ── Duration ──────────────────────────────────────────────────────────────
    val duration: Int = 7,                        // days: 7 / 15 / 30
    val startDate: Long = 0L,
    val endDate: Long = 0L,

    // ── Status ────────────────────────────────────────────────────────────────
    /**
     * Status values:
     * "draft"                  - not yet submitted
     * "pending_payment"        - form filled, awaiting wallet payment
     * "pending_review"         - paid, waiting admin approval (Mode 1)
     * "request_submitted"      - Mode 2 request sent to admin
     * "pending_design"         - admin is designing the ad (Mode 2)
     * "preview_sent"           - admin sent preview to seller (Mode 2)
     * "preview_feedback_sent"  - seller requested changes (Mode 2)
     * "preview_approved"       - seller approved design (Mode 2)
     * "active"                 - live and running
     * "paused"                 - manually paused or budget exhausted
     * "completed"              - campaign ended (endDate passed)
     * "rejected"               - admin rejected
     */
    val status: String = AdStatus.DRAFT,
    val paymentStatus: String = PaymentStatus.UNPAID, // "unpaid" / "paid" / "waived"
    val isActive: Boolean = false,
    val priority: Int = 0,                        // higher = shown first (bid-based)

    // ── Pause / Reject Info ───────────────────────────────────────────────────
    val pausedAt: Long = 0L,
    val pauseReason: String = "",
    val rejectionReason: String = "",

    // ── Preview System (Mode 2) ───────────────────────────────────────────────
    val previewViewedBySeller: Boolean = false,
    val previewViewedAt: Long = 0L,
    val previewSentAt: Long = 0L,
    val previewApprovedBySeller: Boolean = false,
    val previewApprovedAt: Long = 0L,
    val sellerFeedback: String = "",              // changes requested by seller
    val previewIteration: Int = 0,                // how many times preview was revised

    // ── Admin Notes ───────────────────────────────────────────────────────────
    val requestNotes: String = "",                // seller's requirements (Mode 2)
    val adminNotes: String = "",                  // admin's internal notes

    // ── Analytics ─────────────────────────────────────────────────────────────
    val impressions: Long = 0L,
    val clicks: Long = 0L,
    val conversions: Long = 0L,                   // orders placed via this ad
    val ctr: Double = 0.0,                        // clicks / impressions * 100
    val revenueGenerated: Double = 0.0,           // revenue from conversions

    // ── Timestamps ────────────────────────────────────────────────────────────
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByUid: String = "",
    val updatedByUid: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────

object AdType {
    const val BANNER = "BANNER"
    const val IN_FEED = "IN_FEED"
}

object CreatedBy {
    const val SELLER = "seller"
    const val SELLER_REQUEST = "seller_request"
    const val ADMIN = "admin"
}

object ManagedBy {
    const val SELLER = "seller"
    const val ADMIN = "admin"
}

object AdStatus {
    const val DRAFT = "draft"
    const val PENDING_PAYMENT = "pending_payment"
    const val PENDING_REVIEW = "pending_review"
    const val REQUEST_SUBMITTED = "request_submitted"
    const val PENDING_DESIGN = "pending_design"
    const val PREVIEW_SENT = "preview_sent"
    const val PREVIEW_FEEDBACK_SENT = "preview_feedback_sent"
    const val PREVIEW_APPROVED = "preview_approved"
    const val ACTIVE = "active"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val REJECTED = "rejected"

    /** Human-readable label for UI display */
    fun label(status: String): String = when (status) {
        DRAFT -> "Draft"
        PENDING_PAYMENT -> "Awaiting Payment"
        PENDING_REVIEW -> "Under Review"
        REQUEST_SUBMITTED -> "Request Submitted"
        PENDING_DESIGN -> "Being Designed"
        PREVIEW_SENT -> "Preview Ready"
        PREVIEW_FEEDBACK_SENT -> "Changes Requested"
        PREVIEW_APPROVED -> "Approved — Going Live"
        ACTIVE -> "Active"
        PAUSED -> "Paused"
        COMPLETED -> "Completed"
        REJECTED -> "Rejected"
        else -> status
    }
}

object PaymentStatus {
    const val UNPAID = "unpaid"
    const val PAID = "paid"
    const val WAIVED = "waived"   // admin waived payment (Mode 3 / free promo)
}
