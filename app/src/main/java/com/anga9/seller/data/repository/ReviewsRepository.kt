package com.anga9.seller.data.repository

import android.content.Context
import android.util.Log
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.SellerReviewListResponse
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class ReviewsRepository(private val context: Context) {

    fun getSellerReviews(sort: String? = null): Flow<UiState<SellerReviewListResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = ApiClient.getApiService(context).getSellerReviews(sort)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    emit(UiState.Success(body))
                } else {
                    emit(UiState.Error("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                var errorMsg = "Failed to load reviews"
                try {
                    if (errorBody != null) {
                        val json = JSONObject(errorBody)
                        if (json.has("message")) {
                            errorMsg = json.getString("message")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ReviewsRepo", "JSON parse error", e)
                }
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Network error occurred"))
        }
    }
}
