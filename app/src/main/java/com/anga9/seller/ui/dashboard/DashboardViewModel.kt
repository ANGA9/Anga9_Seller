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

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadDashboard(period)
    }

    fun loadDashboard(period: String = _selectedPeriod.value) {
        viewModelScope.launch {
            // Fetch profile in parallel
            val profileResult = profileRepository.getSellerProfile()
            if (profileResult.isSuccess) {
                _sellerProfile.value = profileResult.getOrNull()
            }

            repository.getDashboardStats(period = period).collectLatest { resource ->
                _dashboardState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data!!)
                    is Resource.Error -> UiState.Error(resource.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    fun refresh() = loadDashboard(_selectedPeriod.value)
}
