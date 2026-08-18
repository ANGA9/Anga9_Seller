package com.anga9.seller.ui.notifications

import android.text.format.DateUtils
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.SellerNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application.applicationContext)

    private val _notifications = MutableStateFlow<List<SellerNotification>>(emptyList())
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _groupedItems = MutableStateFlow<List<NotificationListItem>>(emptyList())
    val groupedItems: StateFlow<List<NotificationListItem>> = _groupedItems.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    private var isFetching = false
    private var currentFilter = FilterType.ALL

    enum class FilterType { ALL, UNREAD, ORDERS, DISPUTES, PAYOUTS }

    init {
        loadNotifications(isRefresh = true)
    }

    fun loadNotifications(isRefresh: Boolean = false) {
        if (isFetching) return
        if (isRefresh) {
            currentPage = 1
            hasMore = true
        }
        if (!hasMore) return

        isFetching = true
        if (isRefresh) _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                // We fetch without read filter to accumulate all, and filter client side
                val response = api.getNotifications(page = currentPage, limit = 20)
                if (response.isSuccessful) {
                    val newItems = response.body()?.data ?: emptyList<SellerNotification>()
                    val accumulated = if (isRefresh) newItems else _notifications.value + newItems
                    _notifications.value = accumulated
                    
                    hasMore = newItems.isNotEmpty() && newItems.size == 20
                    currentPage++
                    
                    applyCurrentFilter()
                    _uiState.value = UiState.Success
                } else {
                    _uiState.value = UiState.Error("Failed to load notifications")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            } finally {
                isFetching = false
            }
        }
    }

    fun setFilter(filterType: FilterType) {
        currentFilter = filterType
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filtered = when (currentFilter) {
            FilterType.ALL -> _notifications.value
            FilterType.UNREAD -> _notifications.value.filter { !it.read }
            FilterType.ORDERS -> _notifications.value.filter { it.type.startsWith("order_") }
            FilterType.DISPUTES -> _notifications.value.filter { it.type == "dispute" }
            FilterType.PAYOUTS -> _notifications.value.filter { it.type == "payout" }
        }
        
        _groupedItems.value = groupNotificationsByDate(filtered)
    }

    private fun groupNotificationsByDate(notifications: List<SellerNotification>): List<NotificationListItem> {
        if (notifications.isEmpty()) return emptyList()

        val groupedMap = mutableMapOf<String, MutableList<SellerNotification>>()
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance()
        
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        for (notif in notifications) {
            if (notif.sentAt.isEmpty()) continue
            
            try {
                val date = format.parse(notif.sentAt) ?: continue
                calendar.time = date
                val itemDay = calendar.get(Calendar.DAY_OF_YEAR)
                val itemYear = calendar.get(Calendar.YEAR)
                
                val groupName = when {
                    itemYear == currentYear && itemDay == today -> "Today"
                    itemYear == currentYear && itemDay == today - 1 -> "Yesterday"
                    itemYear == currentYear && today - itemDay <= 7 -> "This week"
                    else -> "Earlier"
                }
                
                groupedMap.getOrPut(groupName) { mutableListOf() }.add(notif)
            } catch (e: Exception) {
                groupedMap.getOrPut("Earlier") { mutableListOf() }.add(notif)
            }
        }

        val resultList = mutableListOf<NotificationListItem>()
        // Order keys manually to ensure correct display order
        listOf("Today", "Yesterday", "This week", "Earlier").forEach { key ->
            groupedMap[key]?.let { list ->
                if (list.isNotEmpty()) {
                    resultList.add(NotificationListItem.Header(key))
                    resultList.addAll(list.map { NotificationListItem.Row(it) })
                }
            }
        }
        return resultList
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                // Optimistic local update
                val updatedList = _notifications.value.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
                _notifications.value = updatedList
                applyCurrentFilter()

                // API call
                api.markNotificationRead(notificationId)
            } catch (e: Exception) {
                // Revert or ignore on failure for v1
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                // Optimistic local update
                val updatedList = _notifications.value.map { it.copy(read = true) }
                _notifications.value = updatedList
                applyCurrentFilter()

                // API call
                api.markAllNotificationsRead()
            } catch (e: Exception) {
                // Ignore failure for v1
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
