package com.anga9.seller.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.data.repository.DashboardRepository
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository(application.applicationContext)
    private val profileRepository = ProfileRepository(application.applicationContext)

    private val _dashboardState = MutableStateFlow<UiState<SellerDashboardStats>>(UiState.Idle)
    val dashboardState: StateFlow<UiState<SellerDashboardStats>> = _dashboardState.asStateFlow()

    private val _sellerProfile = MutableStateFlow<SellerProfileResponse?>(null)
    val sellerProfile: StateFlow<SellerProfileResponse?> = _sellerProfile.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("today")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    init {
        // Pre-load cached profile immediately
        viewModelScope.launch {
            val cached = profileRepository.getSellerProfile()
            if (cached.isSuccess) {
                _sellerProfile.value = cached.getOrNull()
            }
        }
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadDashboard(period)
    }

    fun loadDashboard(period: String = _selectedPeriod.value) {
        viewModelScope.launch {
            val profileResult = profileRepository.getSellerProfile()
            if (profileResult.isSuccess) {
                _sellerProfile.value = profileResult.getOrNull()
            }

            repository.getDashboardStats(period = period).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_dashboardState.value !is UiState.Success) {
                            _dashboardState.value = UiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _dashboardState.value = UiState.Success(resource.data!!)
                    }
                    is Resource.Error -> {
                        // If we already have data on screen, do not wipe it on network error
                        if (_dashboardState.value !is UiState.Success) {
                            _dashboardState.value = UiState.Error(resource.message ?: "Failed to load dashboard")
                        }
                    }
                }
            }
        }
    }

    fun refresh() = loadDashboard(_selectedPeriod.value)
}
