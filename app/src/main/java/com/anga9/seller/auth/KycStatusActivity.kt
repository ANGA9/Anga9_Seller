package com.anga9.seller.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.anga9.seller.data.repository.ProfileRepository
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.Constants

/**
 * Phase 4  KYC Status Screen (redesigned).
 * Handles 3 states: pending, approved, rejected.
 * Realtime Firestore listener auto-navigates on approval.
 * Stepper shows progress: Account Created  KYC Under Review  Start Selling.
 */
class KycStatusActivity : BaseActivity() {

    private val viewModel: AuthViewModel by viewModels()

    // Status views
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusMessage: TextView

    // Stepper
    private lateinit var tvStep2Circle: TextView
    private lateinit var tvStep2Label: TextView
    private lateinit var tvStep3Circle: TextView
    private lateinit var tvStep3Label: TextView
    private lateinit var stepLine2: View

    // Cards
    private lateinit var tvSubmittedInfo: LinearLayout
    private lateinit var layoutRejectionReason: LinearLayout
    private lateinit var tvRejectionReason: TextView

    // Buttons
    private lateinit var btnAction: TextView
    private lateinit var tvLimitedAccess: TextView
    private lateinit var btnLogout: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_status)
        applySystemInsets(findViewById(R.id.rootKycContent))

        initViews()

        val uid = com.anga9.seller.utils.TokenManager.getUserId(this) ?: ""
        if (uid == null) {
            goToLogin()
            return
        }

        // Poll KYC status from backend (replaces Firebase real-time listener)
        lifecycleScope.launch {
            try {
                val profileRepo = com.anga9.seller.data.repository.ProfileRepository(applicationContext)
                val result = profileRepo.getSellerProfile()
                result.fold(
                    onSuccess = { profile ->
                        val status = profile.kycStatus ?: "pending"
                        runOnUiThread { updateUi(status, null) }
                        if (status == Constants.KYC_APPROVED) {
                            startActivity(android.content.Intent(this@KycStatusActivity, DashboardActivity::class.java)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        }
                    },
                    onFailure = { runOnUiThread { updateUi("pending", null) } }
                )
            } catch (e: Exception) {
                runOnUiThread { updateUi("pending", null) }
            }
        }

        btnLogout.setOnClickListener {
            viewModel.signOut()
            goToLogin()
        }
    }

    private fun initViews() {
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvStatusTitle = findViewById(R.id.tvStatusTitle)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvStep2Circle = findViewById(R.id.tvStep2Circle)
        tvStep2Label = findViewById(R.id.tvStep2Label)
        tvStep3Circle = findViewById(R.id.tvStep3Circle)
        tvStep3Label = findViewById(R.id.tvStep3Label)
        stepLine2 = findViewById(R.id.stepLine2)
        tvSubmittedInfo = findViewById(R.id.tvSubmittedInfo)
        layoutRejectionReason = findViewById(R.id.layoutRejectionReason)
        tvRejectionReason = findViewById(R.id.tvRejectionReason)
        btnAction = findViewById(R.id.btnAction)
        tvLimitedAccess = findViewById(R.id.tvLimitedAccess)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun updateUi(status: String, rejectionReason: String?) {
        when (status) {
            Constants.KYC_APPROVED -> showApprovedState()
            Constants.KYC_REJECTED -> showRejectedState(rejectionReason)
            else -> showPendingState()
        }
    }

    private fun showPendingState() {
        // Icon
        ivStatusIcon.setImageResource(R.drawable.ic_kyc_pending)
        ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_orange))

        // Text
        tvStatusTitle.text = getString(R.string.registration_complete)
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        tvStatusMessage.text = getString(R.string.kyc_time_info)

        // Stepper  step 2 active (orange), step 3 inactive
        tvStep2Circle.setBackgroundResource(R.drawable.stepper_circle_active)
        tvStep2Circle.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tvStep2Label.setTextColor(ContextCompat.getColor(this, R.color.warning_orange))
        tvStep3Circle.setBackgroundResource(R.drawable.stepper_circle_inactive)
        tvStep3Circle.setTextColor(ContextCompat.getColor(this, R.color.icon_inactive))
        tvStep3Label.setTextColor(ContextCompat.getColor(this, R.color.icon_inactive))
        stepLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))

        // Cards
        tvSubmittedInfo.visibility = View.VISIBLE
        layoutRejectionReason.visibility = View.GONE

        // Button
        btnAction.text = getString(R.string.explore_dashboard)
        tvLimitedAccess.visibility = View.VISIBLE
        btnAction.setOnClickListener {
            // Allow limited access to dashboard
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }

    private fun showApprovedState() {
        // Icon
        ivStatusIcon.setImageResource(R.drawable.ic_check)
        ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_green))

        // Text
        tvStatusTitle.text = getString(R.string.kyc_approved_title)
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        tvStatusMessage.text = getString(R.string.kyc_approved_msg)

        // Stepper  all 3 steps green
        tvStep2Circle.setBackgroundResource(R.drawable.stepper_circle_active)
        tvStep2Circle.text = ""
        tvStep2Circle.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tvStep2Label.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        tvStep3Circle.setBackgroundResource(R.drawable.stepper_circle_active)
        tvStep3Circle.text = ""
        tvStep3Circle.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tvStep3Label.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        stepLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green))

        // Cards
        tvSubmittedInfo.visibility = View.GONE
        layoutRejectionReason.visibility = View.GONE

        // Button  navigate to full dashboard
        btnAction.text = getString(R.string.start_selling)
        tvLimitedAccess.visibility = View.GONE
        btnAction.setOnClickListener {
            // Save KYC approved status to SharedPrefs so Dashboard knows immediately
            getSharedPreferences(com.anga9.seller.utils.Constants.PREFS_NAME, MODE_PRIVATE)
                .edit().putString("cached_kyc_status", com.anga9.seller.utils.Constants.KYC_APPROVED).apply()
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showRejectedState(rejectionReason: String?) {
        // Icon
        ivStatusIcon.setImageResource(R.drawable.ic_kyc_rejected)
        ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))

        // Text
        tvStatusTitle.text = getString(R.string.kyc_rejected_title)
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        tvStatusMessage.text = "Please re-upload your documents and resubmit."

        // Stepper  step 2 red, step 3 inactive
        tvStep2Circle.setBackgroundResource(R.drawable.stepper_circle_active)
        tvStep2Circle.text = ""
        tvStep2Circle.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tvStep2Label.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        tvStep2Label.text = "KYC\nRejected"
        tvStep3Circle.setBackgroundResource(R.drawable.stepper_circle_inactive)
        tvStep3Circle.setTextColor(ContextCompat.getColor(this, R.color.icon_inactive))
        tvStep3Label.setTextColor(ContextCompat.getColor(this, R.color.icon_inactive))
        stepLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))

        // Cards
        tvSubmittedInfo.visibility = View.GONE
        layoutRejectionReason.visibility = View.VISIBLE
        if (!rejectionReason.isNullOrEmpty()) {
            tvRejectionReason.text = rejectionReason
        } else {
            tvRejectionReason.text = "Documents could not be verified. Please re-upload."
        }

        // Button  go back to KYC step
        btnAction.text = getString(R.string.btn_reupload)
        tvLimitedAccess.visibility = View.GONE
        btnAction.setOnClickListener {
            val reuploadIntent = Intent(this, SellerRegistrationActivity::class.java)
            reuploadIntent.putExtra("start_step", 5)
            reuploadIntent.putExtra("is_reupload", true)
            startActivity(reuploadIntent)
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, SellerPhoneLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Prevent back press  seller must wait for approval or re-upload
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Intentionally blocked
    }
}
