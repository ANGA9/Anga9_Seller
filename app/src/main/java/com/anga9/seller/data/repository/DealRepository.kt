package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.CreateDealRequest
import com.anga9.seller.network.model.DealItem
import com.anga9.seller.network.model.UpdateDealRequest
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DealRepository(private val context: Context) {
    private val api by lazy { ApiClient.getApiService(context) }

    fun getDeals(productId: String? = null, activeOnly: Boolean? = null): Flow<Resource<List<DealItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getDeals(productId, activeOnly)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.deals ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to fetch deals: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }

    suspend fun createDeal(request: CreateDealRequest): Resource<DealItem> {
        return try {
            val response = api.createDeal(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to create deal")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateDealStatus(dealId: String, active: Boolean): Resource<DealItem> {
        return try {
            val response = api.updateDeal(dealId, UpdateDealRequest(active))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to update deal")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteDeal(dealId: String): Resource<Unit> {
        return try {
            val response = api.deleteDeal(dealId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to delete deal")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
