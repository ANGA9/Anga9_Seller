package com.anga9.seller.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.AuthRepository
import com.anga9.seller.network.model.UserProfileResponse
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch

/**
 * AuthViewModel for Seller App.
 *
 * Uses AndroidViewModel to access application context for TokenManager.
 *
 * Auth flow:
 * 1. Seller enters phone → Supabase SDK sends OTP
 * 2. Seller enters OTP → Supabase verifies → returns access token
 * 3. Activity calls verifyWithBackend(supabaseToken)
 * 4. ViewModel calls repository, gets user profile + kyc_status
 * 5. Navigate based on kyc_status:
 *    - null / "pending" → KYC registration flow
 *    - "approved" → Dashboard
 *    - "rejected" → KYC status screen
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    // Token verification state
    private val _verifyState = MutableLiveData<UiState<UserProfileResponse>>()
    val verifyState: LiveData<UiState<UserProfileResponse>> = _verifyState

    // Session check state
    private val _sessionState = MutableLiveData<UiState<UserProfileResponse>>()
    val sessionState: LiveData<UiState<UserProfileResponse>> = _sessionState

    val currentUserId get() = repository.getSavedUserId()
    /** Alias for backward compatibility with SellerRegistrationActivity */
    val currentUid get() = repository.getSavedUserId()
    val currentPhone get() = ""

    /**
     * Call this after Supabase OTP verification succeeds.
     * Verifies token with backend and gets user profile.
     */
    fun verifyWithBackend(supabaseToken: String) {
        _verifyState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.verifyTokenWithBackend(supabaseToken)
            _verifyState.value = result.fold(
                onSuccess = { user -> UiState.Success(user) },
                onFailure = { e -> UiState.Error(e.message ?: "Verification failed") }
            )
        }
    }

    /**
     * Check if user is already logged in with a valid token.
     * Call on SplashActivity.
     */
    fun checkExistingSession() {
        if (!repository.isLoggedIn()) {
            _sessionState.value = UiState.Error("Not logged in")
            return
        }
        _sessionState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.getCurrentUser()
            _sessionState.value = result.fold(
                onSuccess = { user -> UiState.Success(user) },
                onFailure = {
                    repository.signOut()
                    UiState.Error("Session expired")
                }
            )
        }
    }


    // ── KYC Registration methods (backward compat with SellerRegistrationActivity) ──

    // Mutable URL storage for KYC docs
    var newGstCertUrl: String = ""
    var newSecondDocUrl: String = ""
    var newSecondDocType: String = ""

    private val _ownerNameState = androidx.lifecycle.MutableLiveData<UiState<Unit>>()
    val ownerNameState: androidx.lifecycle.LiveData<UiState<Unit>> = _ownerNameState

    private val _businessInfoState = androidx.lifecycle.MutableLiveData<UiState<Unit>>()
    val businessInfoState: androidx.lifecycle.LiveData<UiState<Unit>> = _businessInfoState

    private val _garmentCatState = androidx.lifecycle.MutableLiveData<UiState<Unit>>()
    val garmentCatState: androidx.lifecycle.LiveData<UiState<Unit>> = _garmentCatState

    private val _locationState = androidx.lifecycle.MutableLiveData<UiState<Unit>>()
    val locationState: androidx.lifecycle.LiveData<UiState<Unit>> = _locationState

    private val _newKycSubmitState = androidx.lifecycle.MutableLiveData<UiState<Unit>>()
    val newKycSubmitState: androidx.lifecycle.LiveData<UiState<Unit>> = _newKycSubmitState

    private val _gstUploadState = androidx.lifecycle.MutableLiveData<UiState<String>>()
    val gstUploadState: androidx.lifecycle.LiveData<UiState<String>> = _gstUploadState

    private val _shopUploadState = androidx.lifecycle.MutableLiveData<UiState<String>>()
    val shopUploadState: androidx.lifecycle.LiveData<UiState<String>> = _shopUploadState

    fun saveOwnerName(uid: String, ownerName: String, phone: String) {
        viewModelScope.launch {
            _ownerNameState.value = UiState.Loading
            // Save to SharedPreferences for now (backend KYC submit happens at end)
            getApplication<android.app.Application>().getSharedPreferences("kyc_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("owner_name", ownerName).putString("phone", phone).apply()
            _ownerNameState.value = UiState.Success(Unit)
        }
    }

    fun saveBusinessNameAndType(uid: String, businessName: String, businessType: String) {
        viewModelScope.launch {
            _businessInfoState.value = UiState.Loading
            getApplication<android.app.Application>().getSharedPreferences("kyc_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("business_name", businessName).putString("business_type", businessType).apply()
            _businessInfoState.value = UiState.Success(Unit)
        }
    }

    fun saveGarmentCategories(uid: String, categories: List<String>) {
        viewModelScope.launch {
            _garmentCatState.value = UiState.Loading
            getApplication<android.app.Application>().getSharedPreferences("kyc_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("garment_categories", categories.joinToString(",")).apply()
            _garmentCatState.value = UiState.Success(Unit)
        }
    }

    fun saveLocation(uid: String, pinCode: String, city: String, state: String) {
        viewModelScope.launch {
            _locationState.value = UiState.Loading
            getApplication<android.app.Application>().getSharedPreferences("kyc_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("pincode", pinCode).putString("city", city).putString("state", state).apply()
            _locationState.value = UiState.Success(Unit)
        }
    }

    fun uploadGstCert(uid: String, uri: android.net.Uri) {
        viewModelScope.launch {
            _gstUploadState.value = UiState.Loading
            try {
                val repo = com.anga9.seller.MVVM.data.repository.ProductRepository(getApplication())
                val result = repo.uploadProductImage(uri, "kyc_gst_$uid")
                if (result.isSuccess) {
                    newGstCertUrl = result.getOrThrow()
                    _gstUploadState.value = UiState.Success(newGstCertUrl)
                } else {
                    _gstUploadState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Upload failed")
                }
            } catch (e: Exception) {
                _gstUploadState.value = UiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun uploadSecondDoc(uid: String, uri: android.net.Uri, docType: String) {
        newSecondDocType = docType
        viewModelScope.launch {
            _shopUploadState.value = UiState.Loading
            try {
                val repo = com.anga9.seller.MVVM.data.repository.ProductRepository(getApplication())
                val result = repo.uploadProductImage(uri, "kyc_doc_$uid")
                if (result.isSuccess) {
                    newSecondDocUrl = result.getOrThrow()
                    _shopUploadState.value = UiState.Success(newSecondDocUrl)
                } else {
                    _shopUploadState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Upload failed")
                }
            } catch (e: Exception) {
                _shopUploadState.value = UiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun submitNewKyc(uid: String) {
        viewModelScope.launch {
            _newKycSubmitState.value = UiState.Loading
            try {
                val prefs = getApplication<android.app.Application>().getSharedPreferences("kyc_prefs", android.content.Context.MODE_PRIVATE)
                val profileRepo = com.anga9.seller.data.repository.ProfileRepository(getApplication())
                val request = com.anga9.seller.network.model.SubmitKycRequest(
                    gstCertUrl = newGstCertUrl.ifEmpty { null },
                    panCardUrl = newSecondDocUrl.ifEmpty { null }
                )
                val result = profileRepo.submitKyc(request)
                _newKycSubmitState.value = result.fold(
                    onSuccess = { UiState.Success(Unit) },
                    onFailure = { UiState.Error(it.message ?: "KYC submission failed") }
                )
            } catch (e: Exception) {
                _newKycSubmitState.value = UiState.Error(e.message ?: "KYC submission failed")
            }
        }
    }
    fun signOut() {
        repository.signOut()
    }
    private val _fullRegistrationState = MutableLiveData<UiState<Unit>>()
    val fullRegistrationState: LiveData<UiState<Unit>> = _fullRegistrationState

    fun submitFullRegistration(request: com.anga9.seller.network.model.UpdateSellerProfileRequest) {
        viewModelScope.launch {
            _fullRegistrationState.value = UiState.Loading
            try {
                val profileRepo = com.anga9.seller.data.repository.ProfileRepository(getApplication())
                
                // 1. Create Profile
                var createResult = profileRepo.createSellerProfile(request)
                if (createResult.isFailure) {
                    val errorMsg = createResult.exceptionOrNull()?.message ?: ""
                    if (errorMsg.contains("409")) {
                        // Profile already exists, let's update it instead
                        createResult = profileRepo.updateSellerProfile(request)
                        if (createResult.isFailure) {
                            _fullRegistrationState.value = UiState.Error(createResult.exceptionOrNull()?.message ?: "Failed to update profile")
                            return@launch
                        }
                    } else {
                        _fullRegistrationState.value = UiState.Error(errorMsg.ifEmpty { "Failed to create profile" })
                        return@launch
                    }
                }

                // 2. Submit KYC
                val submitResult = profileRepo.submitKyc(com.anga9.seller.network.model.SubmitKycRequest())
                if (submitResult.isSuccess) {
                    _fullRegistrationState.value = UiState.Success(Unit)
                } else {
                    _fullRegistrationState.value = UiState.Error(submitResult.exceptionOrNull()?.message ?: "Failed to submit KYC")
                }
            } catch (e: Exception) {
                _fullRegistrationState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }
}
