package com.anga9.seller.ui.deals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.CreateDealRequest
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CreateDealViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)

    val selectedProduct = MutableLiveData<SellerProduct?>()
    val dealType = MutableLiveData<DealTypeItem?>()
    val dealPrice = MutableLiveData<String>()
    
    val startsAt = MutableLiveData<Date?>()
    val endsAt = MutableLiveData<Date?>()

    private val _submitState = MutableLiveData<Resource<Unit>>()
    val submitState: LiveData<Resource<Unit>> get() = _submitState

    fun isFormValid(): Boolean {
        val product = selectedProduct.value ?: return false
        val type = dealType.value ?: return false
        val priceStr = dealPrice.value ?: return false
        val price = priceStr.toDoubleOrNull() ?: return false
        val start = startsAt.value ?: return false
        val end = endsAt.value ?: return false

        if (price >= product.price) return false
        if (!end.after(start)) return false

        return true
    }

    fun submitDeal() {
        if (!isFormValid()) return
        
        val product = selectedProduct.value!!
        val type = dealType.value!!
        val price = dealPrice.value!!.toDouble()
        
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startIso = format.format(startsAt.value!!)
        val endIso = format.format(endsAt.value!!)

        val request = CreateDealRequest(
            productId = product.id,
            type = type.title,
            dealPrice = price,
            startsAt = startIso,
            endsAt = endIso,
            quantityThreshold = 1
        )

        _submitState.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val response = api.createDeal(request)
                if (response.isSuccessful && response.body() != null) {
                    _submitState.postValue(Resource.Success(Unit))
                } else {
                    _submitState.postValue(Resource.Error("Failed to create deal: ${response.message()}"))
                }
            } catch (e: Exception) {
                _submitState.postValue(Resource.Error(e.message ?: "An error occurred"))
            }
        }
    }
}
