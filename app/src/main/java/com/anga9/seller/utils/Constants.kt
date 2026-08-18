package com.anga9.seller.utils

object Constants {
    // Firestore Collections - same as admin & customer
    const val COLLECTION_SELLERS = "sellers"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_SELLER_WALLETS = "seller_wallets"
    const val COLLECTION_SELLER_PAYOUTS = "seller_payouts"
    const val COLLECTION_REVENUE = "revenue"
    const val COLLECTION_CATEGORIES = "categories"
    const val COLLECTION_AD_CAMPAIGNS = "adCampaigns"
    const val COLLECTION_AD_PRICING = "adPricing"

    // Firebase Storage
    const val STORAGE_SELLER_DOCS = "seller_documents"

    // KYC Status
    const val KYC_PENDING = "pending"
    const val KYC_APPROVED = "approved"
    const val KYC_REJECTED = "rejected"

    // Product Status
    const val PRODUCT_PENDING = "pending"
    const val PRODUCT_APPROVED = "approved"
    const val PRODUCT_REJECTED = "rejected"

    // Order Status
    const val ORDER_NEW = "new"
    const val ORDER_ACCEPTED = "accepted"
    const val ORDER_PACKED = "packed"
    const val ORDER_SHIPPED = "shipped"
    const val ORDER_DELIVERED = "delivered"
    const val ORDER_CANCELLED = "cancelled"

    // Seller Badge Types
    const val BADGE_NEW = "new"
    const val BADGE_REGULAR = "regular"
    const val BADGE_TRUSTED = "trusted"
    const val BADGE_PREMIUM = "premium"

    // SharedPrefs
    const val PREFS_NAME = "anga_seller_prefs"
    const val PREF_SELLER_ID = "seller_id"
    const val PREF_ONBOARDING_DONE = "onboarding_done"

    // Language — stored by LocaleHelper, key must match LocaleHelper.KEY_LANGUAGE
    const val PREF_LANGUAGE = "app_language"
    const val LANG_ENGLISH = "en"
    const val LANG_HINDI = "hi"
    const val LANG_GUJARATI = "gu"
    const val LANG_MARATHI = "mr"
    const val LANG_PUNJABI = "pa"
    const val LANG_TAMIL = "ta"
}
