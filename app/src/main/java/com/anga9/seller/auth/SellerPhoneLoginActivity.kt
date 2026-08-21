package com.anga9.seller.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.repository.AuthRepository
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Phone OTP Login - Seller App
 *
 * supabase-kt 3.6.0 ka Phone provider password-based login hai (OTP nahi).
 * Isliye direct REST API call karte hain: POST /auth/v1/otp
 * Same endpoint jo web app use karta hai.
 */
class SellerPhoneLoginActivity : BaseActivity() {

    private lateinit var etPhone: EditText
    private lateinit var btnGetOtp: TextView
    private lateinit var progressBar: ProgressBar

    // Supabase project details
    private val supabaseUrl = "https://plfaugkadavxenpqawzw.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc"

    private val httpClient = OkHttpClient()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_phone_login)

        // Apply system insets for proper status bar padding on notched phones
        applySystemInsets(findViewById(R.id.rootLoginContent))

        etPhone     = findViewById(R.id.etPhone)
        btnGetOtp   = findViewById(R.id.btnGetOtp)
        progressBar = findViewById(R.id.progressBar)

        etPhone.hint = "Enter 10-digit mobile number"
        etPhone.inputType = android.text.InputType.TYPE_CLASS_PHONE

        btnGetOtp.isEnabled = false
        btnGetOtp.alpha = 0.5f

        setupInputWatcher()

        btnGetOtp.setOnClickListener {
            val raw = etPhone.text.toString().trim()
            val normalized = PhoneUtils.normalizeIndianPhone(raw)
            if (normalized == null) {
                etPhone.error = "Please enter a valid 10-digit Indian mobile number"
                return@setOnClickListener
            }
            sendPhoneOtp(normalized)
        }
    }

    private fun setupInputWatcher() {
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isValid = PhoneUtils.isValidIndianPhone(s?.toString()?.trim() ?: "")
                btnGetOtp.isEnabled = isValid
                btnGetOtp.alpha = if (isValid) 1.0f else 0.5f
                etPhone.error = null
            }
        })
    }

    private fun sendPhoneOtp(e164Phone: String) {
        setLoading(true)
        Log.d("OTP_DEBUG", "Calling /auth/v1/otp for: $e164Phone")

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Direct REST API call to /auth/v1/otp endpoint
                    // This is the correct OTP endpoint (same as web app's signInWithOtp)
                    val body = JSONObject().apply {
                        put("phone", e164Phone)
                    }.toString()

                    val request = Request.Builder()
                        .url("$supabaseUrl/auth/v1/otp")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""
                    Log.d("OTP_DEBUG", "Response code: ${response.code}, body: $responseBody")
                    Pair(response.code, responseBody)
                }

                setLoading(false)
                val (code, body) = result

                if (code == 200 || code == 204) {
                    Log.d("OTP_DEBUG", "OTP sent successfully!")
                    navigateToOtp(e164Phone)
                } else {
                    Log.e("OTP_DEBUG", "OTP failed: $code - $body")
                    val errorMsg = try {
                        JSONObject(body).optString("message", "Failed to send OTP")
                    } catch (e: Exception) {
                        "Failed to send OTP. Please try again"
                    }
                    Toast.makeText(this@SellerPhoneLoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("OTP_DEBUG", "Network error: ${e.message}")
                setLoading(false)
                Toast.makeText(
                    this@SellerPhoneLoginActivity,
                    "No internet connection. Please check your network",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun navigateToOtp(e164Phone: String) {
        val intent = Intent(this, SellerOtpVerifyActivity::class.java).apply {
            putExtra("phone", e164Phone)
            putExtra("auth_type", "phone")
        }
        startActivity(intent)
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnGetOtp.isEnabled = !loading
        btnGetOtp.text = if (loading) getString(R.string.loading) else getString(R.string.btn_get_otp)
        etPhone.isEnabled = !loading
    }
}