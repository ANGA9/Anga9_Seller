package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data_models.RepeatBuyer
import com.anga9.seller.data_models.SellerStorefront
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.TokenManager

/**
 * StorefrontRepository — Seller App
 *
 * GET /api/users/sellers/:id/repeat-buyers  → repeat buyers list (LIVE)
 * GET /api/users/sellers/:id/storefront     → public seller profile (stub — no response body defined)
 * PATCH /api/users/storefront               → update storefront (stub)
 */
class StorefrontRepository(private val context: Context) {

    private val api by lazy { ApiClient.getApiService(context) }

    suspend fun getStorefront(sellerId: String): Result<SellerStorefront> {
        // Backend endpoint exists but response body not fully defined yet
        return Result.failure(Exception("Seller Storefront feature coming soon"))
    }

    suspend fun updateStorefront(
        request: com.anga9.seller.network.model.UpdateStorefrontRequest
    ): Result<com.anga9.seller.network.model.StorefrontUpdateResponse> {
        return try {
            val response = api.updateStorefront(request)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to update storefront: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * GET /api/users/sellers/:id/repeat-buyers
     * Maps RepeatBuyerItem (API) → RepeatBuyer (UI model)
     */
    suspend fun getRepeatBuyers(sellerId: String): Result<List<RepeatBuyer>> {
        return try {
            val response = api.getRepeatBuyers(sellerId)
            if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                val buyers = items.map { item ->
                    RepeatBuyer(
                        buyerId        = item.customerId,
                        buyerName      = item.customerName,
                        buyerPhone     = "",           // not in API response
                        buyerEmail     = "",           // not in API response
                        totalOrders    = item.orderCount,
                        totalOrderValue = item.totalSpent,
                        lastOrderDate  = parseIsoToMillis(item.lastOrderAt),
                        lastOrderId    = "",           // not in API response
                        outstandingAmount = 0.0,       // not in API response
                        pendingOrdersCount = 0,        // not in API response
                        firstOrderDate = 0L,           // not in API response
                        avgOrderValue  = if (item.orderCount > 0) item.totalSpent / item.orderCount else 0.0,
                        isRegular      = item.orderCount >= 3
                    )
                }
                Result.success(buyers)
            } else {
                Result.failure(Exception("Failed to load repeat buyers: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoToMillis(iso: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(iso)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                sdf2.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf2.parse(iso)?.time ?: 0L
            } catch (e2: Exception) { 0L }
        }
    }

    fun getSavedSellerId(): String? = TokenManager.getUserId(context)
}