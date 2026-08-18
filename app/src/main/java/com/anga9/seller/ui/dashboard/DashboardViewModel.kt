package com.anga9.seller.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.data.repository.DashboardRepository
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository(application.applicationContext)
    private val profileRepository = com.anga9.seller.data.repository.ProfileRepository(application.applicationContext)

    private val _dashboardState = MutableStateFlow<UiState<SellerDashboardStats>>(UiState.Idle)
    val dashboardState: StateFlow<UiState<SellerDashboardStats>> = _dashboardState.asStateFlow()

    private val _sellerProfile = MutableStateFlow<com.anga9.seller.network.model.SellerProfileResponse?>(null)
    val sellerProfile: StateFlow<com.anga9.seller.network.model.SellerProfileResponse?> = _sellerProfile.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            // Fetch profile
            val profileResult = profileRepository.getSellerProfile()
            if (profileResult.isSuccess) {
                _sellerProfile.value = profileResult.getOrNull()
            }

            repository.getDashboardStats().collectLatest { resource ->
                _dashboardState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data!!)
                    is Resource.Error -> UiState.Error(resource.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    fun refresh() = loadDashboard()
}
