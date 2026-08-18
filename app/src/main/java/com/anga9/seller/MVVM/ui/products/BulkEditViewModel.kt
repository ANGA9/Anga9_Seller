package com.anga9.seller.MVVM.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.network.model.BulkPriceUpdateRequest
import com.anga9.seller.network.model.BulkStockUpdateRequest
import com.anga9.seller.network.model.PriceUpdateItem
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.network.model.StockUpdateItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PriceEdit(
    var originalBase: Double?,
    var originalSale: Double?,
    var currentBase: Double?,
    var currentSale: Double?,
    var isDirty: Boolean = false,
    var isValid: Boolean = true
)

data class StockEdit(
    var originalStock: Int,
    var currentStock: Int,
    var isDirty: Boolean = false
)

sealed class BulkEditState {
    object Idle : BulkEditState()
    object Loading : BulkEditState()
    data class Success(val products: List<SellerProductResponse>) : BulkEditState()
    data class Error(val message: String) : BulkEditState()
    object Saving : BulkEditState()
    data class SaveSuccess(val message: String) : BulkEditState()
    data class SaveError(val message: String) : BulkEditState()
}

class BulkEditViewModel : ViewModel() {
    var repository: ProductRepository? = null

    private val _uiState = MutableStateFlow<BulkEditState>(BulkEditState.Idle)
    val uiState: StateFlow<BulkEditState> = _uiState

    var allProducts = listOf<SellerProductResponse>()
    
    val priceEdits = mutableMapOf<String, PriceEdit>()
    val stockEdits = mutableMapOf<String, StockEdit>()
    
    private val _editedCount = MutableStateFlow(0)
    val editedCount: StateFlow<Int> = _editedCount

    fun loadProducts() {
        if (_uiState.value is BulkEditState.Loading) return
        _uiState.value = BulkEditState.Loading
        viewModelScope.launch {
            repository?.getMyProducts("all")?.collect { result ->
                when (result) {
                    is com.anga9.seller.utils.Resource.Loading -> {}
                    is com.anga9.seller.utils.Resource.Success -> {
                        allProducts = result.data ?: emptyList()
                        
                        priceEdits.clear()
                        stockEdits.clear()
                        allProducts.forEach { p ->
                            priceEdits[p.id] = PriceEdit(p.basePrice, p.salePrice, p.basePrice, p.salePrice)
                            stockEdits[p.id] = StockEdit(p.stock, p.stock)
                        }
                        
                        recalculateEditedCount()
                        _uiState.value = BulkEditState.Success(allProducts)
                    }
                    is com.anga9.seller.utils.Resource.Error -> {
                        _uiState.value = BulkEditState.Error(result.message ?: "Failed to load products")
                    }
                }
            }
        }
    }

    fun updatePrice(id: String, basePrice: Double?, salePrice: Double?) {
        val edit = priceEdits[id] ?: return
        edit.currentBase = basePrice
        edit.currentSale = salePrice
        
        edit.isValid = if (basePrice != null && salePrice != null) {
            salePrice < basePrice
        } else {
            true
        }

        edit.isDirty = (edit.originalBase != edit.currentBase || edit.originalSale != edit.currentSale)
        recalculateEditedCount()
    }

    fun updateStock(id: String, quantity: Int) {
        val edit = stockEdits[id] ?: return
        edit.currentStock = quantity
        edit.isDirty = (edit.originalStock != edit.currentStock)
        recalculateEditedCount()
    }

    private fun recalculateEditedCount() {
        var count = 0
        val handled = mutableSetOf<String>()
        
        priceEdits.forEach { (id, edit) ->
            if (edit.isDirty) {
                handled.add(id)
                count++
            }
        }
        
        stockEdits.forEach { (id, edit) ->
            if (edit.isDirty && !handled.contains(id)) {
                count++
            }
        }
        
        _editedCount.value = count
    }

    fun saveChanges() {
        if (_uiState.value is BulkEditState.Saving) return
        
        val priceChanged = priceEdits.filter { it.value.isDirty }
        val stockChanged = stockEdits.filter { it.value.isDirty }
        
        if (priceChanged.isEmpty() && stockChanged.isEmpty()) return
        
        val invalidPrices = priceChanged.filter { !it.value.isValid }
        if (invalidPrices.isNotEmpty()) {
            _uiState.value = BulkEditState.SaveError("Please fix validation errors before saving.")
            return
        }

        _uiState.value = BulkEditState.Saving
        
        viewModelScope.launch {
            var msg = ""
            var success = true
            
            try {
                if (priceChanged.isNotEmpty()) {
                    val req = BulkPriceUpdateRequest(
                        updates = priceChanged.map { (id, edit) ->
                            PriceUpdateItem(
                                productId = id,
                                basePrice = edit.currentBase ?: 0.0,
                                salePrice = edit.currentSale
                            )
                        }
                    )
                    val res = repository?.apiService?.bulkUpdatePrices(req)
                    if (res?.isSuccessful == true) {
                        msg += "Updated ${priceChanged.size} prices. "
                        priceChanged.forEach { (_, edit) -> 
                            edit.originalBase = edit.currentBase
                            edit.originalSale = edit.currentSale
                            edit.isDirty = false
                        }
                    } else {
                        success = false
                    }
                }
                
                if (stockChanged.isNotEmpty()) {
                    val req = BulkStockUpdateRequest(
                        items = stockChanged.map { (id, edit) ->
                            StockUpdateItem(productId = id, quantity = edit.currentStock)
                        }
                    )
                    val res = repository?.apiService?.bulkUpdateStock(req)
                    if (res?.isSuccessful == true) {
                        msg += "Updated ${stockChanged.size} stock items. "
                        stockChanged.forEach { (_, edit) -> 
                            edit.originalStock = edit.currentStock
                            edit.isDirty = false
                        }
                    } else {
                        success = false
                    }
                }
                
                if (success) {
                    recalculateEditedCount()
                    _uiState.value = BulkEditState.SaveSuccess(msg.trim())
                    // Reload to refresh the list perfectly
                    loadProducts()
                } else {
                    _uiState.value = BulkEditState.SaveError("Some updates failed. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = BulkEditState.SaveError(e.message ?: "Network error")
            }
        }
    }
}
