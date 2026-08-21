package com.anga9.seller.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
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
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.launch
import com.anga9.seller.network.SupabaseClient
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.Constants
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * OTP Verification - Seller App
 *
 * Supports:
 *   auth_type = "phone" -> verifyPhoneOtp() via Supabase -> MSG91 SMS
 *   auth_type = "email" -> verifyEmailOtp() via Supabase -> email
 *
 * After verification:
 *   - kycStatus == "approved"          -> DashboardActivity
 *   - kycStatus == null/pending/not_submitted -> SellerRegistrationActivity
 *   - kycStatus == rejected            -> KycStatusActivity
 */
class SellerOtpVerifyActivity : BaseActivity() {

    private val otpBoxes = arrayOfNulls<EditText>(6)
    private lateinit var tvPhone: TextView
    private lateinit var tvChange: TextView
    private lateinit var tvResendTimer: TextView
    private lateinit var tvResend: TextView
    private lateinit var btnVerify: TextView
    private lateinit var progressBar: ProgressBar

    private var identifier = ""   // E.164 phone (+91...) or email
    private var authType = "phone"
    private var countDownTimer: CountDownTimer? = null
    private var resendAttempts = 0
    private val maxResendAttempts = 3

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_otp_verify)

        applySystemInsets(findViewById(R.id.rootOtpContent))

        identifier = intent.getStringExtra("phone") ?: ""
        authType   = intent.getStringExtra("auth_type") ?: "phone"
        authRepository = AuthRepository(applicationContext)

        initViews()
        setupOtpBoxes()

        // Show masked identifier
        tvPhone.text = if (authType == "phone") maskPhone(identifier) else maskEmail(identifier)

        tvChange.setOnClickListener { finish() }

        btnVerify.setOnClickListener { attemptVerify() }

        tvResend.setOnClickListener {
            if (resendAttempts < maxResendAttempts) {
                resendOtp()
            } else {
                Toast.makeText(this, "Maximum resend attempts reached. Please go back and try again.", Toast.LENGTH_SHORT).show()
            }
        }

        startResendTimer(30)
    }

    private fun initViews() {
        otpBoxes[0] = findViewById(R.id.etOtp1)
        otpBoxes[1] = findViewById(R.id.etOtp2)
        otpBoxes[2] = findViewById(R.id.etOtp3)
        otpBoxes[3] = findViewById(R.id.etOtp4)
        otpBoxes[4] = findViewById(R.id.etOtp5)
        otpBoxes[5] = findViewById(R.id.etOtp6)

        tvPhone       = findViewById(R.id.tvPhone)
        tvChange      = findViewById(R.id.tvChange)
        tvResendTimer = findViewById(R.id.tvResendTimer)
        tvResend      = findViewById(R.id.tvResend)
        btnVerify     = findViewById(R.id.btnVerify)
        progressBar   = findViewById(R.id.progressBar)
    }

    private fun setupOtpBoxes() {
        for (i in 0..5) {
            val box = otpBoxes[i] ?: continue

            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < 5) {
                        otpBoxes[i + 1]?.requestFocus()
                    }
                    // Auto-submit when all 6 digits filled
                    if (getOtpString().length == 6) {
                        attemptVerify()
                    }
                }
            })

            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    box.text.isEmpty() && i > 0
                ) {
                    otpBoxes[i - 1]?.apply {
                        requestFocus()
                        setText("")
                    }
                    true
                } else false
            }
        }
        otpBoxes[0]?.requestFocus()
    }

    private fun getOtpString(): String =
        otpBoxes.joinToString("") { it?.text?.toString() ?: "" }

    private fun attemptVerify() {
        val otp = getOtpString()
        if (otp.length != 6) {
            Toast.makeText(this, "Please enter complete 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }
        verifyOtpWithSupabase(otp)
    }

    // ── Verification ─────────────────────────────────────────────────────

    private fun verifyOtpWithSupabase(otp: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                // Verify OTP based on auth type
                if (authType == "phone") {
                    SupabaseClient.auth.verifyPhoneOtp(
                        type  = OtpType.Phone.SMS,
                        phone = identifier,
                        token = otp
                    )
                } else {
                    SupabaseClient.auth.verifyEmailOtp(
                        type  = OtpType.Email.EMAIL,
                        email = identifier,
                        token = otp
                    )
                }

                val session     = SupabaseClient.auth.currentSessionOrNull()
                val accessToken = session?.accessToken
                val refreshToken = session?.refreshToken

                if (accessToken == null) {
                    setLoading(false)
                    Toast.makeText(this@SellerOtpVerifyActivity, "Session error. Please try again.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Verify token with backend & get KYC status
                val result = authRepository.verifyTokenWithBackend(accessToken, refreshToken)

                setLoading(false)
                result.fold(
                    onSuccess = { user ->
                        // Save seller ID
                        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(Constants.PREF_SELLER_ID, user.id).apply()

                        // Fetch seller profile to get exact verification_status
                        val profileRepo = ProfileRepository(applicationContext)
                        val profileResult = profileRepo.getSellerProfile()
                        
                        profileResult.fold(
                            onSuccess = { profile ->
                                val verificationStatus = profile.kycStatus // Maps to verification_status
                                when {
                                    verificationStatus == "verified" -> {
                                        startActivity(
                                            Intent(this@SellerOtpVerifyActivity, DashboardActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        )
                                    }
                                    verificationStatus == "pending" -> {
                                        startActivity(
                                            Intent(this@SellerOtpVerifyActivity, KycStatusActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        )
                                    }
                                    else -> {
                                        startActivity(
                                            Intent(this@SellerOtpVerifyActivity, SellerRegistrationActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        )
                                    }
                                }
                                finish()
                            },
                            onFailure = {
                                // Profile doesn't exist or network error -> route to registration
                                startActivity(
                                    Intent(this@SellerOtpVerifyActivity, SellerRegistrationActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                )
                                finish()
                            }
                        )
                    },
                    onFailure = { e ->
                        clearOtpBoxes()
                        Toast.makeText(this@SellerOtpVerifyActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                setLoading(false)
                clearOtpBoxes()
                val message = when {
                    e.message?.contains("invalid", ignoreCase = true) == true ->
                        "Invalid OTP. Please check and try again"
                    e.message?.contains("expired", ignoreCase = true) == true ->
                        "OTP has expired. Please request a new one"
                    else -> "Verification failed. Please try again"
                }
                Toast.makeText(this@SellerOtpVerifyActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Resend OTP ────────────────────────────────────────────────────────

    private val httpClient = OkHttpClient()
    private val supabaseOtpUrl = "https://plfaugkadavxenpqawzw.supabase.co/auth/v1/otp"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc"

    private fun resendOtp() {
        resendAttempts++
        clearOtpBoxes()
        setLoading(true)

        lifecycleScope.launch {
            try {
                if (authType == "phone") {
                    val code = withContext(Dispatchers.IO) {
                        val body = JSONObject().apply { put("phone", identifier) }.toString()
                        val req = Request.Builder()
                            .url(supabaseOtpUrl)
                            .post(body.toRequestBody("application/json".toMediaType()))
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Content-Type", "application/json")
                            .build()
                        httpClient.newCall(req).execute().code
                    }
                    setLoading(false)
                    if (code == 200 || code == 204) {
                        Toast.makeText(this@SellerOtpVerifyActivity, "OTP resent successfully", Toast.LENGTH_SHORT).show()
                        startResendTimer(30)
                    } else {
                        Toast.makeText(this@SellerOtpVerifyActivity, "Resend failed. Please try again", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                } else {
                    SupabaseClient.auth.signInWith(Email) {
                        email = identifier
                    }
                }
                setLoading(false)
                Toast.makeText(this@SellerOtpVerifyActivity, "OTP resent successfully", Toast.LENGTH_SHORT).show()
                startResendTimer(30)
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@SellerOtpVerifyActivity, "Resend failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    private fun startResendTimer(seconds: Long) {
        tvResend.isEnabled = false
        tvResend.alpha = 0.4f
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResendTimer.text = "Resend OTP in ${millisUntilFinished / 1000}s"
                tvResendTimer.visibility = View.VISIBLE
            }
            override fun onFinish() {
                tvResendTimer.visibility = View.GONE
                if (resendAttempts < maxResendAttempts) {
                    tvResend.isEnabled = true
                    tvResend.alpha = 1f
                }
            }
        }.start()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun clearOtpBoxes() {
        otpBoxes.forEach { it?.setText("") }
        otpBoxes[0]?.requestFocus()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnVerify.isEnabled    = !loading
        btnVerify.text         = if (loading) getString(R.string.loading) else getString(R.string.btn_verify)
        otpBoxes.forEach { it?.isEnabled = !loading }
    }

    private fun maskPhone(phone: String): String {
        val clean = phone.removePrefix("+91").trim()
        return if (clean.length >= 10) "+91 ******${clean.takeLast(4)}" else phone
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name   = parts[0]
        val domain = parts[1]
        val masked = when {
            name.length <= 2 -> name
            name.length <= 4 -> "${name.first()}${"*".repeat(name.length - 1)}"
            else -> "${name.first()}${name[1]}${"*".repeat(name.length - 4)}${name[name.length - 2]}${name.last()}"
        }
        return "$masked@$domain"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
