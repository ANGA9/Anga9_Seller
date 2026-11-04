package com.anga9.seller.ui.notifications

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
                val response = api.getNotifications(page = currentPage, limit = 20)
                if (response.isSuccessful) {
                    val rawItems = response.body()?.data ?: emptyList()
                    // Filter: strictly keep only seller-intended notifications
                    val sellerItems = rawItems.filter { isSellerNotification(it) }
                    val accumulated = if (isRefresh) sellerItems else _notifications.value + sellerItems
                    _notifications.value = accumulated
                    
                    hasMore = rawItems.isNotEmpty() && rawItems.size == 20
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

    /**
     * Determines whether a notification is intended for the seller role,
     * filtering out customer/buyer personal notifications (such as personal order checkouts,
     * payment failure on buyer side, personal tracking updates, etc.) when the same phone number
     * is registered as both a customer and a seller.
     */
    private fun isSellerNotification(n: SellerNotification): Boolean {
        val type = n.type.lowercase().replace("_", ".")
        val title = n.title.lowercase()
        val body = n.body.lowercase()

        // 1. Explicit customer-only event types
        val customerEventTypes = setOf(
            "payment.failed",
            "payment.success",
            "order.confirmed",
            "order.shipped",
            "order.delivered",
            "price.dropped",
            "inventory.restocked"
        )
        if (type in customerEventTypes) {
            return false
        }

        // 2. Buyer phrasing checks ("Your payment for order...", "Your order ANGA-... has been cancelled", "Payment received! Your order...")
        if (body.startsWith("your payment") ||
            body.startsWith("your order") ||
            body.startsWith("payment received! your order") ||
            body.startsWith("your return request") ||
            title.startsWith("payment failed for") ||
            (title.contains("cancelled") && body.contains("a refund will be processed"))
        ) {
            return false
        }

        return true
    }

    fun setFilter(filterType: FilterType) {
        currentFilter = filterType
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filtered = when (currentFilter) {
            FilterType.ALL -> _notifications.value
            FilterType.UNREAD -> _notifications.value.filter { !it.read }
            FilterType.ORDERS -> _notifications.value.filter {
                val t = it.type.lowercase().replace("_", ".")
                t.contains("order")
            }
            FilterType.DISPUTES -> _notifications.value.filter {
                val t = it.type.lowercase().replace("_", ".")
                t.contains("dispute") || t.contains("return")
            }
            FilterType.PAYOUTS -> _notifications.value.filter {
                val t = it.type.lowercase().replace("_", ".")
                t.contains("payout") || t.contains("payment") || t.contains("wallet")
            }
        }
        
        _groupedItems.value = groupNotificationsByDate(filtered)
    }

    private fun parseDate(isoString: String): Date? {
        if (isoString.isBlank()) return null
        val cleaned = if (isoString.contains(".")) isoString.substringBefore(".") + "Z" else isoString
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = sdf.parse(isoString) ?: sdf.parse(cleaned)
                if (d != null) return d
            } catch (_: Exception) {}
        }
        return null
    }

    private fun groupNotificationsByDate(notifications: List<SellerNotification>): List<NotificationListItem> {
        if (notifications.isEmpty()) return emptyList()

        val groupedMap = mutableMapOf<String, MutableList<SellerNotification>>()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        for (notif in notifications) {
            val date = parseDate(notif.sentAt)
            if (date == null) {
                groupedMap.getOrPut("Earlier") { mutableListOf() }.add(notif)
                continue
            }
            
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
        }

        val resultList = mutableListOf<NotificationListItem>()
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
            } catch (_: Exception) {
                // Ignore on failure
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
            } catch (_: Exception) {
                // Ignore failure
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
