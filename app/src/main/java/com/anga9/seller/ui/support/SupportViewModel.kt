package com.anga9.seller.ui.support

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.support.CreateTicketRequest
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.SupabaseClient
import com.anga9.seller.utils.TokenManager
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class SupportUiState {
    object Idle : SupportUiState()
    object Loading : SupportUiState()
    data class Success(val openTickets: Int) : SupportUiState()
    data class Error(val message: String) : SupportUiState()
}

sealed class CreateTicketState {
    object Idle : CreateTicketState()
    object Loading : CreateTicketState()
    object Success : CreateTicketState()
    data class Error(val message: String) : CreateTicketState()
}

class SupportViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application.applicationContext)
    private val contentResolver = application.contentResolver
    private val context = application.applicationContext

    private val _homeState = MutableStateFlow<SupportUiState>(SupportUiState.Idle)
    val homeState: StateFlow<SupportUiState> = _homeState.asStateFlow()

    private val _createState = MutableStateFlow<CreateTicketState>(CreateTicketState.Idle)
    val createState: StateFlow<CreateTicketState> = _createState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = SupportUiState.Loading
            try {
                val response = api.getSupportTickets(page = 1, limit = 50)
                if (response.isSuccessful) {
                    val tickets = response.body()?.tickets ?: emptyList()
                    val openCount = tickets.count { it.status == "open" || it.status == "in_progress" || it.status == "pending_user" }
                    _homeState.value = SupportUiState.Success(openCount)
                } else {
                    _homeState.value = SupportUiState.Error("Failed to load tickets: ${response.code()}")
                }
            } catch (e: Exception) {
                _homeState.value = SupportUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createTicket(
        subject: String,
        category: String,
        description: String,
        priority: String,
        attachments: List<Uri>
    ) {
        viewModelScope.launch {
            _createState.value = CreateTicketState.Loading
            try {
                val userToken = TokenManager.getToken(context)
                val refreshToken = TokenManager.getRefreshToken(context)
                if (userToken != null && SupabaseClient.auth.currentSessionOrNull() == null) {
                    try {
                        SupabaseClient.auth.importAuthToken(userToken, refreshToken ?: "", autoRefresh = true)
                    } catch (e: Exception) {
                        // Continue
                    }
                }

                val uploadedUrls = mutableListOf<String>()

                // 1. Upload attachments to Supabase Storage
                if (attachments.isNotEmpty()) {
                    val bucket = SupabaseClient.storage.from("public-assets")
                    for (uri in attachments) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val ext = getFileExtension(uri) ?: "bin"
                            val filename = "tickets/${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}.$ext"
                            
                            bucket.upload(filename, bytes) {
                                upsert = true
                            }
                            
                            val publicUrl = bucket.publicUrl(filename)
                            uploadedUrls.add(publicUrl)
                        }
                    }
                }

                // 2. Create the ticket via backend API
                val request = CreateTicketRequest(
                    subject = subject.trim(),
                    category = category.trim(),
                    initialMessage = description.trim(),
                    priority = priority.ifEmpty { "medium" },
                    attachments = if (uploadedUrls.isNotEmpty()) uploadedUrls else null
                )

                val response = api.createSupportTicket(request)
                if (response.isSuccessful) {
                    _createState.value = CreateTicketState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    _createState.value = CreateTicketState.Error("Failed to create ticket [${response.code()}]: $errorBody")
                }
            } catch (e: Exception) {
                _createState.value = CreateTicketState.Error(e.message ?: "Failed to create ticket")
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        var ext: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        val name = cursor.getString(displayNameIndex)
                        val lastDot = name.lastIndexOf('.')
                        if (lastDot > 0) {
                            ext = name.substring(lastDot + 1)
                        }
                    }
                }
            }
        }
        if (ext == null) {
            val path = uri.path
            if (path != null) {
                val lastDot = path.lastIndexOf('.')
                if (lastDot > 0) {
                    ext = path.substring(lastDot + 1)
                }
            }
        }
        return ext
    }
}
