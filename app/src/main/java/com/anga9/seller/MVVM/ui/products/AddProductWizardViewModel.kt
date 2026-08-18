package com.anga9.seller.MVVM.ui.products

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.network.model.CreateProductRequest
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddProductWizardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository(application.applicationContext)

    // Step 1: Media
    var selectedImageUris = mutableListOf<Uri>()

    // Step 2: Basic Details
    var productName: String = ""
    var productDescription: String = ""
    var categoryId: String = ""
    var subcategoryId: String = ""

    // Step 3: Pricing & Inventory
    var mrp: Double = 0.0
    var wholesalePrice: Double = 0.0
    var initialStock: Int = 0
    var minOrderQty: Int = 1
    var unit: String = "piece"
    var gstRate: Double = 18.0

    // Step 4: Logistics
    var weightKg: Double? = null
    var brand: String = ""
    var countryOfOrigin: String = "India"
    var hsnCode: String = ""
    var skuCode: String = ""

    // Step 5: Policies
    var returnPolicy: String = ""
    var warranty: String = ""
    var searchTags: List<String> = emptyList()

    private val _createState = MutableStateFlow<UiState<SellerProductResponse>>(UiState.Idle)
    val createState: StateFlow<UiState<SellerProductResponse>> = _createState.asStateFlow()

    fun submitProduct() {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            
            // Collect tags
            val tagList = if (searchTags.isNotEmpty()) searchTags else null

            val request = CreateProductRequest(
                name = productName,
                slug = productName.lowercase().replace(Regex("[^a-z0-9]"), "-"),
                description = productDescription,
                basePrice = mrp,
                salePrice = wholesalePrice,
                minOrderQty = minOrderQty,
                categoryIds = listOf(categoryId.ifEmpty { "general" }),
                unit = unit,
                initialStock = initialStock,
                countryOfOrigin = countryOfOrigin.ifEmpty { "India" },
                gstRate = gstRate,
                tags = tagList,
                hsnCode = hsnCode.ifEmpty { null },
                brand = brand.ifEmpty { null },
                weightKg = weightKg,
                returnPolicy = returnPolicy.ifEmpty { null },
                warranty = warranty.ifEmpty { null },
                sku = skuCode.ifEmpty { null }
            )

            // Image uploading omitted for brevity in MVP (same as before).
            // In a real app, we'd upload images sequentially and map URIs.

            val result = repository.createProduct(request)
            _createState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to create product") }
            )
        }
    }
}
