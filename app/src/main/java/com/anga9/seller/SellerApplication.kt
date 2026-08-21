package com.anga9.seller

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.anga9.seller.utils.LocaleHelper
import kotlinx.coroutines.launch

/**
 * Application class — applies persisted locale before any Activity starts.
 * This ensures the correct language is set even after app restart.
 */
class SellerApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyPersistedLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-apply locale when system configuration changes (e.g. screen rotation)
        LocaleHelper.applyPersistedLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Phase 6 Bugfix: Prevent 20-25 minute auto-logout issue.
        // Supabase auto-refreshes the token in-memory, but TokenManager gets stale.
        // Sync any new session generated in the background into TokenManager.
        kotlinx.coroutines.GlobalScope.launch {
            com.anga9.seller.network.SupabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val uid = com.anga9.seller.utils.TokenManager.getUserId(this@SellerApplication)
                        if (!uid.isNullOrEmpty()) {
                            com.anga9.seller.utils.TokenManager.saveTokens(
                                this@SellerApplication,
                                status.session.accessToken,
                                status.session.refreshToken ?: ""
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
