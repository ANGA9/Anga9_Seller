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

    private val gson = com.google.gson.Gson()
    private val prefs = context.getSharedPreferences("seller_products_cache", Context.MODE_PRIVATE)

    /** Get all products for this seller. Optionally filter by status. */
    fun getMyProducts(statusFilter: String = "all"): Flow<Resource<List<SellerProductResponse>>> = flow {
        val sellerId = TokenManager.getEffectiveSellerId(context) ?: run {
            emit(Resource.Error("Not logged in"))
            return@flow
        }
        val cacheKey = "products_${sellerId}_$statusFilter"

        // 1. Emit cached products if present
        var cachedProducts: List<SellerProductResponse>? = null
        val cachedJson = prefs.getString(cacheKey, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<SellerProductResponse>>() {}.type
                cachedProducts = gson.fromJson(cachedJson, type)
                if (!cachedProducts.isNullOrEmpty()) {
                    emit(Resource.Success(cachedProducts))
                }
            } catch (e: Exception) {
                // Ignore cache parsing errors
            }
        }

        if (cachedProducts == null) {
            emit(Resource.Loading())
        }

        try {
            val status = if (statusFilter == "all") null else statusFilter
            val response = apiService.getSellerProducts(
                sellerId = sellerId,
                status = status,
                page = 1,
                limit = 100
            )
            if (response.isSuccessful) {
                val products = response.body()?.getList() ?: emptyList()
                try {
                    prefs.edit().putString(cacheKey, gson.toJson(products)).apply()
                } catch (e: Exception) {
                    // Ignore
                }
                emit(Resource.Success(products))
            } else {
                if (cachedProducts != null) return@flow
                emit(Resource.Error("Failed to load products: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (cachedProducts == null) {
                emit(Resource.Error(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to load products")))
            }
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
            emit(Resource.Error(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to load categories")))
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
            emit(Resource.Error(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to get product")))
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
            Result.failure(Exception(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to create product")))
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
            Result.failure(Exception(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to update product")))
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
            Result.failure(Exception(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Upload failed")))
        }
    }

    /** Delete a product. */
    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            val response = apiService.deleteProduct(productId)
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("Failed to delete product: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(Exception(com.anga9.seller.utils.AppFormatters.getHumanErrorMessage(e, "Failed to delete product")))
        }
    }

    /**
     * Upload product image to Supabase Storage.
     * Uses same bucket and format as Seller Web (`product-images`).
     *
     * @param uri  Local file URI from image picker
     * @param productId  Optional product identifier
     * @return Public URL of the uploaded image
     */
    suspend fun uploadProductImage(uri: Uri, productId: String = ""): Result<String> {
        return try {
            val sellerId = TokenManager.getEffectiveSellerId(context)
                ?: TokenManager.getUserId(context)
                ?: "seller"

            val userToken = TokenManager.getToken(context)
            val refreshToken = TokenManager.getRefreshToken(context)

            // Ensure SupabaseClient session is initialized
            if (userToken != null && com.anga9.seller.network.SupabaseClient.auth.currentSessionOrNull() == null) {
                try {
                    com.anga9.seller.network.SupabaseClient.auth.importAuthToken(
                        accessToken = userToken,
                        refreshToken = refreshToken ?: "",
                        autoRefresh = true
                    )
                } catch (e: Exception) {
                    // Ignore session import failure and proceed with REST fallback
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot read image file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }

            val fileName = "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}.$extension"
            val storagePath = "$sellerId/$fileName"
            val bucketName = "product-images"

            // 1. Try Supabase Kotlin SDK upload
            try {
                val bucket = com.anga9.seller.network.SupabaseClient.storage.from(bucketName)
                bucket.upload(storagePath, bytes) {
                    upsert = true
                }
                val publicUrl = bucket.publicUrl(storagePath)
                return Result.success(publicUrl)
            } catch (sdkEx: Exception) {
                android.util.Log.w("ProductRepo", "SDK upload failed (${sdkEx.message}), attempting REST fallback")
            }

            // 2. Fallback to Supabase Storage REST API with User JWT
            val authHeader = if (!userToken.isNullOrBlank()) "Bearer $userToken" else "Bearer $supabaseAnonKey"
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                return Result.failure(Exception("Upload failed [${response.code}]: $errBody"))
            }
            response.close()

            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Image upload failed: ${e.message}"))
        }
    }

    /**
     * Upload product video to Supabase Storage.
     * Bucket: "product-videos"
     */
    suspend fun uploadProductVideo(uri: Uri, productId: String = ""): Result<String> {
        return try {
            val sellerId = TokenManager.getEffectiveSellerId(context)
                ?: TokenManager.getUserId(context)
                ?: "seller"

            val userToken = TokenManager.getToken(context)
            val refreshToken = TokenManager.getRefreshToken(context)

            if (userToken != null && com.anga9.seller.network.SupabaseClient.auth.currentSessionOrNull() == null) {
                try {
                    com.anga9.seller.network.SupabaseClient.auth.importAuthToken(
                        accessToken = userToken,
                        refreshToken = refreshToken ?: "",
                        autoRefresh = true
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot read video file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
            val extension = when {
                mimeType.contains("webm") -> "webm"
                mimeType.contains("mov") -> "mov"
                else -> "mp4"
            }

            val fileName = "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}.$extension"
            val storagePath = "$sellerId/$fileName"
            val bucketName = "product-videos"

            try {
                val bucket = com.anga9.seller.network.SupabaseClient.storage.from(bucketName)
                bucket.upload(storagePath, bytes) {
                    upsert = true
                }
                val publicUrl = bucket.publicUrl(storagePath)
                return Result.success(publicUrl)
            } catch (sdkEx: Exception) {
                android.util.Log.w("ProductRepo", "SDK video upload failed (${sdkEx.message}), attempting REST fallback")
            }

            val authHeader = if (!userToken.isNullOrBlank()) "Bearer $userToken" else "Bearer $supabaseAnonKey"
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                return Result.failure(Exception("Video upload failed [${response.code}]: $errBody"))
            }
            response.close()

            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Video upload failed: ${e.message}"))
        }
    }
}