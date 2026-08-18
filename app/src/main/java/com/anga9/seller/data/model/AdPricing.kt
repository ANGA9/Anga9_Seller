package com.anga9.seller.data.model

/**
 * AdPricing — Fetched from Firestore adPricing/config document
 * Admin can update pricing anytime from AdPricingActivity
 * App never hardcodes prices — always fetched from Firestore
 *
 * Firestore path: adPricing/config
 */
data class AdPricing(

    // ── Banner Pricing (base rates) ───────────────────────────────────────────
    val banner7Days: Double = 500.0,
    val banner15Days: Double = 900.0,
    val banner30Days: Double = 1500.0,

    // ── In-Feed Pricing (base rates) ──────────────────────────────────────────
    val inFeed7Days: Double = 300.0,
    val inFeed15Days: Double = 550.0,
    val inFeed30Days: Double = 900.0,

    // ── Service Charges (Mode 2 — admin managed) ──────────────────────────────
    val serviceChargeBanner7Days: Double = 200.0,
    val serviceChargeBanner15Days: Double = 300.0,
    val serviceChargeBanner30Days: Double = 500.0,
    val serviceChargeInFeed7Days: Double = 150.0,
    val serviceChargeInFeed15Days: Double = 200.0,
    val serviceChargeInFeed30Days: Double = 300.0,

    // ── Metadata ──────────────────────────────────────────────────────────────
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""

) {

    /**
     * Returns base price for given ad type and duration
     * @param adType "BANNER" or "IN_FEED"
     * @param days 7, 15, or 30
     */
    fun getBasePrice(adType: String, days: Int): Double = when {
        adType == "BANNER" && days == 7  -> banner7Days
        adType == "BANNER" && days == 15 -> banner15Days
        adType == "BANNER" && days == 30 -> banner30Days
        adType == "IN_FEED" && days == 7  -> inFeed7Days
        adType == "IN_FEED" && days == 15 -> inFeed15Days
        adType == "IN_FEED" && days == 30 -> inFeed30Days
        else -> 0.0
    }

    /**
     * Returns service charge for admin-managed campaigns (Mode 2)
     */
    fun getServiceCharge(adType: String, days: Int): Double = when {
        adType == "BANNER" && days == 7  -> serviceChargeBanner7Days
        adType == "BANNER" && days == 15 -> serviceChargeBanner15Days
        adType == "BANNER" && days == 30 -> serviceChargeBanner30Days
        adType == "IN_FEED" && days == 7  -> serviceChargeInFeed7Days
        adType == "IN_FEED" && days == 15 -> serviceChargeInFeed15Days
        adType == "IN_FEED" && days == 30 -> serviceChargeInFeed30Days
        else -> 0.0
    }

    /**
     * Returns total amount for self-serve (Mode 1)
     */
    fun getTotalSelfServe(adType: String, days: Int): Double =
        getBasePrice(adType, days)

    /**
     * Returns total amount for admin-managed (Mode 2)
     */
    fun getTotalManaged(adType: String, days: Int): Double =
        getBasePrice(adType, days) + getServiceCharge(adType, days)

    companion object {
        /** Firestore document path */
        const val COLLECTION = "adPricing"
        const val DOCUMENT = "config"

        /** Available durations */
        val DURATIONS = listOf(7, 15, 30)

        /** Default pricing (used as fallback if Firestore fetch fails) */
        fun default() = AdPricing()
    }
}
