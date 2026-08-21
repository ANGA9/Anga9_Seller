package com.anga9.seller.network

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client.
 * Base URL points to the ANGA9 API Gateway (port 4000).
 *
 * Optimized for high performance, connection reuse, and minimal I/O overhead.
 */
object ApiClient {

    private const val BASE_URL = "https://api.anga9.com/"

    @Volatile
    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = try {
            Cache(cacheDir, 10L * 1024L * 1024L) // 10 MB
        } catch (e: Exception) {
            null
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .apply { if (cache != null) cache(cache) }
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getApiService(context: Context): ApiService =
        getInstance(context).create(ApiService::class.java)
}
