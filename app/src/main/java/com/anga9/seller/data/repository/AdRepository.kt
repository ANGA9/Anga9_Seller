package com.anga9.seller.data.repository

import android.content.Context
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.AdCampaignResponse
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AdRepository(private val context: Context) {

    private val api by lazy { ApiClient.getApiService(context) }

    fun getMyAds(): Flow<Resource<List<AdCampaignResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getMyAds()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.ads ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to fetch ads: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }

    fun requestAd(request: com.anga9.seller.network.model.AdRequest): Flow<Resource<AdCampaignResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.requestAd(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to submit ad request: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }

    private val supabaseUrl = "https://plfaugkadavxenpqawzw.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc"
    private val httpClient = okhttp3.OkHttpClient()

    suspend fun uploadBannerImage(uri: android.net.Uri): Result<String> {
        return try {
            val sellerId = com.anga9.seller.utils.TokenManager.getEffectiveSellerId(context)
                ?: return Result.failure(Exception("Not logged in"))

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot read image file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val fileName = "${java.util.UUID.randomUUID()}.$extension"
            val storagePath = "$sellerId/ads/$fileName"
            val bucketName = "product-images" // using existing bucket for simplicity, or we can use ad-banners
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"

            val mediaType = mimeType.toMediaType()
            val requestBody = bytes.toRequestBody(mediaType)
            val request = okhttp3.Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", mimeType)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Upload failed: ${response.code} ${response.body?.string()}"))
            }
            response.close()

            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Image upload failed: ${e.message}"))
        }
    }
}
