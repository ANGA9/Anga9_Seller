package com.anga9.seller.ui.deals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.DealItem
import com.anga9.seller.network.model.DealStatus
import com.anga9.seller.network.model.UpdateDealRequest
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch

class DealsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ApiClient.getApiService(application)

    private val _uiState = MutableLiveData<UiState<List<DealItem>>>()
    val uiState: LiveData<UiState<List<DealItem>>> get() = _uiState

    private val _deleteState = MutableLiveData<UiState<Unit>>()
    val deleteState: LiveData<UiState<Unit>> get() = _deleteState

    private val _toggleState = MutableLiveData<UiState<DealItem>>()
    val toggleState: LiveData<UiState<DealItem>> get() = _toggleState

    private var allDeals = listOf<DealItem>()

    fun loadDeals(tab: String = "All Deals", query: String = "") {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getDeals()
                if (response.isSuccessful && response.body() != null) {
                    allDeals = response.body()!!.deals
                    applyFilters(tab, query)
                } else {
                    _uiState.value = UiState.Error("Failed to load deals: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun applyFilters(tab: String, query: String) {
        val filtered = allDeals.filter { deal ->
            val matchesQuery = if (query.isNotEmpty()) {
                deal.displayProduct?.displayTitle?.contains(query, ignoreCase = true) == true
            } else true

            val matchesTab = when (tab) {
                "Active" -> deal.currentStatus == DealStatus.ACTIVE
                "Scheduled" -> deal.currentStatus == DealStatus.SCHEDULED
                "Expired" -> deal.currentStatus == DealStatus.EXPIRED
                else -> true // All Deals
            }

            matchesQuery && matchesTab
        }
        _uiState.value = UiState.Success(filtered)
    }

    fun togglePauseResume(deal: DealItem) {
        // Only ACTIVE or PAUSED can be toggled in UI usually, but backend `active: Boolean` handles it.
        // If it's active, we send active=false to pause it. If it's paused, we send active=true.
        val newActiveStatus = !deal.isActive
        _toggleState.value = UiState.Loading
        
        // Optimistic UI update
        val index = allDeals.indexOfFirst { it.id == deal.id }
        if (index != -1) {
            val updatedDeals = allDeals.toMutableList()
            updatedDeals[index] = deal.copy(isActive = newActiveStatus)
            allDeals = updatedDeals
            // Refresh current list state
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                val filteredIndex = currentState.data.indexOfFirst { it.id == deal.id }
                if (filteredIndex != -1) {
                    val newFiltered = currentState.data.toMutableList()
                    newFiltered[filteredIndex] = updatedDeals[index]
                    _uiState.value = UiState.Success(newFiltered)
                }
            }
        }

        viewModelScope.launch {
            try {
                val response = api.updateDeal(deal.id, UpdateDealRequest(active = newActiveStatus))
                if (response.isSuccessful && response.body() != null) {
                    _toggleState.value = UiState.Success(response.body()!!)
                    // Ensure the backend response is synced with local state
                    val finalIndex = allDeals.indexOfFirst { it.id == deal.id }
                    if (finalIndex != -1) {
                        val finalDeals = allDeals.toMutableList()
                        finalDeals[finalIndex] = response.body()!!
                        allDeals = finalDeals
                        // We don't necessarily need to trigger UI filter again unless necessary, 
                        // as optimistic update already handled the UI change.
                    }
                } else {
                    // Rollback on failure
                    rollbackToggle(deal)
                    _toggleState.value = UiState.Error("Failed to update deal status")
                }
            } catch (e: Exception) {
                rollbackToggle(deal)
                _toggleState.value = UiState.Error(e.message ?: "Failed to update deal")
            }
        }
    }

    private fun rollbackToggle(originalDeal: DealItem) {
        val index = allDeals.indexOfFirst { it.id == originalDeal.id }
        if (index != -1) {
            val updatedDeals = allDeals.toMutableList()
            updatedDeals[index] = originalDeal
            allDeals = updatedDeals
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                val filteredIndex = currentState.data.indexOfFirst { it.id == originalDeal.id }
                if (filteredIndex != -1) {
                    val newFiltered = currentState.data.toMutableList()
                    newFiltered[filteredIndex] = originalDeal
                    _uiState.value = UiState.Success(newFiltered)
                }
            }
        }
    }

    fun deleteDeal(dealId: String) {
        _deleteState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.deleteDeal(dealId)
                if (response.isSuccessful) {
                    _deleteState.value = UiState.Success(Unit)
                    // Remove locally
                    allDeals = allDeals.filter { it.id != dealId }
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = UiState.Success(currentState.data.filter { it.id != dealId })
                    }
                } else {
                    _deleteState.value = UiState.Error("Failed to delete deal")
                }
            } catch (e: Exception) {
                _deleteState.value = UiState.Error(e.message ?: "Error deleting deal")
            }
        }
    }
}
