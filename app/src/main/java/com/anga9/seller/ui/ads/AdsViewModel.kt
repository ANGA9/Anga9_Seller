package com.anga9.seller.ui.ads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.AdRepository
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.network.model.AdCampaignResponse
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AdRepository(application.applicationContext)
    private val productRepo = ProductRepository(application.applicationContext)

    private val _campaignsState = MutableStateFlow<UiState<List<AdCampaignResponse>>>(UiState.Idle)
    val campaignsState: StateFlow<UiState<List<AdCampaignResponse>>> = _campaignsState.asStateFlow()
    
    private val _productCountState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val productCountState: StateFlow<UiState<Int>> = _productCountState.asStateFlow()

    fun loadMyCampaigns() {
        viewModelScope.launch {
            _campaignsState.value = UiState.Loading
            repo.getMyAds().collectLatest { resource ->
                when (resource) {
                    is Resource.Success<*> -> _campaignsState.value = UiState.Success((resource.data as? List<AdCampaignResponse>) ?: emptyList())
                    is Resource.Error -> _campaignsState.value = UiState.Error(resource.message ?: "Failed")
                    is Resource.Loading<*> -> _campaignsState.value = UiState.Loading
                }
            }
        }
    }
    
    fun checkPublishedProductCount() {
        viewModelScope.launch {
            _productCountState.value = UiState.Loading
            try {
                val res = productRepo.getMyProducts("published")
                res.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success<*> -> {
                            val dataList = resource.data as? List<*>
                            _productCountState.value = UiState.Success(dataList?.size ?: 0)
                        }
                        is Resource.Error -> _productCountState.value = UiState.Error(resource.message ?: "Failed")
                        is Resource.Loading<*> -> {}
                    }
                }
            } catch (e: Exception) {
                _productCountState.value = UiState.Error(e.message ?: "Error fetching product count")
            }
        }
    }

    // ── STUBS TO PREVENT COMPILATION ERRORS IN OTHER ACTIVITIES ───────────

    // Single campaign state (for AdPreviewActivity)
    private val _campaignState = MutableStateFlow<UiState<com.anga9.seller.data.model.AdCampaign>>(UiState.Idle)
    val campaignState: StateFlow<UiState<com.anga9.seller.data.model.AdCampaign>> = _campaignState.asStateFlow()

    private val _pricingState = MutableStateFlow<Resource<List<com.anga9.seller.data.model.AdPricing>>>(Resource.Loading())
    val pricingState: StateFlow<Resource<List<com.anga9.seller.data.model.AdPricing>>> = _pricingState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionState: StateFlow<UiState<String>> = _actionState.asStateFlow()

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }

    fun loadPricing() {
        // Stub
    }

    fun submitAdRequest(ad: com.anga9.seller.data.model.AdCampaign) {
        // Stub
    }

    fun payAndSubmit(campaignId: String, amount: Double = 0.0) {
        // Stub
    }

    fun approvePreview(campaignId: String) {
        // Stub
    }

    fun requestPreviewChanges(campaignId: String, feedback: String) {
        // Stub
    }

    fun loadCampaignById(campaignId: String) {
        // Stub
    }

    fun markPreviewViewed(campaignId: String) {
        // Stub
    }
}