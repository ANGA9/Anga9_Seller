package com.anga9.seller.ui.profile

import android.content.Intent
import android.net.Uri
import com.anga9.seller.ui.legal.LegalActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Seller Profile Activity
 * Display seller profile information loaded from backend (Phase 3A).
 * Firebase removed - uses ProfileRepository via ProfileViewModel.
 */
class SellerProfileActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var layoutAvatarLetter: FrameLayout
    private lateinit var tvAvatarLetter: TextView
    private lateinit var btnChangePhoto: FrameLayout
    private lateinit var tvBusinessName: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvGstNumber: TextView
    private lateinit var tvPanNumber: TextView
    private lateinit var tvBusinessType: TextView
    private lateinit var tvGarmentCategories: TextView
    private lateinit var tvLocation: TextView
    private lateinit var layoutGarmentSection: android.view.View
    private lateinit var tvSellerBadge: TextView
    private lateinit var tvAccountCreated: TextView
    private lateinit var tvKycStatus: TextView
    private lateinit var btnEditBusiness: LinearLayout
    private lateinit var btnBankDetails: LinearLayout
    private lateinit var btnDeliveryZones: LinearLayout
    private lateinit var btnNotifications: LinearLayout
    private lateinit var btnHelpSupport: LinearLayout
    private lateinit var btnLogout: LinearLayout

    private var currentProfile: SellerProfileResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_profile)
        initViews()
        setupListeners()
        observeViewModel()
        // Load profile using no-arg method (new backend API)
        viewModel.loadProfile()
        // ── Phase 5 (Multi-Brand): detect child brand context ────────────────
        applyChildBrandContext()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        layoutAvatarLetter = findViewById(R.id.layoutAvatarLetter)
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvGstNumber = findViewById(R.id.tvGstNumber)
        tvPanNumber = findViewById(R.id.tvPanNumber)
        tvBusinessType = findViewById(R.id.tvBusinessType)
        tvGarmentCategories = findViewById(R.id.tvGarmentCategories)
        tvLocation = findViewById(R.id.tvLocation)
        layoutGarmentSection = findViewById(R.id.layoutGarmentSection)
        tvSellerBadge = findViewById(R.id.tvSellerBadge)
        tvAccountCreated = findViewById(R.id.tvAccountCreated)
        tvKycStatus = findViewById(R.id.tvKycStatus)
        btnEditBusiness = findViewById(R.id.btnEditBusiness)
        btnBankDetails = findViewById(R.id.btnBankDetails)
        btnDeliveryZones = findViewById(R.id.btnDeliveryZones)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnHelpSupport = findViewById(R.id.btnHelpSupport)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        btnBack.setOnClickListener { finish() }
        btnEditBusiness.setOnClickListener {
            startActivity(Intent(this, EditBusinessActivity::class.java))
        }
        btnBankDetails.setOnClickListener {
            startActivity(Intent(this, BankDetailsActivity::class.java))
        }
        btnDeliveryZones.setOnClickListener {
            startActivity(Intent(this, DeliveryZonesActivity::class.java))
        }
        btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }
        btnHelpSupport.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        // Disputes
        findViewById<android.view.View?>(R.id.btnDisputes)?.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.disputes.SellerDisputesActivity::class.java))
        }
        // Reviews
        findViewById<android.view.View?>(R.id.btnReviews)?.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.reviews.SellerReviewsActivity::class.java))
        }
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
        // Legal — Phase 3 integration
        findViewById<android.view.View?>(R.id.btnPrivacyPolicy)?.setOnClickListener {
            LegalActivity.startPrivacy(this)
        }
        findViewById<android.view.View?>(R.id.btnTermsConditions)?.setOnClickListener {
            LegalActivity.startTerms(this)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        currentProfile = state.data
                        displayProfile(state.data)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        showToast(state.message)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                when (state) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        showToast("Profile updated successfully")
                        viewModel.loadProfile()
        // ── Phase 5 (Multi-Brand): detect child brand context ────────────────
        applyChildBrandContext()
                        viewModel.resetUpdateState()
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        showToast(state.message)
                        viewModel.resetUpdateState()
                    }
                }
            }
        }
    }

    private fun displayProfile(profile: SellerProfileResponse) {
        val businessName = profile.businessName ?: profile.name ?: "Seller"
        val ownerName = profile.ownerName ?: profile.name ?: ""

        tvBusinessName.text = businessName
        tvOwnerName.text = ownerName
        tvAvatarLetter.text = businessName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"

        // Load profile photo if exists
        val photoUrl = profile.avatarUrl
        if (!photoUrl.isNullOrEmpty()) {
            ivProfilePhoto.load(photoUrl) {
                transformations(CircleCropTransformation())
            }
            ivProfilePhoto.visibility = View.VISIBLE
            layoutAvatarLetter.visibility = View.GONE
        } else {
            ivProfilePhoto.visibility = View.GONE
            layoutAvatarLetter.visibility = View.VISIBLE
        }

        tvEmail.text = profile.email ?: "Not provided"
        val rawPhone = profile.phone ?: ""
        tvPhone.text = if (rawPhone.startsWith("+91") && rawPhone.length >= 13) {
            "+91 ${rawPhone.substring(3, 8)} ${rawPhone.substring(8)}"
        } else rawPhone.ifEmpty { "Not provided" }

        tvGstNumber.text = profile.gstNumber ?: "Not provided"
        tvPanNumber.text = "Not provided"  // not in SellerProfileResponse
        tvBusinessType.text = profile.businessType ?: "Not provided"

        val location = listOf(profile.city, profile.state)
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        tvLocation.text = location.ifEmpty { "Not provided" }

        // Garment categories - not in backend model yet
        layoutGarmentSection.visibility = android.view.View.GONE

        // Badge - use kyc_status as badge indicator
        val kycStatus = profile.kycStatus ?: "pending"
        tvSellerBadge.text = kycStatus.uppercase()
        val badgeColor = when (kycStatus.lowercase()) {
            "approved" -> android.graphics.Color.parseColor("#10B981")
            "pending" -> android.graphics.Color.parseColor("#F59E0B")
            "rejected" -> android.graphics.Color.parseColor("#EF4444")
            else -> android.graphics.Color.parseColor("#6B7280")
        }
        tvSellerBadge.setBackgroundColor(badgeColor)

        // Account created
        tvAccountCreated.text = profile.createdAt ?: "Unknown"

        // KYC Status
        when (kycStatus.lowercase()) {
            "approved" -> {
                tvKycStatus.text = "\u2714 Verified"
                tvKycStatus.setTextColor(getColor(R.color.success))
            }
            "pending", "not_submitted" -> {
                tvKycStatus.text = "\u23F3 Pending"
                tvKycStatus.setTextColor(getColor(R.color.warning))
            }
            "rejected" -> {
                tvKycStatus.text = "\u2716 Rejected"
                tvKycStatus.setTextColor(getColor(R.color.error))
            }
            else -> {
                tvKycStatus.text = kycStatus
            }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Photo picker - uploads via Supabase Storage (Phase 3B)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfilePhoto(it) }
    }

    private fun uploadProfilePhoto(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val productRepo = com.anga9.seller.MVVM.data.repository.ProductRepository(applicationContext)
                val sellerId = getSellerId()
                val result = productRepo.uploadProductImage(uri, "profile_$sellerId")
                if (result.isSuccess) {
                    val photoUrl = result.getOrThrow()
                    ivProfilePhoto.load(uri) {
                        transformations(CircleCropTransformation())
                    }
                    ivProfilePhoto.visibility = View.VISIBLE
                    layoutAvatarLetter.visibility = View.GONE
                    // Update profile with new avatar URL
                    viewModel.updateProfile(UpdateSellerProfileRequest(avatarUrl = photoUrl))
                    progressBar.visibility = View.GONE
                    showToast("Profile photo updated")
                } else {
                    progressBar.visibility = View.GONE
                    showToast("Failed to upload photo")
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showToast("Failed to upload photo: ${e.message}")
            }
        }
    }

    // ── Phase 5 (Multi-Brand): Child brand profile restrictions ──────────────
    /**
     * When a child brand is active:
     *  - Shows a blue info banner at the top: "Editing profile for: [Brand Name]"
     *  - Disables phone and email display fields (child brands use parent's contact)
     *  - Shows tooltip on those fields: "Notifications sent to parent account"
     *
     * Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md - Phase 5.2
     */
    private fun applyChildBrandContext() {
        val activeBrandId = TokenManager.getActiveBrandId(this) ?: return
        // Only applies when a child brand is active
        val prefs = getSharedPreferences("anga9_seller_prefs", android.content.Context.MODE_PRIVATE)
        val brandName = prefs.getString("brand_name_", null)
        val label = if (!brandName.isNullOrEmpty()) brandName else "This Brand"

        // 1. Show child brand banner (re-use KYC banner view as info banner)
        //    The KYC banner is already visible infrastructure — we overlay a new message
        //    using a Toast for simplicity (KYC banner has different color requirements)
        android.widget.Toast.makeText(
            this,
            "\uD83C\uDFEA Editing profile for: ",
            android.widget.Toast.LENGTH_LONG
        ).show()

        // 2. Visually mark phone and email as read-only (child brands have no contact)
        //    They are TextViews in this activity (not EditTexts), so we dim them
        //    and set a content description explaining why
        tvPhone.alpha = 0.45f
        tvEmail.alpha = 0.45f
        tvPhone.contentDescription = "Notifications sent to parent account's phone"
        tvEmail.contentDescription = "Notifications sent to parent account's email"

        // 3. Append a note to the displayed values
        if (tvPhone.text.toString().isNotEmpty() &&
            tvPhone.text.toString() != "Not provided") {
            tvPhone.text = "  \u2139 Parent contact"
        }
        if (tvEmail.text.toString().isNotEmpty() &&
            tvEmail.text.toString() != "Not provided") {
            tvEmail.text = "  \u2139 Parent contact"
        }
    }
}