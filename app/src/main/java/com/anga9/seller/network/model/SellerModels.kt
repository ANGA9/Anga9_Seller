package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

// ── 3A: Seller Profile & KYC ─────────────────────────────────────────────────

data class SellerProfileWrapperResponse(
    @SerializedName("sellerProfile") val sellerProfile: SellerProfileResponse?
)

data class SellerProfileResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("auth_uid") val authUid: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String = "seller",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("verification_status") val kycStatus: String? = null,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("business_type") val businessType: String? = null,
    @SerializedName("owner_name") val ownerName: String? = null,
    @SerializedName("gst_number") val gstNumber: String? = null,
    @SerializedName("pan_number") val panNumber: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("pincode") val pincode: String? = null,
    @SerializedName("bank_account_number") val bankAccountNumber: String? = null,
    @SerializedName("bank_ifsc") val bankIfsc: String? = null,
    @SerializedName("bank_account_name") val bankAccountName: String? = null,
    @SerializedName("delivery_zones") val deliveryZones: List<String>? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    
    // Storefront fields
    @SerializedName("storefront_banner_url") val storefrontBannerUrl: String? = null,
    @SerializedName("about_md") val aboutMd: String? = null,
    @SerializedName("storefront_published") val storefrontPublished: Boolean = false,
    @SerializedName("social_links") val socialLinks: Map<String, String>? = null
)

data class SellerStatsResponse(
    @SerializedName("total_products") val totalProducts: Int = 0,
    @SerializedName("total_orders") val totalOrders: Int = 0,
    @SerializedName("pending_orders") val pendingOrders: Int = 0,
    @SerializedName("total_earnings") val totalEarnings: Double = 0.0,
    @SerializedName("pending_payout") val pendingPayout: Double = 0.0,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("total_reviews") val totalReviews: Int = 0,
    @SerializedName("pending_returns") val pendingReturns: Int = 0,
    @SerializedName("open_tickets") val openTickets: Int = 0
)

data class UpdateSellerProfileRequest(
    @SerializedName("owner_name") val ownerName: String? = null,
    @SerializedName("name") val name: String? = null, // Used for email/name
    @SerializedName("store_description") val storeDescription: String? = null,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("business_type") val businessType: String? = null,
    @SerializedName("business_category") val businessCategory: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("pincode") val pincode: String? = null,
    @SerializedName("gstin") val gstin: String? = null,
    @SerializedName("pan_number") val panNumber: String? = null,
    @SerializedName("aadhaar_number") val aadhaarNumber: String? = null,
    @SerializedName("bank_account_name") val bankAccountName: String? = null,
    @SerializedName("bank_account_number") val bankAccountNumber: String? = null,
    @SerializedName("bank_ifsc") val bankIfsc: String? = null,
    @SerializedName("bank_name") val bankName: String? = null,
    @SerializedName("bank_branch") val bankBranch: String? = null,
    @SerializedName("pickup_address_same") val pickupAddressSame: Boolean? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("delivery_zones") val deliveryZones: List<String>? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class SubmitKycRequest(
    @SerializedName("gst_number") val gstNumber: String? = null,
    @SerializedName("pan_number") val panNumber: String? = null,
    @SerializedName("gst_cert_url") val gstCertUrl: String? = null,
    @SerializedName("pan_card_url") val panCardUrl: String? = null,
    @SerializedName("shop_photo_url") val shopPhotoUrl: String? = null,
    @SerializedName("bank_proof_url") val bankProofUrl: String? = null
)

// ── 3B: Products ─────────────────────────────────────────────────────────────

data class SingleProductWrapperResponse(
    @SerializedName("product") val product: SellerProductResponse?
)

data class SellerProductResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("mrp") val mrp: Double? = null,
    @SerializedName("base_price") val basePrice: Double? = null,
    @SerializedName("sale_price") val salePrice: Double? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("subcategory") val subcategory: String? = null,
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("videos") val videos: List<String>? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("seller_id") val sellerId: String = "",
    @SerializedName("seller_name") val sellerName: String? = null,
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("gst_rate") val gstRate: Double? = null,
    @SerializedName("hsn_code") val hsnCode: String? = null,
    @SerializedName("weight") val weight: Double? = null,
    @SerializedName("variants") val variants: List<ProductVariantResponse>? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("min_order_qty") val minOrderQty: Int? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("commission_rate") val commissionRate: Double? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("category_ids") val categoryIds: List<String>? = null
)

data class ProductVariantResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("value") val value: String = "",
    @SerializedName("price") val price: Double? = null,
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("sku") val sku: String? = null
)

data class CategoryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("parent_id") val parentId: String?,
    @SerializedName("sort_order") val sortOrder: Int?
)

data class CategoriesWrapperResponse(
    @SerializedName("categories") val categories: List<CategoryResponse>
)

data class CreateProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("base_price") val basePrice: Double,
    @SerializedName("sale_price") val salePrice: Double,
    @SerializedName("min_order_qty") val minOrderQty: Int = 1,
    @SerializedName("category_ids") val categoryIds: List<String>,
    @SerializedName("unit") val unit: String? = "piece",
    @SerializedName("status") val status: String? = "pending_review",
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("videos") val videos: List<String>? = null,
    @SerializedName("initial_stock") val initialStock: Int = 0,
    @SerializedName("country_of_origin") val countryOfOrigin: String? = "India",
    @SerializedName("gst_rate") val gstRate: Double? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("hsn_code") val hsnCode: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("return_policy") val returnPolicy: String? = null,
    @SerializedName("warranty") val warranty: String? = null,
    @SerializedName("sku") val sku: String? = null
)

data class ProductVariantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: String,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("sku") val sku: String? = null
)

data class UpdateProductRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("base_price") val basePrice: Double? = null,  // MRP
    @SerializedName("sale_price") val salePrice: Double? = null,  // Wholesale
    @SerializedName("price") val price: Double? = null,
    @SerializedName("mrp") val mrp: Double? = null,
    @SerializedName("category_ids") val categoryIds: List<String>? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("subcategory") val subcategory: String? = null,
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("videos") val videos: List<String>? = null,
    @SerializedName("stock") val stock: Int? = null,
    @SerializedName("min_order_qty") val minOrderQty: Int? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("gst_rate") val gstRate: Double? = null,
    @SerializedName("hsn_code") val hsnCode: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class ProductListResponse(
    @SerializedName("data") val data: List<SellerProductResponse>? = null,
    @SerializedName("products") val products: List<SellerProductResponse>? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20
) {
    fun getList(): List<SellerProductResponse> = data ?: products ?: emptyList()
}

// ── 3C: Inventory ─────────────────────────────────────────────────────────────

data class InventoryResponse(
    @SerializedName("product_id") val productId: String = "",
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("reserved") val reserved: Int = 0,
    @SerializedName("available") val available: Int = 0,
    @SerializedName("low_stock_threshold") val lowStockThreshold: Int = 5,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class UpdateStockRequest(
    @SerializedName("stock") val stock: Int? = null,
    @SerializedName("adjustment") val adjustment: Int? = null,
    @SerializedName("reason") val reason: String? = null
)

data class BulkStockUpdateRequest(
    @SerializedName("items") val items: List<StockUpdateItem>
)

data class StockUpdateItem(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
)

data class BulkPriceUpdateRequest(
    @SerializedName("updates") val updates: List<PriceUpdateItem>
)

data class PriceUpdateItem(
    @SerializedName("product_id") val productId: String,
    @SerializedName("base_price") val basePrice: Double,
    @SerializedName("sale_price") val salePrice: Double?
)

// ── 3D: Orders ────────────────────────────────────────────────────────────────

data class SellerOrderResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("customer_id") val customerId: String = "",
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("seller_id") val sellerId: String = "",
    @SerializedName("items") val items: List<OrderItemResponse> = emptyList(),
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("subtotal") val subtotal: Double = 0.0,
    @SerializedName("delivery_charges") val deliveryCharges: Double = 0.0,
    @SerializedName("gst_amount") val gstAmount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("delivery_address") val deliveryAddress: OrderAddressResponse? = null,
    @SerializedName("tracking_number") val trackingNumber: String? = null,
    @SerializedName("courier_name") val courierName: String? = null,
    @SerializedName("status_history") val statusHistory: List<StatusHistoryResponse>? = null,
    @SerializedName("placed_at") val placedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    /** Effective status — web seller uses items[0].status, fallback to order status */
    fun getEffectiveStatus(): String = items.firstOrNull()?.status ?: status
    /** Effective date — backend uses placed_at, fallback to created_at */
    fun getEffectiveDate(): String? = placedAt ?: createdAt
}

data class OrderItemResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("product_id") val productId: String = "",
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("product_image") var productImage: String? = null,
    @SerializedName("variant_id") val variantId: String? = null,
    @SerializedName("variant_name") val variantName: String? = null,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("total_price") val totalPrice: Double = 0.0,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("seller_id") val sellerId: String? = null,
    @SerializedName("gst_rate") val gstRate: Double? = null,
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class OrderAddressResponse(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("pincode") val pincode: String? = null
)

data class StatusHistoryResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("note") val note: String? = null
)

data class UpdateOrderStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("tracking_number") val trackingNumber: String? = null,
    @SerializedName("courier_name") val courierName: String? = null,
    @SerializedName("note") val note: String? = null
)

data class SellerOrderListResponse(
    @SerializedName("orders") val orders: List<SellerOrderResponse>? = null,
    @SerializedName("data") val data: List<SellerOrderResponse>? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20
) {
    fun getList(): List<SellerOrderResponse> = data ?: orders ?: emptyList()
}

// ── 3E: Wallet & Payouts ──────────────────────────────────────────────────────

data class SellerEarningsResponse(
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("pending") val pending: Double = 0.0,
    @SerializedName("available") val available: Double = 0.0,
    @SerializedName("requested") val requested: Double = 0.0,
    @SerializedName("paid") val paid: Double = 0.0
)

data class EarningHistoryResponse(
    @SerializedName("earnings") val earnings: List<EarningItemResponse> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

data class EarningItemResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("type") val type: String = "credit",
    @SerializedName("status") val status: String = "pending",
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SellerPayoutResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("seller_id") val sellerId: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("utr_number") val utrNumber: String? = null,
    @SerializedName("bank_account") val bankAccount: String? = null,
    @SerializedName("bank_ifsc") val bankIfsc: String? = null,
    @SerializedName("requested_at") val requestedAt: String? = null,
    @SerializedName("processed_at") val processedAt: String? = null
)

data class PayoutListResponse(
    @SerializedName("payouts") val payouts: List<SellerPayoutResponse> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

data class PayoutRequestBody(
    @SerializedName("amount") val amount: Double,
    @SerializedName("bank_account_number") val bankAccountNumber: String? = null,
    @SerializedName("bank_ifsc") val bankIfsc: String? = null,
    @SerializedName("bank_account_name") val bankAccountName: String? = null
)

// ── Generic ───────────────────────────────────────────────────────────────────

data class ApiSuccessResponse(
    @SerializedName("message") val message: String = "Success",
    @SerializedName("success") val success: Boolean = true
)


// ── Dispute Models ────────────────────────────────────────────────────────────

data class DisputeRespondRequest(
    @com.google.gson.annotations.SerializedName("seller_response") val sellerResponse: String,
    @com.google.gson.annotations.SerializedName("request_admin") val requestAdmin: Boolean = false
)

data class DisputeItem(
    @com.google.gson.annotations.SerializedName("id") val id: String = "",
    @com.google.gson.annotations.SerializedName("order_id") val orderId: String = "",
    @com.google.gson.annotations.SerializedName("order_item_id") val orderItemId: String = "",
    @com.google.gson.annotations.SerializedName("type") val type: String = "",
    @com.google.gson.annotations.SerializedName("reason") val reason: String = "",
    @com.google.gson.annotations.SerializedName("status") val status: String = "open",
    @com.google.gson.annotations.SerializedName("evidence_images") val evidenceImages: List<String> = emptyList(),
    @com.google.gson.annotations.SerializedName("seller_response") val sellerResponse: String? = null,
    @com.google.gson.annotations.SerializedName("admin_decision") val adminDecision: String? = null,
    @com.google.gson.annotations.SerializedName("refund_amount") val refundAmount: Double = 0.0,
    @com.google.gson.annotations.SerializedName("created_at") val createdAt: String = "",
    @com.google.gson.annotations.SerializedName("customer") val customer: DisputeCustomer? = null
)

data class DisputeCustomer(
    @com.google.gson.annotations.SerializedName("id") val id: String = "",
    @com.google.gson.annotations.SerializedName("full_name") val fullName: String = "",
    @com.google.gson.annotations.SerializedName("phone") val phone: String = ""
)

data class DisputeResponse(
    @com.google.gson.annotations.SerializedName("dispute") val dispute: DisputeItem? = null,
    @com.google.gson.annotations.SerializedName("disputes") val disputes: List<DisputeItem>? = null
)

data class DisputeListResponse(
    @com.google.gson.annotations.SerializedName("data") val data: List<DisputeItem> = emptyList(),
    @com.google.gson.annotations.SerializedName("total") val total: Int = 0,
    @com.google.gson.annotations.SerializedName("page") val page: Int = 1,
    @com.google.gson.annotations.SerializedName("limit") val limit: Int = 20
)

// ── Phase 6A — Reviews (Seller view) ─────────────────────────────────────────

data class SellerReviewItem(
    @com.google.gson.annotations.SerializedName("id")           val id: String = "",
    @com.google.gson.annotations.SerializedName("user_name")    val userName: String = "",
    @com.google.gson.annotations.SerializedName("rating")       val rating: Float = 0f,
    @com.google.gson.annotations.SerializedName("title")        val title: String? = null,
    @com.google.gson.annotations.SerializedName("body")         val body: String? = null,
    @com.google.gson.annotations.SerializedName("helpful_count") val helpfulCount: Int = 0,
    @com.google.gson.annotations.SerializedName("status")       val status: String = "",
    @com.google.gson.annotations.SerializedName("created_at")   val createdAt: String = "",
    @com.google.gson.annotations.SerializedName("product_id")   val productId: String = "",
    @com.google.gson.annotations.SerializedName("customer_id")  val customerId: String = "",
    @com.google.gson.annotations.SerializedName("products")     val products: ReviewProduct? = null
)

data class ReviewProduct(
    @com.google.gson.annotations.SerializedName("seller_id") val sellerId: String = "",
    @com.google.gson.annotations.SerializedName("name") val name: String = "",
    @com.google.gson.annotations.SerializedName("images") val images: List<String>? = emptyList()
)

data class SellerReviewListResponse(
    @com.google.gson.annotations.SerializedName("data")             val data: List<SellerReviewItem> = emptyList(),
    @com.google.gson.annotations.SerializedName("total")            val total: Int = 0,
    @com.google.gson.annotations.SerializedName("average_rating")   val averageRating: Float = 0f,
    @com.google.gson.annotations.SerializedName("page")             val page: Int = 1,
    @com.google.gson.annotations.SerializedName("limit")            val limit: Int = 10
)

// ── Phase 6C — Ad Campaigns (Seller) ─────────────────────────────────────────

data class AdCampaignRequest(
    @com.google.gson.annotations.SerializedName("product_id")  val productId: String,
    @com.google.gson.annotations.SerializedName("placement")   val placement: String,      // home_hero | category_top | search_sidebar
    @com.google.gson.annotations.SerializedName("starts_at")   val startsAt: String,
    @com.google.gson.annotations.SerializedName("ends_at")     val endsAt: String,
    @com.google.gson.annotations.SerializedName("banner_url")  val bannerUrl: String,
    @com.google.gson.annotations.SerializedName("headline")    val headline: String? = null,
    @com.google.gson.annotations.SerializedName("cta_text")    val ctaText: String? = null,
    @com.google.gson.annotations.SerializedName("budget_inr")  val budgetInr: Double
)

data class AdCampaignItem(
    @com.google.gson.annotations.SerializedName("id")          val id: String = "",
    @com.google.gson.annotations.SerializedName("product_id")  val productId: String = "",
    @com.google.gson.annotations.SerializedName("placement")   val placement: String = "",
    @com.google.gson.annotations.SerializedName("banner_url")  val bannerUrl: String = "",
    @com.google.gson.annotations.SerializedName("headline")    val headline: String? = null,
    @com.google.gson.annotations.SerializedName("status")      val status: String = "",    // pending | approved | rejected | active | expired
    @com.google.gson.annotations.SerializedName("budget_inr")  val budgetInr: Double = 0.0,
    @com.google.gson.annotations.SerializedName("starts_at")   val startsAt: String = "",
    @com.google.gson.annotations.SerializedName("ends_at")     val endsAt: String = "",
    @com.google.gson.annotations.SerializedName("impressions") val impressions: Int = 0,
    @com.google.gson.annotations.SerializedName("clicks")      val clicks: Int = 0,
    @com.google.gson.annotations.SerializedName("created_at")  val createdAt: String = ""
)


data class AdCampaignListResponse(
    @com.google.gson.annotations.SerializedName("data")  val data: List<AdCampaignItem> = emptyList(),
    @com.google.gson.annotations.SerializedName("total") val total: Int = 0,
    @com.google.gson.annotations.SerializedName("page")  val page: Int = 1,
    @com.google.gson.annotations.SerializedName("limit") val limit: Int = 20
)

// ── Phase 6D — Storefront (Seller) ───────────────────────────────────────────

data class UpdateStorefrontRequest(
    @com.google.gson.annotations.SerializedName("storefront_banner_url") val storefrontBannerUrl: String? = null,
    @com.google.gson.annotations.SerializedName("about_md")              val aboutMd: String? = null,
    @com.google.gson.annotations.SerializedName("storefront_published")  val storefrontPublished: Boolean? = null,
    @com.google.gson.annotations.SerializedName("social_links")          val socialLinks: Map<String, String>? = null
)

data class StorefrontUpdateResponse(
    @com.google.gson.annotations.SerializedName("store_name")            val storeName: String = "",
    @com.google.gson.annotations.SerializedName("storefront_banner_url") val storefrontBannerUrl: String? = null,
    @com.google.gson.annotations.SerializedName("about_md")              val aboutMd: String? = null,
    @com.google.gson.annotations.SerializedName("storefront_published")  val storefrontPublished: Boolean = false
)

data class RepeatBuyerItem(
    @com.google.gson.annotations.SerializedName("customer_id")   val customerId: String = "",
    @com.google.gson.annotations.SerializedName("customer_name") val customerName: String = "",
    @com.google.gson.annotations.SerializedName("order_count")   val orderCount: Int = 0,
    @com.google.gson.annotations.SerializedName("total_spent")   val totalSpent: Double = 0.0,
    @com.google.gson.annotations.SerializedName("last_order_at") val lastOrderAt: String = ""
)

data class RepeatBuyersResponse(
    @com.google.gson.annotations.SerializedName("data")  val data: List<RepeatBuyerItem> = emptyList(),
    @com.google.gson.annotations.SerializedName("total") val total: Int = 0
)


