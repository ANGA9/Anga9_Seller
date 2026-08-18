package com.anga9.seller.network

import android.content.Context
import com.anga9.seller.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches the Supabase JWT token
 * to every outgoing request as a Bearer Authorization header.
 *
 * Phase 1 (Multi-Brand): Also injects X-Brand-ID header when a child
 * brand is active. The backend authMiddleware intercepts this header,
 * validates that the target brand's parent_user_id == caller.id,
 * and impersonates the child brand transparently.
 *
 * When X-Brand-ID is NOT set (null), the backend defaults to the
 * authenticated user's own brand — no change in existing behaviour.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val supabaseToken = com.anga9.seller.network.SupabaseClient.auth.currentAccessTokenOrNull()
        val token = supabaseToken ?: TokenManager.getToken(context)
        val activeBrandId = TokenManager.getActiveBrandId(context)
        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        // Inject X-Brand-ID only when impersonating a child brand.
        // If activeBrandId is null, header is omitted and backend uses
        // the authenticated caller's own brand context (default behaviour).
        if (activeBrandId != null) {
            builder.header("X-Brand-ID", activeBrandId)
        }

        var response = chain.proceed(builder.build())

        // Phase 6: Handle 1-hour auto-logout issue. 
        // If 401 Unauthorized, try to refresh the token using the refresh token stored in TokenManager.
        // Synchronized to prevent concurrent refresh requests throwing `refresh_token_already_used`.
        if (response.code == 401) {
            synchronized(this) {
                // Check if another thread already refreshed the token
                val currentTokenInManager = TokenManager.getToken(context)
                if (currentTokenInManager != null && currentTokenInManager != token) {
                    response.close()
                    val retryBuilder = originalRequest.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer $currentTokenInManager")
                    if (activeBrandId != null) {
                        retryBuilder.header("X-Brand-ID", activeBrandId)
                    }
                    return chain.proceed(retryBuilder.build())
                }

                val refreshToken = TokenManager.getRefreshToken(context)
                if (refreshToken != null) {
                    try {
                        kotlinx.coroutines.runBlocking {
                            // If Supabase memory session is empty, import it first WITHOUT auto-refresh
                            if (com.anga9.seller.network.SupabaseClient.auth.currentSessionOrNull() == null) {
                                com.anga9.seller.network.SupabaseClient.auth.importAuthToken(
                                    accessToken = token ?: "",
                                    refreshToken = refreshToken,
                                    autoRefresh = false
                                )
                            }
                            // Now explicitly refresh the session. This WILL throw an exception if it fails
                            // (e.g. refresh_token_already_used), which will be caught by the outer catch block.
                            com.anga9.seller.network.SupabaseClient.auth.refreshCurrentSession()
                        }
                        val newToken = com.anga9.seller.network.SupabaseClient.auth.currentAccessTokenOrNull()
                        if (newToken != null && newToken != token) {
                            // Save the new tokens
                            val newRefresh = com.anga9.seller.network.SupabaseClient.auth.currentSessionOrNull()?.refreshToken ?: refreshToken
                            TokenManager.saveTokens(context, newToken, newRefresh)
                            
                            // Retry the request with the new token
                            response.close()
                            val retryBuilder = originalRequest.newBuilder()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Bearer $newToken")
                            if (activeBrandId != null) {
                                retryBuilder.header("X-Brand-ID", activeBrandId)
                            }
                            response = chain.proceed(retryBuilder.build())
                        } else {
                            // Supabase SDK swallowed the exception and cleared the session.
                            // We must manually throw an exception to trigger the logout block.
                            throw Exception("Session refresh failed silently")
                        }
                    } catch (e: Exception) {
                        // Refresh failed! Token is expired or revoked.
                        // Force logout the user locally and redirect to Login screen
                        TokenManager.clearAll(context)
                        try {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    val intent = android.content.Intent(context, com.anga9.seller.auth.SellerPhoneLoginActivity::class.java).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e3: Exception) {
                                    // Ignore
                                }
                            }
                        } catch (e2: Exception) {
                            // Ignore if we can't post to main thread
                        }
                    }
                }
            }
        }

        return response
    }
}
