package com.anga9.seller.data.model.privacy

import androidx.annotation.DrawableRes
import com.anga9.seller.R

/**
 * Model representing a device permission request item.
 * Driven by config array so permissions can easily be updated or extended.
 */
data class AppPermissionItem(
    val id: String,
    @get:DrawableRes val iconRes: Int,
    val title: String,
    val description: String
)

object DataPrivacyConfig {
    /**
     * Default list of device permissions requested by the Seller App.
     */
    val permissions: List<AppPermissionItem> = listOf(
        AppPermissionItem(
            id = "camera",
            iconRes = R.drawable.ic_camera,
            title = "Camera",
            description = "Used for KYC verification, catalog photos, and barcode scanning"
        ),
        AppPermissionItem(
            id = "storage",
            iconRes = R.drawable.ic_folder,
            title = "Storage & Media",
            description = "Used to upload GST/trademark certificates and download invoices"
        ),
        AppPermissionItem(
            id = "notifications",
            iconRes = R.drawable.ic_notifications,
            title = "Push Notifications",
            description = "Used for instant alerts on new orders, pickups, and payouts"
        ),
        AppPermissionItem(
            id = "location",
            iconRes = R.drawable.ic_location,
            title = "Location",
            description = "Used to verify warehouse address and coordinate pickups"
        )
    )

    /**
     * Compliance & Data Security bullet points.
     */
    val privacyBullets: List<String> = listOf(
        "KYC data (GSTIN, PAN, bank details) encrypted at rest using AES-256",
        "Data hosted within India, DPDP Act 2023 compliant",
        "Never sold or rented to third-party advertisers"
    )
}
