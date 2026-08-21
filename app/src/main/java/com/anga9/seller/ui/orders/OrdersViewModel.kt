package com.anga9.seller.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.SellerOrder
import com.anga9.seller.data.model.SellerOrderItem
import com.anga9.seller.data.repository.OrderRepository
import com.anga9.seller.network.model.SellerOrderResponse
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrderRepository(application.applicationContext)

    // ── New backend states ───────────────────────────────────────────
    private val _ordersState = MutableLiveData<Resource<List<SellerOrderResponse>>>()
    val ordersState: LiveData<Resource<List<SellerOrderResponse>>> = _ordersState

    private val _orderDetailState = MutableLiveData<Resource<SellerOrderResponse>>()
    val orderDetailState: LiveData<Resource<SellerOrderResponse>> = _orderDetailState

    private val _updateStatusState = MutableLiveData<Resource<SellerOrderResponse>>()
    val updateStatusState: LiveData<Resource<SellerOrderResponse>> = _updateStatusState

    private val _allOrders = MutableLiveData<List<SellerOrderResponse>>()
    val allOrders: LiveData<List<SellerOrderResponse>> = _allOrders
    
    private val _filteredOrders = MutableLiveData<Resource<List<SellerOrderResponse>>>()
    val filteredOrders: LiveData<Resource<List<SellerOrderResponse>>> = _filteredOrders

    private var currentSearchQuery = ""
    private var currentStatusFilter = "all"

    // ── MyOrdersActivity compatibility states (LiveData<Resource<List<SellerOrder>>>) ──
    // MyOrdersActivity uses old SellerOrder model - we convert from SellerOrderResponse
    private val _orders = MutableLiveData<Resource<List<SellerOrder>>>()
    val orders: LiveData<Resource<List<SellerOrder>>> = _orders

    private val _updateStatus = MutableLiveData<Resource<SellerOrder>>()
    val updateStatus: LiveData<Resource<SellerOrder>> = _updateStatus

    private val _rejectStatus = MutableLiveData<Resource<SellerOrder>>()
    val rejectStatus: LiveData<Resource<SellerOrder>> = _rejectStatus

    // ── Methods ───────────────────────────────────────────────────────

    fun loadOrders() {
        if (_allOrders.value.isNullOrEmpty()) {
            _filteredOrders.value = Resource.Loading()
        }
        viewModelScope.launch {
            repository.getSellerOrders("all").collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_allOrders.value.isNullOrEmpty()) {
                            _filteredOrders.postValue(Resource.Loading())
                        }
                    }
                    is Resource.Success -> {
                        val orders = resource.data ?: emptyList()
                        _allOrders.postValue(orders)
                        applyFilters(orders)
                    }
                    is Resource.Error -> {
                        if (_allOrders.value.isNullOrEmpty()) {
                            _filteredOrders.postValue(Resource.Error(resource.message ?: "Failed"))
                        }
                    }
                }
            }
        }
    }

    fun setStatusFilter(status: String) {
        currentStatusFilter = status.lowercase()
        applyFilters()
    }

    fun searchOrders(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    private fun applyFilters(sourceList: List<SellerOrderResponse>? = null) {
        val all = sourceList ?: _allOrders.value ?: run {
            _filteredOrders.postValue(Resource.Success(emptyList()))
            return
        }
        var filtered = all.toList()

        if (currentStatusFilter != "all") {
            filtered = filtered.filter { it.getEffectiveStatus().equals(currentStatusFilter, ignoreCase = true) }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { order ->
                val idStr = order.orderNumber ?: order.id
                idStr.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        _filteredOrders.postValue(Resource.Success(filtered))
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            repository.getOrderDetail(orderId).collectLatest { resource ->
                _orderDetailState.postValue(resource)
            }
        }
    }

    fun updateOrderStatus(
        orderId: String,
        status: String,
        trackingNumber: String? = null,
        courierName: String? = null,
        note: String? = null
    ) {
        _updateStatusState.value = Resource.Loading()
        _updateStatus.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, status, trackingNumber, courierName, note)
            _updateStatusState.postValue(
                result.fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Failed to update status") }
                )
            )
            _updateStatus.postValue(
                result.fold(
                    onSuccess = { Resource.Success(it.toSellerOrder()) },
                    onFailure = { Resource.Error(it.message ?: "Failed to update status") }
                )
            )
        }
    }

    /** Reject an order with a reason */
    fun rejectOrder(orderId: String, reason: String) {
        _rejectStatus.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateOrderStatus(
                orderId = orderId,
                status = "CANCELLED",
                note = reason
            )
            _rejectStatus.postValue(
                result.fold(
                    onSuccess = { Resource.Success(it.toSellerOrder()) },
                    onFailure = { Resource.Error(it.message ?: "Failed to reject order") }
                )
            )
        }
    }

    // -- OrderDetailActivity compatibility ----------------------------------
    // selectedOrder: LiveData<Resource<SellerOrder>> for OrderDetailActivity
    private val _selectedOrder = MutableLiveData<Resource<SellerOrder>>()
    val selectedOrder: LiveData<Resource<SellerOrder>> = _selectedOrder

    /** Load a single order by ID - populates selectedOrder for OrderDetailActivity */
    fun loadOrderById(orderId: String) {
        _selectedOrder.value = Resource.Loading()
        viewModelScope.launch {
            repository.getOrderDetail(orderId).collectLatest { resource ->
                _orderDetailState.postValue(resource)
                _selectedOrder.postValue(when (resource) {
                    is Resource.Loading -> Resource.Loading()
                    is Resource.Success -> Resource.Success(resource.data!!.toSellerOrder())
                    is Resource.Error -> Resource.Error(resource.message ?: "Failed to load order")
                })
            }
        }
    }

    // â”€â”€ Conversion helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private fun SellerOrderResponse.toSellerOrder(): SellerOrder = SellerOrder(
        orderId = id,
        customerId = customerId,
        customerName = customerName ?: "",
        customerPhone = customerPhone ?: "",
        sellerId = sellerId,
        items = items.map { item ->
            SellerOrderItem(
                productId = item.productId,
                productName = item.productName,
                productImage = item.productImage ?: "",
                unitPrice = item.price,
                quantity = item.quantity,
                subtotal = item.price * item.quantity
            )
        },
        totalAmount = totalAmount,
        deliveryCharges = deliveryCharges,
        gstAmount = gstAmount,
        paymentMethod = paymentMethod ?: "COD",
        deliveryAddress = deliveryAddress?.let {
            "${it.addressLine1 ?: ""}, ${it.city ?: ""}, ${it.state ?: ""} - ${it.pincode ?: ""}"
        } ?: "",
        trackingNumber = trackingNumber ?: "",
        courierName = courierName ?: "",
        orderStatus = status,
        createdAt = 0L
    )
}
