package com.anga9.seller.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.data.repository.InventoryRepository
import com.anga9.seller.network.model.InventoryResponse
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class InventoryRow(
    val product: SellerProductResponse,
    val stock: InventoryResponse?
)

data class InventoryStatSummary(
    val totalProducts: Int = 0,
    val lowStock: Int = 0,
    val outOfStock: Int = 0
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val productRepository = ProductRepository(application.applicationContext)
    private val inventoryRepository = InventoryRepository(application.applicationContext)

    private val _allRows = MutableStateFlow<List<InventoryRow>>(emptyList())
    
    private val _uiState = MutableStateFlow<UiState<List<InventoryRow>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<InventoryRow>>> = _uiState.asStateFlow()

    private val _statSummary = MutableStateFlow(InventoryStatSummary())
    val statSummary: StateFlow<InventoryStatSummary> = _statSummary.asStateFlow()

    private val _updateStockState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val updateStockState: StateFlow<UiState<Boolean>> = _updateStockState.asStateFlow()

    fun fetchInventory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            // 1. Fetch Products
            productRepository.getMyProducts("active,pending_review,draft,archived,rejected").collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val products = resource.data ?: emptyList()
                        fetchStockForProducts(products)
                    }
                    is Resource.Error -> {
                        _uiState.value = UiState.Error(resource.message ?: "Failed to load products")
                    }
                    is Resource.Loading -> {
                        _uiState.value = UiState.Loading
                    }
                }
            }
        }
    }

    private suspend fun fetchStockForProducts(products: List<SellerProductResponse>) {
        // 2. Fetch stock for each product concurrently (matching web seller Promise.all)
        val deferredResults = products.map { product ->
            viewModelScope.async {
                var stockResponse: InventoryResponse? = null
                inventoryRepository.getStock(product.id).collectLatest { stockResource ->
                    if (stockResource is Resource.Success) {
                        stockResponse = stockResource.data
                    }
                }
                InventoryRow(product = product, stock = stockResponse)
            }
        }

        val results = deferredResults.awaitAll()
        _allRows.value = results
        
        calculateStats(results)
        _uiState.value = UiState.Success(results)
    }

    private fun calculateStats(rows: List<InventoryRow>) {
        val lowStockCount = rows.count { 
            val qty = it.stock?.effectiveQuantity ?: it.stock?.quantity ?: it.stock?.stock ?: 0
            val threshold = it.stock?.lowStockThreshold ?: 10
            qty in 1..threshold
        }
        val outOfStockCount = rows.count { 
            val qty = it.stock?.effectiveQuantity ?: it.stock?.quantity ?: it.stock?.stock ?: 0
            qty <= 0 
        }

        _statSummary.value = InventoryStatSummary(
            totalProducts = rows.size,
            lowStock = lowStockCount,
            outOfStock = outOfStockCount
        )
    }

    fun updateStock(productId: String, quantity: Int, threshold: Int) {
        viewModelScope.launch {
            _updateStockState.value = UiState.Loading
            val result = inventoryRepository.updateStock(productId, quantity, threshold, "Manual update via app")
            
            if (result.isSuccess) {
                // Optimistic UI Update
                val currentRows = _allRows.value.toMutableList()
                val index = currentRows.indexOfFirst { it.product.id == productId }
                if (index != -1) {
                    val oldRow = currentRows[index]
                    val updatedStock = oldRow.stock?.copy(
                        quantity = quantity,
                        stock = quantity,
                        lowStockThreshold = threshold
                    ) ?: InventoryResponse(
                        productId = productId,
                        quantity = quantity,
                        stock = quantity,
                        lowStockThreshold = threshold,
                        available = quantity
                    )
                    
                    currentRows[index] = oldRow.copy(stock = updatedStock)
                    _allRows.value = currentRows
                    calculateStats(currentRows)
                    _uiState.value = UiState.Success(currentRows) // re-emit
                }
                
                _updateStockState.value = UiState.Success(true)
            } else {
                _updateStockState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update stock")
            }
        }
    }
}
