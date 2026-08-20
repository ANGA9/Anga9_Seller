package com.anga9.seller.ui.privacy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application.applicationContext)

    private val _deleteState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Boolean>> = _deleteState.asStateFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            val result = repository.deleteAccount()
            _deleteState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to delete account") }
            )
        }
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Idle
    }
}
