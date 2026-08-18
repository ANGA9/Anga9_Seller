package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.data.model.BrandUser
import com.anga9.seller.data.model.CreateBrandRequest
import com.anga9.seller.data.model.toBrandUser
import com.anga9.seller.network.ApiClient
import com.anga9.seller.utils.Resource

/**
 * Repository for Multi-Brand Management API calls.
 * Phase 1 — Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md
 *
 * Handles both endpoints and normalises their different response shapes
 * into the common BrandUser UI model before returning to the ViewModel.
 */
class BrandRepository(private val context: Context) {

    private val api = ApiClient.getApiService(context)

    /**
     * Fetches all brands for the logged-in parent seller.
     * GET /api/users/brands
     *
     * The response is a flat join (BrandListItem list).
     * Each item is mapped to BrandUser via toBrandUser() before returning.
     */
    suspend fun getBrands(): Resource<List<BrandUser>> {
        return try {
            val response = api.getBrands()
            if (response.isSuccessful) {
                val brands = response.body()?.brands?.map { it.toBrandUser() } ?: emptyList()
                Resource.Success(brands)
            } else {
                Resource.Error("Failed to load brands: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error while loading brands")
        }
    }

    /**
     * Creates a new child brand under the logged-in parent seller.
     * POST /api/users/brands
     *
     * The response is a nested { user, profile } object (CreateBrandResponse).
     * It is mapped to BrandUser via toBrandUser() before returning.
     */
    suspend fun createBrand(request: CreateBrandRequest): Resource<BrandUser> {
        return try {
            val response = api.createBrand(request)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Resource.Error("Empty response from server")
                Resource.Success(body.toBrandUser())
            } else {
                Resource.Error("Failed to create brand: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error while creating brand")
        }
    }
}
