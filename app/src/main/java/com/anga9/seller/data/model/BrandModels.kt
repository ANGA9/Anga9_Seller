package com.anga9.seller.data.model

// ─────────────────────────────────────────────────────────────────────────────
// MULTI-BRAND MANAGEMENT — Phase 1 Data Models
// Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md — Phase 1.1
//
// IMPORTANT: GET /api/users/brands  -> flat join  -> parsed as BrandListItem
//            POST /api/users/brands -> nested obj -> parsed as CreateBrandResponse
//            Both are mapped to the common BrandUser UI model via extension fns.
// ─────────────────────────────────────────────────────────────────────────────

// Step 1: Common UI model (used everywhere in the app after parsing)

/**
 * The canonical in-memory model the UI works with.
 * Never parsed directly from JSON — always produced by toBrandUser() extensions.
 */
data class BrandUser(
    val id: String,              // Child brand's users.id (UUID)
    val displayName: String?,    // Brand display name / full_name
    val storeSlug: String?,      // Unique slug
    val storeName: String?,      // seller_profiles.store_name
    val logoUrl: String?,        // seller_profiles.logo_url
    val parentUserId: String?,   // null if this IS the parent brand
    val isApproved: Boolean = false
)

// Step 2: GET /api/users/brands response models (flat join)

/**
 * Parses one entry from GET /api/users/brands.
 * Backend returns a flat SQL join of users + seller_profiles.
 *
 * Example JSON:
 * {
 *   "id": "uuid",
 *   "full_name": "Parent Name",
 *   "parent_user_id": null,
 *   "seller_profiles": { "store_name": "My Store", "store_slug": "my-store" }
 * }
 */
data class BrandListItem(
    val id: String,
    val full_name: String?,
    val parent_user_id: String?,
    val seller_profiles: BrandSellerProfile?
)

data class BrandSellerProfile(
    val store_name: String?,
    val store_slug: String?,
    val logo_url: String?,
    val is_approved: Boolean = false
)

/** Top-level wrapper for GET /api/users/brands */
data class BrandsResponse(
    val brands: List<BrandListItem>
)

/** Maps a flat GET response item to the common BrandUser UI model */
fun BrandListItem.toBrandUser() = BrandUser(
    id = id,
    displayName = full_name,
    storeSlug = seller_profiles?.store_slug,
    storeName = seller_profiles?.store_name,
    logoUrl = seller_profiles?.logo_url,
    parentUserId = parent_user_id,
    isApproved = seller_profiles?.is_approved ?: false
)

// Step 3: POST /api/users/brands response models (nested object)

/**
 * Parses the response from POST /api/users/brands.
 * Backend creates two rows and returns them nested — NOT same shape as GET.
 *
 * Example JSON:
 * {
 *   "brand": {
 *     "user":    { "id": "uuid", "parent_user_id": "parent-uuid", "role": "seller" },
 *     "profile": { "user_id": "uuid", "store_name": "New Brand", "store_slug": "new-brand" }
 *   }
 * }
 */
data class CreateBrandResponse(
    val brand: CreatedBrandWrapper
)

data class CreatedBrandWrapper(
    val user: CreatedBrandUser,
    val profile: CreatedBrandProfile
)

data class CreatedBrandUser(
    val id: String,
    val parent_user_id: String?,
    val role: String?
)

data class CreatedBrandProfile(
    val user_id: String,
    val store_name: String?,
    val store_slug: String?
)

/** Maps the nested POST response to the common BrandUser UI model */
fun CreateBrandResponse.toBrandUser() = BrandUser(
    id = brand.user.id,
    displayName = brand.profile.store_name,
    storeSlug = brand.profile.store_slug,
    storeName = brand.profile.store_name,
    logoUrl = null,             // Not returned on creation; fetched on next GET
    parentUserId = brand.user.parent_user_id,
    isApproved = false          // Always pending approval on first creation
)

// Step 4: Request model for POST /api/users/brands

data class CreateBrandRequest(
    val store_slug: String,
    val store_name: String,
    val display_name: String? = null
)
