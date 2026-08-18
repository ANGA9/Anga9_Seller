package com.anga9.seller.ui.profile

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.UpdatePreferencesRequest
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.SellerFcmService
import kotlinx.coroutines.launch

/**
 * Notification Settings Activity
 *
 * FIX 14 (Web Team Audit): Preferences now synced to backend on save.
 * Local: SharedPrefs (instant, used by FCM filtering)
 * Remote: PATCH /api/notifications/preferences (backend confirmed ✅)
 */
class NotificationSettingsActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var switchNewOrder: Switch
    private lateinit var switchOrderStatus: Switch
    private lateinit var switchPayoutUpdate: Switch
    private lateinit var switchProductApproval: Switch
    private lateinit var switchLowStock: Switch
    private lateinit var switchPromotional: Switch
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        initViews()
        setupListeners()
        loadPreferences()
        syncFromBackend()   // FIX 14: load latest prefs from backend on open
    }

    private fun initViews() {
        btnBack              = findViewById(R.id.btnBack)
        switchNewOrder       = findViewById(R.id.switchNewOrder)
        switchOrderStatus    = findViewById(R.id.switchOrderStatus)
        switchPayoutUpdate   = findViewById(R.id.switchPayoutUpdate)
        switchProductApproval = findViewById(R.id.switchProductApproval)
        switchLowStock       = findViewById(R.id.switchLowStock)
        switchPromotional    = findViewById(R.id.switchPromotional)
        btnSave              = findViewById(R.id.btnSave)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { savePreferences() }
    }

    // ─── Load from local SharedPrefs (fast, used immediately) ────────────

    private fun loadPreferences() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        switchNewOrder.isChecked        = prefs.getBoolean(SellerFcmService.PREF_NOTIF_NEW_ORDER, true)
        switchOrderStatus.isChecked     = prefs.getBoolean(SellerFcmService.PREF_NOTIF_ORDER_STATUS, true)
        switchPayoutUpdate.isChecked    = prefs.getBoolean(SellerFcmService.PREF_NOTIF_PAYOUT, true)
        switchProductApproval.isChecked = prefs.getBoolean(SellerFcmService.PREF_NOTIF_PRODUCT, true)
        switchLowStock.isChecked        = prefs.getBoolean(SellerFcmService.PREF_NOTIF_LOW_STOCK, true)
        switchPromotional.isChecked     = prefs.getBoolean("notif_promotional", false)
    }

    // ─── FIX 14: Sync latest preferences from backend on open ────────────

    private fun syncFromBackend() {
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@NotificationSettingsActivity)
                val response = apiService.getNotificationPreferences()
                if (response.isSuccessful) {
                    val prefs = response.body()
                    if (prefs != null) {
                        // Update toggles with backend values
                        val pushEnabled = prefs.pushEnabled
                        val remotePrefs = prefs.preferences ?: emptyMap()

                        // Map backend preference keys to our local toggles
                        // Backend key names match what we send in UpdatePreferencesRequest
                        switchNewOrder.isChecked        = remotePrefs["new_order"]     ?: pushEnabled
                        switchOrderStatus.isChecked     = remotePrefs["order_status"]  ?: pushEnabled
                        switchPayoutUpdate.isChecked    = remotePrefs["payout"]        ?: pushEnabled
                        switchProductApproval.isChecked = remotePrefs["product"]       ?: pushEnabled
                        switchLowStock.isChecked        = remotePrefs["low_stock"]     ?: pushEnabled
                        switchPromotional.isChecked     = remotePrefs["promotional"]   ?: false

                        // Also update local SharedPrefs to keep in sync
                        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().apply {
                            putBoolean(SellerFcmService.PREF_NOTIF_NEW_ORDER,    switchNewOrder.isChecked)
                            putBoolean(SellerFcmService.PREF_NOTIF_ORDER_STATUS, switchOrderStatus.isChecked)
                            putBoolean(SellerFcmService.PREF_NOTIF_PAYOUT,       switchPayoutUpdate.isChecked)
                            putBoolean(SellerFcmService.PREF_NOTIF_PRODUCT,      switchProductApproval.isChecked)
                            putBoolean(SellerFcmService.PREF_NOTIF_LOW_STOCK,    switchLowStock.isChecked)
                            putBoolean("notif_promotional",                       switchPromotional.isChecked)
                            apply()
                        }
                        Log.d("NotifSettings", "Synced preferences from backend")
                    }
                }
            } catch (e: Exception) {
                // Silent fail — local prefs already loaded above, user won't notice
                Log.w("NotifSettings", "Could not sync from backend: ${e.message}")
            }
        }
    }

    // ─── FIX 14: Save to local AND backend ───────────────────────────────

    private fun savePreferences() {
        btnSave.isEnabled = false

        // 1. Save locally immediately (fast, used by FCM)
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(SellerFcmService.PREF_NOTIF_NEW_ORDER,    switchNewOrder.isChecked)
            putBoolean(SellerFcmService.PREF_NOTIF_ORDER_STATUS, switchOrderStatus.isChecked)
            putBoolean(SellerFcmService.PREF_NOTIF_PAYOUT,       switchPayoutUpdate.isChecked)
            putBoolean(SellerFcmService.PREF_NOTIF_PRODUCT,      switchProductApproval.isChecked)
            putBoolean(SellerFcmService.PREF_NOTIF_LOW_STOCK,    switchLowStock.isChecked)
            putBoolean("notif_promotional",                       switchPromotional.isChecked)
            apply()
        }

        // 2. Sync to backend (PATCH /api/notifications/preferences — confirmed ✅)
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(this@NotificationSettingsActivity)
                val request = UpdatePreferencesRequest(
                    pushEnabled = switchNewOrder.isChecked || switchOrderStatus.isChecked,
                    preferences = mapOf(
                        "new_order"    to switchNewOrder.isChecked,
                        "order_status" to switchOrderStatus.isChecked,
                        "payout"       to switchPayoutUpdate.isChecked,
                        "product"      to switchProductApproval.isChecked,
                        "low_stock"    to switchLowStock.isChecked,
                        "promotional"  to switchPromotional.isChecked
                    )
                )
                val response = apiService.updateNotificationPreferences(request)
                if (response.isSuccessful) {
                    Log.d("NotifSettings", "Preferences synced to backend")
                } else {
                    Log.w("NotifSettings", "Backend sync failed: ${response.code()}")
                }
            } catch (e: Exception) {
                // Silent fail — local save already done
                Log.w("NotifSettings", "Backend sync error: ${e.message}")
            } finally {
                btnSave.isEnabled = true
            }
        }

        showToast("Notification preferences saved")
        finish()
    }
}