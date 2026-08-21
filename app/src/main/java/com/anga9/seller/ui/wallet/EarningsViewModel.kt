package com.anga9.seller.ui.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.WalletRepository
import com.anga9.seller.network.model.EarningHistoryResponse
import com.anga9.seller.network.model.SellerEarningsResponse
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EarningsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WalletRepository(application.applicationContext)

    private val _earningsState = MutableLiveData<Resource<SellerEarningsResponse>>()
    val earningsState: LiveData<Resource<SellerEarningsResponse>> = _earningsState

    private val _historyState = MutableLiveData<Resource<EarningHistoryResponse>>()
    val historyState: LiveData<Resource<EarningHistoryResponse>> = _historyState

    fun loadEarnings() {
        if (_earningsState.value !is Resource.Success) {
            _earningsState.value = Resource.Loading()
        }
        viewModelScope.launch {
            repository.getEarnings().collectLatest { resource ->
                _earningsState.postValue(resource)
            }
        }
    }

    fun loadEarningsHistory(page: Int = 1) {
        if (page == 1) {
            _historyState.value = Resource.Loading()
        }
        viewModelScope.launch {
            repository.getEarningsHistory(page).collectLatest { resource ->
                _historyState.postValue(resource)
            }
        }
    }
}
