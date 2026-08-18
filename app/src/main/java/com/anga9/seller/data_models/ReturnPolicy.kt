package com.anga9.seller.data_models

/**
 * Return & Exchange policy set by seller per product.
 * Stored inside products/{productId}.returnPolicy in Firestore.
 *
 * IMPORTANT: When an order is placed, a snapshot of this policy is copied
 * into the order document (returnPolicySnapshot). This ensures the policy
 * at the time of purchase is preserved even if the seller changes it later.
 */
data class ReturnPolicy(
    /**
     * Policy type:
     * - "none"           → No return, no exchange (final sale)
     * - "exchange_only"  → Exchange for different size/variant only
     * - "return_refund"  → Full return with refund only
     * - "both"           → Both return+refund and exchange allowed (default)
     */
    val type: String = POLICY_BOTH,

    /**
     * Return/exchange window in days after delivery.
     * Valid values: 3, 7, 10, 15
     */
    val windowDays: Int = 7,

    /**
     * Optional conditions the customer must meet for return/exchange.
     * Values: "tags_intact", "original_packaging", "unused_only"
     */
    val conditions: List<String> = emptyList()
) {
    companion object {
        const val POLICY_NONE = "none"
        const val POLICY_EXCHANGE_ONLY = "exchange_only"
        const val POLICY_RETURN_REFUND = "return_refund"
        const val POLICY_BOTH = "both"

        const val CONDITION_TAGS_INTACT = "tags_intact"
        const val CONDITION_ORIGINAL_PACKAGING = "original_packaging"
        const val CONDITION_UNUSED_ONLY = "unused_only"

        val VALID_WINDOW_DAYS = listOf(3, 7, 10, 15)

        /** Human-readable label for display in UI */
        fun getDisplayLabel(type: String, windowDays: Int): String {
            return when (type) {
                POLICY_NONE -> "Non-returnable"
                POLICY_EXCHANGE_ONLY -> "${windowDays}-day Exchange Only"
                POLICY_RETURN_REFUND -> "${windowDays}-day Return & Refund"
                POLICY_BOTH -> "${windowDays}-day Return & Exchange"
                else -> "Non-returnable"
            }
        }

        /** Returns true if the policy allows any form of return/exchange */
        fun isReturnable(type: String): Boolean = type != POLICY_NONE

        /** Returns true if refund is applicable for this policy */
        fun isRefundApplicable(type: String): Boolean =
            type == POLICY_RETURN_REFUND || type == POLICY_BOTH

        /** Returns true if exchange is applicable for this policy */
        fun isExchangeApplicable(type: String): Boolean =
            type == POLICY_EXCHANGE_ONLY || type == POLICY_BOTH
    }

    /** Human-readable conditions string for display */
    fun getConditionsDisplay(): String {
        return conditions.mapNotNull { condition ->
            when (condition) {
                CONDITION_TAGS_INTACT -> "Tags intact"
                CONDITION_ORIGINAL_PACKAGING -> "Original packaging"
                CONDITION_UNUSED_ONLY -> "Unused condition"
                else -> null
            }
        }.joinToString(" · ")
    }

    /** Validate that this policy has sensible values */
    fun isValid(): Boolean {
        val validTypes = listOf(POLICY_NONE, POLICY_EXCHANGE_ONLY, POLICY_RETURN_REFUND, POLICY_BOTH)
        return type in validTypes && (type == POLICY_NONE || windowDays in VALID_WINDOW_DAYS)
    }
}
