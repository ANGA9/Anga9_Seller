package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

// GET /api/orders/:id/invoice
/**
 * Response for GET /api/orders/:id/invoice
 * Returns URL to the invoice PDF.
 */
data class InvoiceResponse(
    @SerializedName("url")            val url: String = "",
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("order_id")       val orderId: String = "",
    @SerializedName("created_at")     val createdAt: String = ""
)

// POST /api/products/bulk-import
/**
 * Request for POST /api/products/bulk-import
 * Sends a list of products to import in bulk.
 */
data class BulkImportRequest(
    @SerializedName("products") val products: List<Map<String, Any>>
)

/**
 * Response for POST /api/products/bulk-import
 */
data class BulkImportResponse(
    @SerializedName("imported") val imported: Int = 0,
    @SerializedName("failed")   val failed: Int = 0,
    @SerializedName("errors")   val errors: List<String> = emptyList()
)