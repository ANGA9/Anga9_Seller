package com.anga9.seller.data_models

data class Seller(
    val uid: String = "",
    val phone: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val businessType: String = "",       // Retailer / Wholesaler / Manufacturer / Trader
    val gstNumber: String = "",
    val panNumber: String = "",
    val address: String = "",
    val pinCode: String = "",
    val city: String = "",
    val state: String = "",
    val kycStatus: String = "pending",   // pending / approved / rejected
    val verificationStatus: String = "pending",
    val rejectionReason: String = "",
    val badgeType: String = "new",       // new / regular / trusted / premium
    val category: String = "NEW",        // NEW/REGULAR/TRUSTED/PREMIUM
    val documents: SellerDocuments = SellerDocuments(),
    val fcmToken: String = "",
    val createdAt: Long = 0L,
    val approvedAt: Long = 0L,

    // New Onboarding Fields (from redesigned registration flow)
    val language: String = "",                        // "hi" / "en" / "gu" / "mr" / "pa" / "ta"
    val garmentCategories: List<String> = emptyList(), // ["Readymade Garments", "Fabric / Grey Cloth", ...]
    val kycGstUrl: String = "",                       // GST certificate URL (new flow)
    val kycSecondDocType: String = "",                // "udyam" / "shop" / "trade" / "aadhaar" / ""
    val kycSecondDocUrl: String = "",                 // Second KYC document URL (optional)

    // Bank Details
    val bankAccountNumber: String = "",
    val ifscCode: String = "",
    val bankName: String = "",
    val branchName: String = "",
    val accountType: String = "Savings",

    // Delivery Zones
    val deliveryPincodes: List<String> = emptyList(),

    // Business Details
    val businessDescription: String = "",
    val businessCategory: String = "",
    val yearEstablished: Int = 0,

    // Notification Preferences
    val notifNewOrder: Boolean = true,
    val notifOrderStatus: Boolean = true,
    val notifPayoutUpdate: Boolean = true,
    val notifProductApproval: Boolean = true,
    val notifLowStock: Boolean = true,

    // Profile Photo
    val profilePhotoUrl: String? = null
)

data class SellerDocuments(
    val gstCertUrl: String = "",
    val panCardUrl: String = "",
    val shopPhotoUrl: String = "",
    val bankProofUrl: String = ""
)