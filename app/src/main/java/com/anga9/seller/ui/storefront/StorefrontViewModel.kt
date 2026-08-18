package com.anga9.seller.ui.storefront

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.data.repository.StorefrontRepository
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.UpdateStorefrontRequest
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StorefrontUiState {
    object Loading : StorefrontUiState()
    data class Success(val profile: SellerProfileResponse) : StorefrontUiState()
    data class Error(val message: String) : StorefrontUiState()
}

sealed class StorefrontSaveState {
    object Idle : StorefrontSaveState()
    object Saving : StorefrontSaveState()
    object Success : StorefrontSaveState()
    data class Error(val message: String) : StorefrontSaveState()
}

class StorefrontViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = ProfileRepository(application)
    private val storefrontRepository = StorefrontRepository(application)
    private val productRepository = ProductRepository(application)

    private val _uiState = MutableStateFlow<StorefrontUiState>(StorefrontUiState.Loading)
    val uiState: StateFlow<StorefrontUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<StorefrontSaveState>(StorefrontSaveState.Idle)
    val saveState: StateFlow<StorefrontSaveState> = _saveState.asStateFlow()

    init {
        loadStorefront()
    }

    private fun loadStorefront() {
        viewModelScope.launch {
            _uiState.value = StorefrontUiState.Loading
            val result = profileRepository.getSellerProfile()
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    _uiState.value = StorefrontUiState.Success(profile)
                } else {
                    _uiState.value = StorefrontUiState.Error("Profile not found")
                }
            } else {
                _uiState.value = StorefrontUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load storefront")
            }
        }
    }

    fun saveStorefront(
        bannerUri: Uri?,
        existingBannerUrl: String,
        aboutMd: String,
        published: Boolean,
        socialLinks: Map<String, String>
    ) {
        viewModelScope.launch {
            _saveState.value = StorefrontSaveState.Saving
            try {
                val sellerId = TokenManager.getUserId(getApplication()) ?: ""

                var finalBannerUrl = existingBannerUrl

                // If a new local image is selected, upload it first
                if (bannerUri != null) {
                    withContext(Dispatchers.IO) {
                        val uploadResult = productRepository.uploadProductImage(bannerUri, "storefront_banner_$sellerId")
                        if (uploadResult.isSuccess) {
                            finalBannerUrl = uploadResult.getOrThrow()
                        } else {
                            throw Exception(uploadResult.exceptionOrNull()?.message ?: "Failed to upload banner image")
                        }
                    }
                }

                val request = UpdateStorefrontRequest(
                    storefrontBannerUrl = finalBannerUrl.ifBlank { null },
                    aboutMd = aboutMd.ifBlank { null },
                    storefrontPublished = published,
                    socialLinks = socialLinks.ifEmpty { null }
                )

                val updateResult = storefrontRepository.updateStorefront(request)
                if (updateResult.isSuccess) {
                    _saveState.value = StorefrontSaveState.Success
                } else {
                    _saveState.value = StorefrontSaveState.Error(updateResult.exceptionOrNull()?.message ?: "Failed to save storefront")
                }
            } catch (e: Exception) {
                _saveState.value = StorefrontSaveState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = StorefrontSaveState.Idle
    }
}
