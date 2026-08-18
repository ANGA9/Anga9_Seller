package com.anga9.seller.ui.returns

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data_models.ReturnRequest
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReturnsViewModel : ViewModel() {
    private val repository = ReturnsRepository()

    private val _returnsState = MutableStateFlow<UiState<List<ReturnRequest>>>(UiState.Idle)
    val returnsState: StateFlow<UiState<List<ReturnRequest>>> = _returnsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val actionState: StateFlow<UiState<Boolean>> = _actionState.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _returnDetail = MutableStateFlow<UiState<ReturnRequest>>(UiState.Idle)
    val returnDetail: StateFlow<UiState<ReturnRequest>> = _returnDetail.asStateFlow()

    fun loadMyReturns(statusFilter: String? = null) {
        viewModelScope.launch {
            repository.getMyReturns(statusFilter).collect { result ->
                _returnsState.value = when (result) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success<*> -> UiState.Success((result.data as? List<ReturnRequest>) ?: emptyList())
                    is Resource.Error -> UiState.Error(result.message ?: "Failed to load")
                }
            }
        }
    }

    fun loadPendingCount() {
        viewModelScope.launch {
            _pendingCount.value = repository.getPendingReturnsCount()
        }
    }

    fun loadReturnById(returnId: String) {
        viewModelScope.launch {
            _returnDetail.value = UiState.Loading
            val result = repository.getReturnById(returnId)
            _returnDetail.value = when (result) {
                is Resource.Success<*> -> UiState.Success(result.data as ReturnRequest)
                is Resource.Error -> UiState.Error(result.message ?: "Failed to load")
                else -> UiState.Error("Unknown error")
            }
        }
    }

    fun approveReturn(returnId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val result = repository.approveReturn(returnId)
            _actionState.value = when (result) {
                is Resource.Success<*> -> UiState.Success(true)
                is Resource.Error -> UiState.Error(result.message ?: "Failed to approve")
                else -> UiState.Error("Unknown error")
            }
        }
    }

    fun rejectReturn(returnId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val result = repository.rejectReturn(returnId, reason)
            _actionState.value = when (result) {
                is Resource.Success<*> -> UiState.Success(true)
                is Resource.Error -> UiState.Error(result.message ?: "Failed to reject")
                else -> UiState.Error("Unknown error")
            }
        }
    }

    fun markItemReceived(returnId: String, inspectionResult: String, photoUris: List<Uri>) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val result = repository.markItemReceived(returnId, inspectionResult, photoUris)
            _actionState.value = when (result) {
                is Resource.Success<*> -> UiState.Success(true)
                is Resource.Error -> UiState.Error(result.message ?: "Failed to mark received")
                else -> UiState.Error("Unknown error")
            }
        }
    }

    fun resetActionState() { _actionState.value = UiState.Idle }
}