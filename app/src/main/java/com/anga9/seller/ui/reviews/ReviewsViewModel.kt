package com.anga9.seller.ui.reviews

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.repository.ReviewsRepository
import com.anga9.seller.network.model.SellerReviewListResponse
import com.anga9.seller.network.model.SellerReviewItem
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReviewsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<UiState<SellerReviewListResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<SellerReviewListResponse>> = _uiState.asStateFlow()

    private val _filteredReviews = MutableStateFlow<List<SellerReviewItem>>(emptyList())
    val filteredReviews: StateFlow<List<SellerReviewItem>> = _filteredReviews.asStateFlow()

    private var allReviews: List<SellerReviewItem> = emptyList()

    private val _currentSort = MutableStateFlow("newest")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    private val _currentQuery = MutableStateFlow("")

    fun loadReviews(sort: String = _currentSort.value) {
        _currentSort.value = sort
        viewModelScope.launch {
            repository.getSellerReviews(sort).collect { state ->
                _uiState.value = state
                if (state is UiState.Success) {
                    allReviews = state.data.data
                    applyFilter(_currentQuery.value)
                }
            }
        }
    }

    fun applyFilter(query: String) {
        _currentQuery.value = query
        if (query.isBlank()) {
            _filteredReviews.value = allReviews
        } else {
            val lowerQuery = query.lowercase()
            _filteredReviews.value = allReviews.filter { review ->
                review.title?.lowercase()?.contains(lowerQuery) == true ||
                review.body?.lowercase()?.contains(lowerQuery) == true ||
                review.products?.name?.lowercase()?.contains(lowerQuery) == true ||
                review.userName.lowercase().contains(lowerQuery)
            }
        }
    }
}
