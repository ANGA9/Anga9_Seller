package com.anga9.seller.ui.brand

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.BrandUser
import com.anga9.seller.data.model.CreateBrandRequest
import com.anga9.seller.data.repository.BrandRepository
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * ViewModel for Multi-Brand Management — Phase 2.
 * Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md — Phase 2.1
 *
 * Handles brand list loading, brand switching, and new brand creation.
 * Sends ACTION_BRAND_SWITCHED broadcast after every switch so DashboardActivity
 * and other screens can refresh their data.
 */
class BrandViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BrandRepository(application)

    private val _brands = MutableLiveData<Resource<List<BrandUser>>>()
    val brands: LiveData<Resource<List<BrandUser>>> = _brands

    private val _activeBrandId = MutableLiveData<String?>()
    val activeBrandId: LiveData<String?> = _activeBrandId

    private val _createResult = MutableLiveData<Resource<BrandUser>>()
    val createResult: LiveData<Resource<BrandUser>> = _createResult

    // ── Brand Loading ─────────────────────────────────────────────────────

    /**
     * Loads all brands for the logged-in parent seller.
     * Index 0 of the returned list is always the parent (the authenticated user).
     */
    fun loadBrands() {
        viewModelScope.launch {
            _brands.value = Resource.Loading()
            _brands.value = repo.getBrands()
        }
    }

    // ── Brand Switching ──────────────────────────────────────────────────

    /**
     * Switches the active brand context.
     * Pass null to switch back to the parent (own account).
     * Persists to TokenManager and broadcasts ACTION_BRAND_SWITCHED.
     */
    fun switchBrand(context: Context, brandId: String?) {
        if (brandId != null) {
            TokenManager.setActiveBrandId(context, brandId)
        } else {
            TokenManager.clearActiveBrandId(context)
        }
        _activeBrandId.value = brandId

        // Notify DashboardActivity (and any other listeners) to refresh data
        val intent = Intent(ACTION_BRAND_SWITCHED)
        context.sendBroadcast(intent)
    }

    /**
     * Returns the display name of the currently active brand from the loaded list.
     * Falls back to the first brand (parent) if no active brand is set.
     */
    fun getCurrentBrandName(brands: List<BrandUser>, context: Context): String {
        val activeId = TokenManager.getActiveBrandId(context)
        return brands.find { it.id == activeId }?.storeName
            ?: brands.firstOrNull()?.storeName
            ?: "My Store"
    }

    // ── Brand Creation ───────────────────────────────────────────────────

    /**
     * Creates a new child brand via POST /api/users/brands.
     * On success, the new brand is ready for immediate switching.
     */
    fun createBrand(storeName: String, storeSlug: String) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            val request = CreateBrandRequest(store_slug = storeSlug, store_name = storeName)
            // Repository maps nested POST response -> BrandUser automatically
            _createResult.value = repo.createBrand(request)
        }
    }

    companion object {
        /** Broadcast sent after every brand switch. DashboardActivity listens for this. */
        const val ACTION_BRAND_SWITCHED = "com.anga9.seller.BRAND_SWITCHED"
    }
}
