package com.anga9.seller.MVVM.ui.products

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.network.model.CreateProductRequest
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.network.model.UpdateProductRequest
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository(application.applicationContext)

    // Products list
    private val _productsState = MutableStateFlow<UiState<List<SellerProductResponse>>>(UiState.Idle)
    val productsState: StateFlow<UiState<List<SellerProductResponse>>> = _productsState.asStateFlow()

    // Add/Update/Delete
    private val _createState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val createState: StateFlow<UiState<SellerProductResponse>> = _createState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val updateState: StateFlow<UiState<SellerProductResponse>> = _updateState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Boolean>> = _deleteState.asStateFlow()
    /** Alias for deleteState — used by MyProductsActivity */
    val deleteProductState: StateFlow<UiState<Boolean>> get() = _deleteState.asStateFlow()
    /** Stock update state (for MyProductsActivity stock dialog) */
    private val _updateStockState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val updateStockState: StateFlow<UiState<Boolean>> = _updateStockState.asStateFlow()

    // AddEditProductActivity compatibility states
    /** Categories list (stub - returns empty, backend categories come from product-service) */
    private val _categoriesState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val categoriesState: StateFlow<UiState<List<String>>> = _categoriesState.asStateFlow()

    private val _allCategoriesState = MutableStateFlow<UiState<List<com.anga9.seller.network.model.CategoryResponse>>>(UiState.Idle)
    val allCategoriesState: StateFlow<UiState<List<com.anga9.seller.network.model.CategoryResponse>>> = _allCategoriesState.asStateFlow()

    /** Subcategories for a given category (stub - returns empty list) */
    private val _subcategoriesState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val subcategoriesState: StateFlow<UiState<List<String>>> = _subcategoriesState.asStateFlow()

    /** Product types for a given subcategory (stub - returns empty list) */
    private val _productTypesState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val productTypesState: StateFlow<UiState<List<String>>> = _productTypesState.asStateFlow()

    /** Add product state */
    private val _addProductState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val addProductState: StateFlow<UiState<SellerProductResponse>> = _addProductState.asStateFlow()

    /** Update product state */
    private val _updateProductState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val updateProductState: StateFlow<UiState<SellerProductResponse>> = _updateProductState.asStateFlow()
    /** Single product detail state (for edit mode) */
    private val _productDetailState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val productDetailState: StateFlow<UiState<SellerProductResponse>> = _productDetailState.asStateFlow()

    // Methods

    fun loadProducts(statusFilter: String = "all") {
        viewModelScope.launch {
            repository.getMyProducts(statusFilter).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_productsState.value !is UiState.Success) {
                            _productsState.value = UiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _productsState.value = UiState.Success(resource.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        if (_productsState.value !is UiState.Success) {
                            _productsState.value = UiState.Error(resource.message ?: "Failed to load products")
                        }
                    }
                }
            }
        }
    }

    /**
     * Load categories. Backend categories come from product-service GET /api/categories/.
     * Returns empty list stub - AddEditProductActivity spinners will be disabled.
     */
    fun loadCategories() {
        _categoriesState.value = UiState.Success(emptyList())
    }

    fun fetchAllCategories() {
        viewModelScope.launch {
            repository.getCategories().collectLatest { resource ->
                _allCategoriesState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> UiState.Error(resource.message ?: "Failed to load categories")
                }
            }
        }
    }

    /**
     * Load subcategories for a category.
     * Backend doesn't have a dedicated subcategory endpoint yet.
     */
    fun loadSubcategories(category: String) {
        _subcategoriesState.value = UiState.Success(emptyList())
    }

    /**
     * Load product types for a subcategory.
     * Accepts 1 or 2 params for backward compatibility.
     */
    fun loadProductTypes(category: String, subcategory: String = "") {
        _productTypesState.value = UiState.Success(emptyList())
    }

    /**
     * Get category ID from name (stub - returns the name itself as ID).
     */
    fun getCategoryId(categoryName: String): String = categoryName

    /**
     * Add a new product. Uploads first image from list if provided.
     * @param product SellerProduct data model
     * @param imageUris list of image URIs to upload (uses first one)
     */
    fun addProduct(product: SellerProduct, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _addProductState.value = UiState.Loading
            try {
                var imageUrl = product.imageUrl
                if (imageUris.isNotEmpty()) {
                    val uploadResult = repository.uploadProductImage(imageUris.first(), "new_${System.currentTimeMillis()}")
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrThrow()
                    }
                }
                val allImages = if (imageUrl.isNotEmpty()) listOf(imageUrl) else null
                val request = CreateProductRequest(
                    name = product.name,
                    slug = product.name.lowercase().replace(" ", "-"),
                    description = product.description.ifEmpty { null },
                    basePrice = product.price,                        // MRP = base_price
                    salePrice = product.wholesalePrice,               // Wholesale = sale_price (required)
                    categoryIds = listOf(product.category.ifEmpty { "general" }),
                    images = allImages,
                    initialStock = product.stock,
                    gstRate = product.gstPercent.toDouble(),
                    hsnCode = product.hsnCode.ifEmpty { null },
                    weightKg = null
                )
                val result = repository.createProduct(request)
                _addProductState.value = result.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "Failed to add product") }
                )
            } catch (e: Exception) {
                _addProductState.value = UiState.Error(e.message ?: "Failed to add product")
            }
        }
    }

    /**
     * Update an existing product.
     */
    fun updateProduct(product: SellerProduct, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _updateProductState.value = UiState.Loading
            try {
                var imageUrl = product.imageUrl
                if (imageUris.isNotEmpty()) {
                    val uploadResult = repository.uploadProductImage(imageUris.first(), product.id)
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrThrow()
                    }
                }
                val allImages = if (imageUrl.isNotEmpty()) listOf(imageUrl) else null
                val request = UpdateProductRequest(
                    name = product.name,
                    description = product.description.ifEmpty { null },
                    basePrice = product.price,                        // MRP
                    salePrice = product.wholesalePrice,               // Wholesale — never null when price fields are sent
                    price = product.price,
                    mrp = product.price,
                    category = product.category.ifEmpty { null },
                    subcategory = product.subcategory.ifEmpty { null },
                    images = allImages,
                    stock = product.stock,
                    gstRate = product.gstPercent.toDouble(),
                    hsnCode = product.hsnCode.ifEmpty { null },
                    isActive = product.isActive
                )
                val result = repository.updateProduct(product.id, request)
                _updateProductState.value = result.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "Failed to update product") }
                )
            } catch (e: Exception) {
                _updateProductState.value = UiState.Error(e.message ?: "Failed to update product")
            }
        }
    }

    fun createProduct(request: CreateProductRequest) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            val result = repository.createProduct(request)
            _createState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to create product") }
            )
        }
    }

    fun updateProduct(productId: String, request: UpdateProductRequest) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            val result = repository.updateProduct(productId, request)
            _updateState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to update product") }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            val result = repository.deleteProduct(productId)
            _deleteState.value = result.fold(
                onSuccess = { UiState.Success(true) },
                onFailure = { UiState.Error(it.message ?: "Failed to delete product") }
            )
        }
    }

    /**
     * Load a single product by ID (for edit mode).
     * Result is emitted to productDetailState.
     */
    fun getProductById(productId: String) {
        viewModelScope.launch {
            _productDetailState.value = UiState.Loading
            repository.getProductById(productId).collectLatest { resource ->
                _productDetailState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data!!)
                    is Resource.Error -> UiState.Error(resource.message ?: "Failed to load product")
                }
            }
        }
    }
    /**
     * Search seller's products by name (client-side filter on loaded products).
     * Loads all products and filters by query string.
     */
    fun searchProducts(query: String) {
        viewModelScope.launch {
            _productsState.value = UiState.Loading
            repository.getMyProducts("all").collectLatest { resource ->
                _productsState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val filtered = (resource.data ?: emptyList()).filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.category?.contains(query, ignoreCase = true) == true
                        }
                        UiState.Success(filtered)
                    }
                    is Resource.Error -> UiState.Error(resource.message ?: "Search failed")
                }
            }
        }
    }
    /**
     * Update stock for a product via InventoryRepository.
     * Uses GET /api/inventory/:productId PATCH endpoint.
     */
    fun updateStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            _updateStockState.value = UiState.Loading
            try {
                val inventoryRepo = com.anga9.seller.data.repository.InventoryRepository(getApplication())
                val result = inventoryRepo.updateStock(productId, newStock)
                _updateStockState.value = result.fold(
                    onSuccess = { UiState.Success(true) },
                    onFailure = { UiState.Error(it.message ?: "Failed to update stock") }
                )
            } catch (e: Exception) {
                _updateStockState.value = UiState.Error(e.message ?: "Failed to update stock")
            }
        }
    }

    fun resetStockState() { _updateStockState.value = UiState.Idle }
    fun resetAddState() { _addProductState.value = UiState.Idle }
    fun resetUpdateState() { _updateProductState.value = UiState.Idle }
    fun resetCreateState() { _createState.value = UiState.Idle }
    fun resetDeleteState() { _deleteState.value = UiState.Idle }
}
