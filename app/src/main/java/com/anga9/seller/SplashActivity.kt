package com.anga9.seller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.auth.KycStatusActivity
import com.anga9.seller.auth.LanguageSelectionActivity
import com.anga9.seller.auth.SellerPhoneLoginActivity
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.LocaleHelper
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyPersistedLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#363FF9")
        window.navigationBarColor = android.graphics.Color.parseColor("#2830DE")
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 1500)
    }

    private fun checkAuthState() {
        val isLoggedIn = TokenManager.isLoggedIn(this)

        if (!isLoggedIn) {
            // New user - check if language has been selected
            if (LocaleHelper.isLanguageSelected(this)) {
                goTo(SellerPhoneLoginActivity::class.java)
            } else {
                goTo(LanguageSelectionActivity::class.java)
            }
        } else {
            // Existing user - restore session and check KYC status
            lifecycleScope.launch {
                try {
                    val accessToken = TokenManager.getToken(applicationContext)
                    val refreshToken = TokenManager.getRefreshToken(applicationContext)
                    if (accessToken != null && refreshToken != null) {
                        try {
                            com.anga9.seller.network.SupabaseClient.auth.importAuthToken(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                autoRefresh = false
                            )
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }

                    val profileRepo = ProfileRepository(applicationContext)
                    val result = profileRepo.getSellerProfile()
                    result.fold(
                        onSuccess = { profile ->
                            val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                            prefs.edit().apply {
                                if (!profile.businessName.isNullOrEmpty()) putString("seller_business_name", profile.businessName)
                                putString("cached_kyc_status", profile.kycStatus)
                            }.apply()
                            
                            when (profile.kycStatus) {
                                "verified" -> goTo(DashboardActivity::class.java)
                                "pending" -> goTo(KycStatusActivity::class.java)
                                else -> goTo(com.anga9.seller.auth.SellerRegistrationActivity::class.java)
                            }
                        },
                        onFailure = { error ->
                            val cachedKyc = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                                .getString("cached_kyc_status", null)
                            if (cachedKyc == "verified" || cachedKyc == Constants.KYC_APPROVED) {
                                goTo(DashboardActivity::class.java)
                            } else {
                                goTo(KycStatusActivity::class.java)
                            }
                        }
                    )
                } catch (e: Exception) {
                    val cachedKyc = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                        .getString("cached_kyc_status", null)
                    if (cachedKyc == "verified" || cachedKyc == Constants.KYC_APPROVED) {
                        goTo(DashboardActivity::class.java)
                    } else {
                        goTo(KycStatusActivity::class.java)
                    }
                }
            }
        }
    }

    private fun goTo(cls: Class<*>) {
        startActivity(Intent(this, cls))
        finish()
    }
}