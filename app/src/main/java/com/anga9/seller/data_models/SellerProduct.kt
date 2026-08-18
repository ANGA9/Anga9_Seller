package com.anga9.seller.data_models

data class SellerProduct(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val discount: Int = 0,
    val gstPercent: Int = 5,             // 0, 5, 12, 18, 28
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerPhone: String = "",
    val isVerifiedSeller: Boolean = false,

    // Order Type - B2B core
    val orderType: String = "per_piece", // "per_piece" or "per_set"
    val moq: Int = 1,                    // For per_piece: min order qty; For per_set: auto = setSize
    val setSize: Int = 1,                // pieces per set (only used when orderType = "per_set")
    val setLabel: String = "",           // e.g. "SÃ—1, MÃ—2, LÃ—2, XLÃ—1" (optional, seller fills)
    val hsnCode: String = "",            // HSN code for GST invoice

    val unit: String = "unit",           // kg, litre, piece, box, set, etc.
    val sku: String = "",                // Stock Keeping Unit
    val stock: Int = 0,
    val lowStockThreshold: Int = 5,
    val bulkPricing: List<BulkPriceTier> = emptyList(),
    val category: String = "",
    val categoryId: String = "",
    val subcategory: String = "",
    val productType: String = "",
    val variants: List<ProductVariant> = emptyList(),
    val hasVariants: Boolean = false,    // true = Size/Color matrix ordering enabled
    val expiryDate: String = "",         // For fresh/food products
    val batchNumber: String = "",
    val warehouseLocation: String = "",

    // Garment specific fields
    val fabric: String = "",             // e.g. "100% Cotton", "Polyester Blend"
    val sizesAvailable: String = "",     // e.g. "S, M, L, XL, XXL"
    val colorsAvailable: String = "",    // e.g. "Red, Blue, Black, White"
    val gsm: String = "",                // e.g. "180 GSM"
    val pattern: String = "",            // e.g. "Checked", "Solid", "Striped"
    val fitShape: String = "",           // e.g. "Regular", "Slim", "Oversized"

    // Return & Exchange Policy
    // Seller sets this per product. Snapshot is copied to order at purchase time.
    val returnPolicy: ReturnPolicy = ReturnPolicy(),

    // Approval System
    val status: String = "pending",      // pending / approved / rejected
    val isApproved: Boolean = false,
    val rejectionReason: String = "",
    val isActive: Boolean = false,       // true only when approved
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BulkPriceTier(
    val minQuantity: Int = 0,
    val maxQuantity: Int = 0,
    val pricePerUnit: Double = 0.0,
    val discountPercent: Int = 0
)

data class ProductVariant(
    val color: String = "",
    val colorHex: String = "#000000",    // hex color code for UI display
    val sizes: Map<String, VariantSize> = emptyMap()  // "S" -> VariantSize, "M" -> VariantSize
) {
    fun totalStock(): Int = sizes.values.sumOf { it.stock }
    fun isSizeAvailable(size: String): Boolean = (sizes[size]?.stock ?: 0) > 0
}

data class VariantSize(
    val stock: Int = 0,
    val price: Double = 0.0
)

