package com.anga9.seller.MVVM.data.repository

import android.content.Context
import android.net.Uri
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.CreateProductRequest
import com.anga9.seller.network.model.ProductListResponse
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.network.model.UpdateProductRequest
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * ProductRepository — Seller App (Phase 3B)
 *
 * Image/Video Upload:
 *   Uses Supabase Storage REST API directly via OkHttp (no SDK needed).
 *   Endpoint: POST /storage/v1/object/<bucket>/<path>
 *   Auth: apikey + Authorization headers with anon key
 *
 * All other data operations go through Retrofit -> ANGA9 API Gateway.
 */
class ProductRepository(private val context: Context) {

    val apiService = ApiClient.getApiService(context)

    // Supabase Storage REST config (same project as web)
    private val supabaseUrl = "https://plfaugkadavxenpqawzw.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc"

    private val httpClient = OkHttpClient()

    /** Get all products for this seller. Optionally filter by status. */
    fun getMyProducts(statusFilter: String = "all"): Flow<Resource<List<SellerProductResponse>>> = flow {
        emit(Resource.Loading())
        try {
            // Phase 4 (Multi-Brand): use effective seller ID so child brand products load correctly
            val sellerId = TokenManager.getEffectiveSellerId(context) ?: run {
                emit(Resource.Error("Not logged in"))
                return@flow
            }
            val status = if (statusFilter == "all") null else statusFilter
            val response = apiService.getSellerProducts(
                sellerId = sellerId,
                status = status,
                page = 1,
                limit = 100
            )
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.getList() ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to load products: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }
    /** Get all categories. */
    fun getCategories(): Flow<Resource<List<com.anga9.seller.network.model.CategoryResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getCategories()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.categories ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to load categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    /** Get single product by ID. */
    fun getProductById(productId: String): Flow<Resource<SellerProductResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getProductById(productId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.product != null) emit(Resource.Success(body.product))
                else emit(Resource.Error("Product not found"))
            } else {
                emit(Resource.Error("Failed to get product: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    /** Create a new product. */
    suspend fun createProduct(request: CreateProductRequest): Result<SellerProductResponse> {
        return try {
            val response = apiService.createProduct(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.product != null) Result.success(body.product)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to create product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Update an existing product. */
    suspend fun updateProduct(productId: String, request: UpdateProductRequest): Result<SellerProductResponse> {
        return try {
            val response = apiService.updateProduct(productId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.product != null) Result.success(body.product)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to update product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /** Bulk upload products via CSV. */
    suspend fun bulkUploadProducts(filePart: okhttp3.MultipartBody.Part): Result<com.anga9.seller.network.model.BulkImportResult> {
        return try {
            val response = apiService.bulkUploadProducts(filePart)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                // Try to parse the error message if possible
                val errorBody = response.errorBody()?.string()
                val message = try {
                    org.json.JSONObject(errorBody ?: "").getString("error")
                } catch (e: Exception) {
                    "Upload failed: ${response.code()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Delete a product. */
    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            val response = apiService.deleteProduct(productId)
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("Failed to delete product: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /**
     * Upload product image to Supabase Storage via REST API (no SDK).
     * Uses same bucket as Seller Web — images are shared/synced.
     *
     * Endpoint: POST /storage/v1/object/product-images/<path>
     * Headers: apikey, Authorization: Bearer <anon_key>, Content-Type: image/jpeg
     *
     * @param uri  Local file URI from image picker
     * @param productId  Used to organize files in storage (seller_id/product_id/filename)
     * @return Public URL of the uploaded image
     */
    suspend fun uploadProductImage(uri: Uri, productId: String): Result<String> {
        return try {
            val sellerId = TokenManager.getEffectiveSellerId(context) // Phase 4: brand-scoped storage folder
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

            val fileName = "${UUID.randomUUID()}.$extension"
            val storagePath = "$sellerId/$productId/$fileName"
            val bucketName = "product-images"

            // Upload via Supabase Storage REST API
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
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

            // Construct public URL (same format as SDK)
            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Image upload failed: ${e.message}"))
        }
    }

    /**
     * Upload product video to Supabase Storage via REST API (no SDK).
     * Bucket: "product-videos"
     *
     * @param uri  Local video URI from file picker
     * @param productId  Used to organize files in storage
     * @return Public URL of the uploaded video
     */
    suspend fun uploadProductVideo(uri: Uri, productId: String): Result<String> {
        return try {
            val sellerId = TokenManager.getEffectiveSellerId(context) // Phase 4: brand-scoped storage folder
                ?: return Result.failure(Exception("Not logged in"))

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot read video file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
            val extension = when (mimeType) {
                "video/quicktime" -> "mov"
                "video/x-msvideo" -> "avi"
                "video/webm" -> "webm"
                else -> "mp4"
            }

            val fileName = "${UUID.randomUUID()}.$extension"
            val storagePath = "$sellerId/$productId/$fileName"
            val bucketName = "product-videos"

            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
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
            Result.failure(Exception("Video upload failed: ${e.message}"))
        }
    }
}