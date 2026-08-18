package com.anga9.seller.ui.b2b

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.StorefrontRepository
import com.anga9.seller.data_models.RepeatBuyer
import com.anga9.seller.data_models.SellerStorefront
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch

enum class BuyerFilter { ALL, WITH_OUTSTANDING, REGULAR }

class B2BViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = StorefrontRepository(application.applicationContext)

    // ── Storefront ────────────────────────────────────────────────
    private val _storefrontState = MutableLiveData<UiState<SellerStorefront>>()
    val storefrontState: LiveData<UiState<SellerStorefront>> = _storefrontState
    val storefront: LiveData<UiState<SellerStorefront>> = _storefrontState

    private val _updateState = MutableLiveData<UiState<Boolean>>(UiState.Idle)
    val updateState: LiveData<UiState<Boolean>> = _updateState

    private val _bannerUpload = MutableLiveData<UiState<String>>(UiState.Idle)
    val bannerUpload: LiveData<UiState<String>> = _bannerUpload

    // ── Repeat Buyers ─────────────────────────────────────────────
    private val _buyers = MutableLiveData<UiState<List<RepeatBuyer>>>(UiState.Idle)
    val buyers: LiveData<UiState<List<RepeatBuyer>>> = _buyers

    // Alias used by older code
    private val _repeatBuyersState = _buyers
    val repeatBuyersState: LiveData<UiState<List<RepeatBuyer>>> = _buyers

    private val _markPaidState = MutableLiveData<UiState<Boolean>>(UiState.Idle)
    val markPaidState: LiveData<UiState<Boolean>> = _markPaidState

    private val _buyerOrders = MutableLiveData<UiState<List<Map<String, Any?>>>>(UiState.Idle)
    val buyerOrders: LiveData<UiState<List<Map<String, Any?>>>> = _buyerOrders

    var currentFilter: BuyerFilter = BuyerFilter.ALL

    // ── Storefront Methods ────────────────────────────────────────
    fun loadStorefront(sellerId: String) {
        viewModelScope.launch {
            _storefrontState.value = UiState.Loading
            val result = repo.getStorefront(sellerId)
            _storefrontState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Storefront coming soon") }
            )
        }
    }

    fun updateStorefront(sellerId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            _updateState.value = UiState.Error("Storefront update coming soon")
        }
    }

    fun uploadBanner(sellerId: String, bytes: ByteArray) {
        viewModelScope.launch {
            _bannerUpload.value = UiState.Loading
            _bannerUpload.value = UiState.Error("Banner upload coming soon")
        }
    }

    // ── Repeat Buyers Methods ─────────────────────────────────────
    fun loadBuyers(sellerId: String) {
        viewModelScope.launch {
            _buyers.value = UiState.Loading
            val result = repo.getRepeatBuyers(sellerId)
            _buyers.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to load buyers") }
            )
        }
    }

    // Alias for older code that calls loadRepeatBuyers
    fun loadRepeatBuyers(sellerId: String) = loadBuyers(sellerId)

    fun markCodPaid(orderId: String, sellerId: String) {
        viewModelScope.launch {
            _markPaidState.value = UiState.Loading
            // Stub — COD mark-paid endpoint not in BACKEND_API_REFERENCE yet
            _markPaidState.value = UiState.Error("Mark paid feature coming soon")
        }
    }

    fun loadBuyerOrders(sellerId: String, buyerId: String) {
        viewModelScope.launch {
            _buyerOrders.value = UiState.Loading
            // Stub — buyer order history endpoint not in BACKEND_API_REFERENCE yet
            _buyerOrders.value = UiState.Success(emptyList())
        }
    }

    fun getFilteredBuyers(buyers: List<RepeatBuyer>): List<RepeatBuyer> {
        return when (currentFilter) {
            BuyerFilter.ALL              -> buyers
            BuyerFilter.WITH_OUTSTANDING -> buyers.filter { it.outstandingAmount > 0 }
            BuyerFilter.REGULAR          -> buyers.filter { it.isRegular }
        }
    }
}