package com.anga9.seller.ui.disputes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.DisputesRepository
import com.anga9.seller.network.model.DisputeItem
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DisputesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DisputesRepository(application.applicationContext)

    private val _disputesState = MutableStateFlow<UiState<List<DisputeItem>>>(UiState.Loading)
    val disputesState: StateFlow<UiState<List<DisputeItem>>> = _disputesState.asStateFlow()

    private val _activeIssuesCount = MutableStateFlow(0)
    val activeIssuesCount: StateFlow<Int> = _activeIssuesCount.asStateFlow()

    private val _respondState = MutableStateFlow<UiState<DisputeItem?>>(UiState.Success(null))
    val respondState: StateFlow<UiState<DisputeItem?>> = _respondState.asStateFlow()

    private var cachedAllDisputes: List<DisputeItem> = emptyList()
    var currentFilterKey: String = "all"
        private set

    init {
        loadDisputes("all")
    }

    fun loadDisputes(filterKey: String = currentFilterKey) {
        currentFilterKey = filterKey
        viewModelScope.launch {
            _disputesState.value = UiState.Loading

            // We fetch the full list to accurately compute activeIssuesCount, then apply client filtering
            repository.getSellerDisputes(status = null).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _disputesState.value = UiState.Loading
                    }
                    is Resource.Success -> {
                        val fullList = resource.data ?: emptyList()
                        cachedAllDisputes = fullList
                        
                        // Active count strictly counts action_required (open)
                        val activeCount = fullList.count { DisputeConfig.isActionRequired(it.status) }
                        _activeIssuesCount.value = activeCount

                        val filtered = filterList(fullList, filterKey)
                        _disputesState.value = UiState.Success(filtered)
                    }
                    is Resource.Error -> {
                        _disputesState.value = UiState.Error(resource.message ?: "Failed to load disputes")
                    }
                }
            }
        }
    }

    fun setFilter(filterKey: String) {
        currentFilterKey = filterKey
        val filtered = filterList(cachedAllDisputes, filterKey)
        _disputesState.value = UiState.Success(filtered)
    }

    private fun filterList(list: List<DisputeItem>, filterKey: String): List<DisputeItem> {
        return when (filterKey.lowercase()) {
            "open", "action_required" -> list.filter { DisputeConfig.isActionRequired(it.status) }
            "seller_responded", "in_review", "admin_review" -> list.filter {
                val s = it.status.lowercase()
                s == "seller_responded" || s == "admin_review" || s == "in_review"
            }
            "resolved", "closed" -> list.filter { DisputeConfig.isResolved(it.status) }
            else -> list // "all"
        }
    }

    fun submitResponse(
        orderId: String,
        disputeId: String,
        responseText: String,
        qcStatus: String
    ) {
        viewModelScope.launch {
            _respondState.value = UiState.Loading
            val result = repository.respondToDispute(
                orderId = orderId,
                disputeId = disputeId,
                responseText = responseText,
                qcStatus = qcStatus
            )
            result.onSuccess { updatedItem ->
                _respondState.value = UiState.Success(updatedItem)
                // Reload list to refresh UI
                loadDisputes(currentFilterKey)
            }.onFailure { error ->
                _respondState.value = UiState.Error(error.localizedMessage ?: "Failed to send response")
            }
        }
    }

    fun resetRespondState() {
        _respondState.value = UiState.Success(null)
    }
}
