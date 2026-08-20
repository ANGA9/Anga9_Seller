package com.anga9.seller.network

import com.anga9.seller.network.model.*
import com.anga9.seller.data.model.BrandsResponse
import com.anga9.seller.data.model.CreateBrandRequest
import com.anga9.seller.data.model.CreateBrandResponse
import com.anga9.seller.data.model.support.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service Ã¢â‚¬â€ ANGA9 API Gateway (port 4000).
 * Phase 1: Auth endpoints
 * Phase 3: Seller Profile, Products, Inventory, Orders, Wallet, Payouts
 */
interface ApiService {

    // Ã¢â€â‚¬Ã¢â€â‚¬ AUTH Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/auth-service/src/routes/auth.routes.ts

    @POST("api/auth/verify")
    suspend fun verifyToken(
        @Body request: AuthVerifyRequest
    ): Response<AuthVerifyResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserProfileResponse>

    // Ã¢â€â‚¬Ã¢â€â‚¬ 3A: SELLER PROFILE & KYC Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/user-service/src/routes/user.routes.ts

    @GET("api/users/seller-profile")
    suspend fun getSellerProfile(): Response<SellerProfileWrapperResponse>

    @POST("api/users/seller-profile")
    suspend fun createSellerProfile(
        @Body request: UpdateSellerProfileRequest
    ): Response<SellerProfileWrapperResponse>

    @PATCH("api/users/seller-profile")
    suspend fun updateSellerProfile(
        @Body request: UpdateSellerProfileRequest
    ): Response<SellerProfileWrapperResponse>

    @POST("api/users/seller-profile/submit")
    suspend fun submitKyc(
        @Body request: SubmitKycRequest
    ): Response<SellerProfileWrapperResponse>

    @GET("api/users/seller-stats")
    suspend fun getSellerStats(): Response<SellerStatsResponse>

    @DELETE("api/users/profile")
    suspend fun deleteAccount(): Response<ApiSuccessResponse>

    // Ã¢â€â‚¬Ã¢â€â‚¬ 3B: PRODUCTS Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/product-service/src/routes/product.routes.ts

    @GET("api/categories")
    suspend fun getCategories(): Response<CategoriesWrapperResponse>

    @GET("api/products")
    suspend fun getSellerProducts(
        @Query("seller_id") sellerId: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ProductListResponse>

    @GET("api/products/{id}")
    suspend fun getProductById(
        @Path("id") productId: String
    ): Response<SingleProductWrapperResponse>

    @POST("api/products")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): Response<SingleProductWrapperResponse>

    @Multipart
    @POST("api/products/bulk-upload")
    suspend fun bulkUploadProducts(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<BulkImportResult>

    @PATCH("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body request: UpdateProductRequest
    ): Response<SingleProductWrapperResponse>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(
        @Path("id") productId: String
    ): Response<ApiSuccessResponse>

    // ── 3B.1: DEALS ─────────────────────────────────────────────────────────────
    // Verified from: services/product-service/src/routes/deal.routes.ts

    @GET("api/deals")
    suspend fun getDeals(
        @Query("product_id") productId: String? = null,
        @Query("active_only") activeOnly: Boolean? = null
    ): Response<DealResponse>

    @POST("api/deals")
    suspend fun createDeal(
        @Body request: CreateDealRequest
    ): Response<DealItem>

    @PUT("api/deals/{id}")
    suspend fun updateDeal(
        @Path("id") dealId: String,
        @Body request: UpdateDealRequest
    ): Response<DealItem>

    @DELETE("api/deals/{id}")
    suspend fun deleteDeal(
        @Path("id") dealId: String
    ): Response<Void>

    // Ã¢â€â‚¬Ã¢â€â‚¬ 3C: INVENTORY Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/inventory-service/src/routes/inventory.routes.ts

    @GET("api/inventory/{productId}")
    suspend fun getStock(
        @Path("productId") productId: String
    ): Response<InventoryResponse>

    @PATCH("api/inventory/{productId}")
    suspend fun updateStock(
        @Path("productId") productId: String,
        @Body request: UpdateStockRequest
    ): Response<InventoryResponse>

    @GET("api/inventory/low-stock")
    suspend fun getLowStockProducts(): Response<List<InventoryResponse>>

    @POST("api/products/bulk-prices")
    suspend fun bulkUpdatePrices(
        @Body request: BulkPriceUpdateRequest
    ): Response<Map<String, Int>>

    @POST("api/inventory/bulk-update")
    suspend fun bulkUpdateStock(
        @Body request: BulkStockUpdateRequest
    ): Response<ApiSuccessResponse>

    // Ã¢â€â‚¬Ã¢â€â‚¬ 3D: ORDERS Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/order-service/src/routes/order.routes.ts

    @GET("api/orders/seller")
    suspend fun getSellerOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<SellerOrderListResponse>

    @GET("api/orders/seller/{orderId}")
    suspend fun getSellerOrderDetail(
        @Path("orderId") orderId: String
    ): Response<SellerOrderResponse>

    @PATCH("api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<SellerOrderResponse>

    // Ã¢â€â‚¬Ã¢â€â‚¬ 3E: WALLET & PAYOUTS Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Verified from: services/payment-service/src/routes/payment.routes.ts
    // Gateway proxy: /api/seller/earnings Ã¢â€ â€™ payment-service

    @GET("api/seller/earnings")
    suspend fun getSellerEarnings(): Response<SellerEarningsResponse>

    @GET("api/seller/earnings/history")
    suspend fun getEarningsHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<EarningHistoryResponse>

    @GET("api/seller/payouts")
    suspend fun getSellerPayouts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PayoutListResponse>

    @POST("api/seller/payouts/request")
    suspend fun requestPayout(
        @Body request: PayoutRequestBody
    ): Response<SellerPayoutResponse>

    // -- PHASE 5: FCM Device Token Registration ---------------------------
    @POST("api/notifications/device-tokens")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): retrofit2.Response<AuthVerifyResponse>

    @DELETE("api/notifications/device-tokens")
    suspend fun unregisterDeviceToken(
        @Body request: UnregisterTokenRequest
    ): retrofit2.Response<AuthVerifyResponse>

    // ── AD CAMPAIGNS ──────────────────────────────────────────────────────────

    @GET("api/seller/ads")
    suspend fun getMyAds(): Response<AdListResponse>

    @POST("api/seller/ads/request")
    suspend fun requestAd(
        @Body request: AdRequest
    ): Response<AdCampaignResponse>

    // -- HELP & SUPPORT - TICKETS ------------------------------------------
    // Gateway: /api/support -> support-service
    // Role inferred from JWT claim (seller role)

    @retrofit2.http.POST("api/support/tickets")
    suspend fun createSupportTicket(
        @retrofit2.http.Body request: CreateTicketRequest
    ): Response<TicketResponse>

    @retrofit2.http.GET("api/support/tickets")
    suspend fun getSupportTickets(
        @retrofit2.http.Query("page")  page: Int? = null,
        @retrofit2.http.Query("limit") limit: Int? = null
    ): Response<TicketListResponse>

    @retrofit2.http.GET("api/support/tickets/{id}")
    suspend fun getSupportTicketById(
        @retrofit2.http.Path("id") ticketId: String
    ): Response<TicketDetail>

    @retrofit2.http.POST("api/support/tickets/{id}/messages")
    suspend fun replySupportTicket(
        @retrofit2.http.Path("id") ticketId: String,
        @retrofit2.http.Body request: ReplyRequest
    ): Response<MessageResponse>

    @retrofit2.http.PATCH("api/support/tickets/{id}/status")
    suspend fun updateSupportTicketStatus(
        @retrofit2.http.Path("id") ticketId: String,
        @retrofit2.http.Body request: TicketStatusRequest
    ): Response<TicketResponse>

    @retrofit2.http.POST("api/support/tickets/{id}/rate")
    suspend fun rateSupportTicket(
        @retrofit2.http.Path("id") ticketId: String,
        @retrofit2.http.Body request: RateTicketRequest
    ): Response<RateTicketResponse>

    @retrofit2.http.POST("api/support/tickets/{id}/attachments")
    suspend fun uploadSupportAttachment(
        @retrofit2.http.Path("id") ticketId: String,
        @retrofit2.http.Body request: Map<String, String>
    ): Response<AttachmentUploadResponse>

    // -- HELP & SUPPORT - KNOWLEDGE BASE -----------------------------------

    @retrofit2.http.GET("api/support/articles")
    suspend fun getSupportArticles(
        @retrofit2.http.Query("q")        query: String? = null,
        @retrofit2.http.Query("category") category: String? = null,
        @retrofit2.http.Query("page")     page: Int? = null,
        @retrofit2.http.Query("limit")    limit: Int? = null
    ): Response<ArticleListResponse>

    @retrofit2.http.GET("api/support/articles/{slug}")
    suspend fun getSupportArticleBySlug(
        @retrofit2.http.Path("slug") slug: String
    ): Response<ArticleResponse>

    @retrofit2.http.POST("api/support/articles/{slug}/feedback")
    suspend fun rateSupportArticle(
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Body request: ArticleFeedbackRequest
    ): Response<ArticleFeedbackResponse>

    // ── DISPUTES (Seller) ───────────────────────────────────────────────────
    // GET  /api/seller/orders/disputes   → list seller disputes
    // GET  /api/orders/:id/dispute       → get dispute for an order
    // PATCH /api/orders/:id/dispute/:disputeId → seller respond to dispute

    @GET("api/seller/orders/disputes")
    suspend fun getSellerDisputes(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null
    ): Response<DisputeListResponse>

    @GET("api/orders/{orderId}/dispute")
    suspend fun getOrderDispute(
        @Path("orderId") orderId: String
    ): Response<DisputeResponse>

    @PATCH("api/orders/{orderId}/dispute/{disputeId}")
    suspend fun respondToDispute(
        @Path("orderId") orderId: String,
        @Path("disputeId") disputeId: String,
        @Body request: DisputeRespondRequest
    ): Response<DisputeResponse>

    // ── REVIEWS — Phase 6A ────────────────────────────────────────────────────
    // GET /api/products/:id/reviews — view reviews on own products (public)

    @GET("api/products/{id}/reviews")
    suspend fun getProductReviews(
        @Path("id") productId: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: String? = null
    ): Response<SellerReviewListResponse>

    @GET("api/products/seller/reviews")
    suspend fun getSellerReviews(
        @Query("sort") sort: String? = null
    ): Response<SellerReviewListResponse>

    // ── AD CAMPAIGNS — Phase 6C ───────────────────────────────────────────────
    // POST /api/seller/ads/request — request a new ad campaign
    // GET  /api/seller/ads/         — list own ad campaigns

    @POST("api/seller/ads/request")
    suspend fun requestAdCampaign(
        @Body request: AdCampaignRequest
    ): Response<AdCampaignResponse>

    @GET("api/seller/ads/")
    suspend fun getMyAdCampaigns(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<AdCampaignListResponse>

    // ── STOREFRONT — Phase 6D ─────────────────────────────────────────────────
    // PATCH /api/users/storefront          — update own storefront
    // GET   /api/users/sellers/:id/repeat-buyers — repeat buyers analytics

    @PATCH("api/users/storefront")
    suspend fun updateStorefront(
        @Body request: UpdateStorefrontRequest
    ): Response<StorefrontUpdateResponse>

    @GET("api/users/sellers/{id}/repeat-buyers")
    suspend fun getRepeatBuyers(
        @Path("id") sellerId: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<RepeatBuyersResponse>

    @GET("api/seller/orders/disputes/")
    suspend fun getSellerDisputes(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<DisputeListResponse>

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────
    // Section 17 of BACKEND_API_REFERENCE.md
    // Same as customer app — role-based filtering done server-side via JWT

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page")  page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("read")  read: Boolean? = null
    ): Response<NotificationListResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<UnreadCountResponse>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") notificationId: String
    ): Response<ApiSuccessResponse>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiSuccessResponse>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") notificationId: String
    ): Response<ApiSuccessResponse>

    @GET("api/notifications/preferences")
    suspend fun getNotificationPreferences(): Response<NotificationPreferencesResponse>

    @PATCH("api/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Body request: UpdatePreferencesRequest
    ): Response<NotificationPreferencesResponse>

    // Unregister all tokens (on full logout / account switch)
    @DELETE("api/notifications/device-tokens/all")
    suspend fun unregisterAllDeviceTokens(): Response<ApiSuccessResponse>

    // ── INVOICE ───────────────────────────────────────────────────────────────
    // GET /api/orders/:id/invoice — download invoice PDF

    @GET("api/orders/{id}/invoice")
    suspend fun getOrderInvoice(
        @Path("id") orderId: String
    ): Response<InvoiceResponse>

    // ── BULK PRODUCT IMPORT ───────────────────────────────────────────────────
    // POST /api/products/bulk-import — bulk import products

    @POST("api/products/bulk-import")
    suspend fun bulkImportProducts(
        @Body request: BulkImportRequest
    ): Response<BulkImportResponse>

    // -- MULTI-BRAND MANAGEMENT — Phase 1 ------------------------------------
    // Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md
    // Verified from: services/user-service — GET/POST /api/users/brands

    @GET("api/users/brands")
    suspend fun getBrands(): Response<BrandsResponse>

    @POST("api/users/brands")
    suspend fun createBrand(
        @Body request: CreateBrandRequest
    ): Response<CreateBrandResponse>
}
