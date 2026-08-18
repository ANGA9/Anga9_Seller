package com.anga9.seller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.FcmTokenManager
import com.anga9.seller.utils.LocaleHelper
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.auth.SellerPhoneLoginActivity

/**
 * Base class for all activities.
 * Handles: locale application, auth check, FCM token refresh.
 * Firebase Auth/Firestore removed — using TokenManager (Supabase JWT).
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyPersistedLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (isLoggedIn) {
            FcmTokenManager.refreshAndRegisterToken(this)
        }
    }

    /** Current logged-in seller UID from TokenManager */
    protected val currentUid: String
        get() = TokenManager.getUserId(this) ?: ""

    /** Check if seller is logged in via TokenManager */
    protected val isLoggedIn: Boolean
        get() = TokenManager.isLoggedIn(this)

    /** Save String value to SharedPrefs */
    protected fun savePref(key: String, value: String) {
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    /** Save Boolean value to SharedPrefs */
    protected fun savePref(key: String, value: Boolean) {
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    protected fun getPrefString(key: String, default: String = ""): String =
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getString(key, default) ?: default

    protected fun getPrefBool(key: String, default: Boolean = false): Boolean =
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(key, default)

    /** Get current seller ID from prefs */
    protected fun getSellerId(): String = getPrefString(Constants.PREF_SELLER_ID)

    /** Show a short toast */
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Logout seller and navigate to login */
    protected fun logout() {
        FcmTokenManager.clearToken(this)
        TokenManager.clearAll(this)
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, SellerPhoneLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ── Phase 5 (Multi-Brand): Brand indicator for secondary screens ─────────
    /**
     * Shows a Toast when a child brand is active so the seller always knows
     * which brand context they are operating in.
     * Call this from onResume() of any child screen (Orders, Wallet, Analytics).
     * Does nothing when the parent brand (own account) is active.
     */
    protected fun showBrandContextIfActive() {
        val activeBrandId = TokenManager.getActiveBrandId(this) ?: return
        val prefs = getSharedPreferences("anga9_seller_prefs", android.content.Context.MODE_PRIVATE)
        val brandName = prefs.getString("brand_name_", null)
        val label = if (!brandName.isNullOrEmpty()) brandName else "Child Brand"
        android.widget.Toast.makeText(
            this,
            "\uD83C\uDFEA Viewing: ",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}