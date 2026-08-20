package com.anga9.seller.ui.ads

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.data.repository.AdRepository
import com.anga9.seller.network.model.AdRequest
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CreateAdViewModel(application: Application) : AndroidViewModel(application) {

    private val adRepository = AdRepository(application.applicationContext)
    private val productRepository = ProductRepository(application.applicationContext)

    // Form Fields
    private val _selectedProduct = MutableStateFlow<SellerProductResponse?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _placement = MutableStateFlow<String?>("home_hero")
    val placement = _placement.asStateFlow()

    private val _bannerUri = MutableStateFlow<Uri?>(null)
    val bannerUri = _bannerUri.asStateFlow()

    private val _headline = MutableStateFlow("")
    val headline = _headline.asStateFlow()

    private val _ctaText = MutableStateFlow<String?>("Shop Now")
    val ctaText = _ctaText.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate = _endDate.asStateFlow()

    private val _budgetInr = MutableStateFlow<Double?>(500.0)
    val budgetInr = _budgetInr.asStateFlow()

    // Form Validations
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors = _validationErrors.asStateFlow()

    // Products list for Bottom Sheet
    private val _productsListState = MutableStateFlow<UiState<List<SellerProductResponse>>>(UiState.Idle)
    val productsListState = _productsListState.asStateFlow()

    // Submission State
    private val _submitState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val submitState = _submitState.asStateFlow()

    init {
        val tmrw = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val nextWk = Calendar.getInstance().apply {
            timeInMillis = tmrw.timeInMillis
            add(Calendar.DAY_OF_YEAR, 7)
        }
        _startDate.value = tmrw.timeInMillis
        _endDate.value = nextWk.timeInMillis

        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _productsListState.value = UiState.Loading
            productRepository.getMyProducts("active,pending_review,draft,archived,rejected").collectLatest { resource ->
                when (resource) {
                    is Resource.Success<*> -> {
                        val list = (resource.data as? List<SellerProductResponse>) ?: emptyList()
                        _productsListState.value = UiState.Success(list)
                        if (_selectedProduct.value == null && list.isNotEmpty()) {
                            _selectedProduct.value = list[0]
                        }
                    }
                    is Resource.Error -> {
                        _productsListState.value = UiState.Error(resource.message ?: "Failed to load products")
                    }
                    is Resource.Loading<*> -> {}
                }
            }
        }
    }

    fun setProduct(product: SellerProductResponse) { _selectedProduct.value = product }
    fun setPlacement(placementValue: String) { _placement.value = placementValue }
    fun setBannerUri(uri: Uri) { _bannerUri.value = uri }
    fun setHeadline(text: String) { _headline.value = text }
    fun setCtaText(text: String) { _ctaText.value = text }
    fun setStartDate(timeInMillis: Long) { _startDate.value = timeInMillis }
    fun setEndDate(timeInMillis: Long) { _endDate.value = timeInMillis }
    fun setBudget(budget: Double?) { _budgetInr.value = budget }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (_selectedProduct.value == null) errors["product"] = "Please select a product"
        if (_placement.value == null) errors["placement"] = "Please select a placement area"
        if (_bannerUri.value == null) errors["banner"] = "Please upload a banner image"
        
        val headlineVal = _headline.value.trim()
        if (headlineVal.length < 5 || headlineVal.length > 100) {
            errors["headline"] = "Headline must be between 5 and 100 characters"
        }
        
        if (_ctaText.value == null) errors["cta"] = "Please select a Call to Action"
        
        val start = _startDate.value
        val end = _endDate.value
        if (start == null) errors["start_date"] = "Required field"
        if (end == null) {
            errors["end_date"] = "Required field"
        } else if (start != null && end <= start) {
            errors["end_date"] = "End date must be after start date"
        }

        val budget = _budgetInr.value ?: 0.0
        if (budget < 500.0) errors["budget"] = "Minimum budget is ₹500"

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    fun submitCampaign() {
        if (!validate()) return

        viewModelScope.launch {
            _submitState.value = UiState.Loading

            // 1. Upload Banner Image
            val uploadResult = adRepository.uploadBannerImage(_bannerUri.value!!)
            if (uploadResult.isFailure) {
                _submitState.value = UiState.Error(uploadResult.exceptionOrNull()?.message ?: "Failed to upload image")
                return@launch
            }
            val bannerUrl = uploadResult.getOrThrow()

            // 2. Format Dates to ISO-8601
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val startsAtStr = sdf.format(Date(_startDate.value!!))
            val endsAtStr = sdf.format(Date(_endDate.value!!))

            // 3. Create Request Object
            val request = AdRequest(
                productId = _selectedProduct.value!!.id,
                placement = _placement.value!!,
                startsAt = startsAtStr,
                endsAt = endsAtStr,
                bannerUrl = bannerUrl,
                headline = _headline.value.trim(),
                ctaText = _ctaText.value ?: "Shop Now",
                budgetInr = _budgetInr.value ?: 500.0
            )

            // 4. Submit to Backend API
            adRepository.requestAd(request).collectLatest { resource ->
                when (resource) {
                    is Resource.Success<*> -> {
                        _submitState.value = UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        _submitState.value = UiState.Error(resource.message ?: "Failed to submit request")
                    }
                    is Resource.Loading<*> -> {}
                }
            }
        }
    }
}
