package com.anga9.seller.ui.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.WalletRepository
import com.anga9.seller.network.model.SellerEarningsResponse
import com.anga9.seller.network.model.PayoutListResponse
import com.anga9.seller.network.model.SellerPayoutResponse
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PayoutsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WalletRepository(application)

    private val _payoutsState = MutableStateFlow<Resource<PayoutListResponse>>(Resource.Loading())
    val payoutsState: StateFlow<Resource<PayoutListResponse>> = _payoutsState.asStateFlow()

    private val _earningsState = MutableStateFlow<Resource<SellerEarningsResponse>>(Resource.Loading())
    val earningsState: StateFlow<Resource<SellerEarningsResponse>> = _earningsState.asStateFlow()

    private val _requestState = MutableStateFlow<Resource<SellerPayoutResponse>?>(null)
    val requestState: StateFlow<Resource<SellerPayoutResponse>?> = _requestState.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        fetchEarnings()
        fetchPayouts()
    }

    private fun fetchEarnings() {
        viewModelScope.launch {
            if (_earningsState.value !is Resource.Success) {
                _earningsState.value = Resource.Loading()
            }
            repository.getEarnings().collect { result ->
                _earningsState.value = result
            }
        }
    }

    private fun fetchPayouts() {
        viewModelScope.launch {
            if (_payoutsState.value !is Resource.Success) {
                _payoutsState.value = Resource.Loading()
            }
            repository.getPayouts(page = 1).collect { result ->
                _payoutsState.value = result
            }
        }
    }

    fun requestPayout(amount: Double) {
        viewModelScope.launch {
            _requestState.value = Resource.Loading()
            val result = repository.requestPayout(amount = amount)
            result.fold(
                onSuccess = { response ->
                    _requestState.value = Resource.Success(response)
                    fetchData() // Refresh data after successful payout request
                },
                onFailure = { error ->
                    _requestState.value = Resource.Error(error.message ?: "Failed to request payout")
                }
            )
        }
    }

    fun resetRequestState() {
        _requestState.value = null
    }
}
