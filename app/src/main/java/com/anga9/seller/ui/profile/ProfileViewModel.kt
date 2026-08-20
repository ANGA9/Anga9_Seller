package com.anga9.seller.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.SellerStatsResponse
import com.anga9.seller.network.model.SubmitKycRequest
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application.applicationContext)

    private val _profileState = MutableStateFlow<UiState<SellerProfileResponse>>(UiState.Idle)
    val profileState: StateFlow<UiState<SellerProfileResponse>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<SellerProfileResponse>>(UiState.Idle)
    val updateState: StateFlow<UiState<SellerProfileResponse>> = _updateState.asStateFlow()

    private val _statsState = MutableStateFlow<UiState<SellerStatsResponse>>(UiState.Idle)
    val statsState: StateFlow<UiState<SellerStatsResponse>> = _statsState.asStateFlow()

    private val _kycState = MutableStateFlow<UiState<SellerProfileResponse>>(UiState.Idle)
    val kycState: StateFlow<UiState<SellerProfileResponse>> = _kycState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            val result = repository.getSellerProfile()
            _profileState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to load profile") }
            )
        }
    }

    fun updateProfile(request: UpdateSellerProfileRequest) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            val result = repository.updateSellerProfile(request)
            _updateState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to update profile") }
            )
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _statsState.value = UiState.Loading
            val result = repository.getSellerStats()
            _statsState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to load stats") }
            )
        }
    }

    fun submitKyc(request: SubmitKycRequest) {
        viewModelScope.launch {
            _kycState.value = UiState.Loading
            val result = repository.submitKyc(request)
            _kycState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "KYC submission failed") }
            )
        }
    }

    private val _ifscState = MutableStateFlow<UiState<Pair<String, String>>>(UiState.Idle)
    val ifscState: StateFlow<UiState<Pair<String, String>>> = _ifscState.asStateFlow()

    fun lookupIfsc(ifsc: String) {
        if (ifsc.trim().length != 11) return
        viewModelScope.launch {
            _ifscState.value = UiState.Loading
            val result = repository.lookupIfsc(ifsc)
            _ifscState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Invalid IFSC") }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }


    fun getSavedSellerId(): String? = repository.getSavedSellerId()
}
