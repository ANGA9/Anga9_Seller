package com.anga9.seller.ui.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.auth.SellerPhoneLoginActivity
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.utils.UiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Business Profile Activity — Seller App (Matching Web Seller Portal)
 *
 * Connected to live backend endpoints:
 *   GET   /api/users/seller-profile
 *   PATCH /api/users/seller-profile
 */
class SellerProfileActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    // Header & Loading
    private lateinit var btnBack: ImageView
    private lateinit var layoutStatusPill: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusText: TextView
    private lateinit var progressBar: ProgressBar

    // Section 1: Store Identity
    private lateinit var etStoreName: EditText
    private lateinit var etBusinessName: EditText
    private lateinit var etBusinessType: EditText
    private lateinit var etBusinessCategory: EditText
    private lateinit var tvDescCounter: TextView
    private lateinit var etStoreDescription: EditText

    // Section 2: Identity Documents (KYC)
    private lateinit var etGstin: EditText
    private lateinit var etPan: EditText

    // Section 3: Registered Address
    private lateinit var etAddressLine1: EditText
    private lateinit var etCity: EditText
    private lateinit var etState: EditText
    private lateinit var etPincode: EditText

    // Section 4: Bank Account
    private lateinit var etAccountHolderName: EditText
    private lateinit var layoutAccountHolderWarning: LinearLayout
    private lateinit var etAccountNumber: EditText
    private lateinit var etIfscCode: EditText
    private lateinit var etBankName: EditText
    private lateinit var etBankBranch: EditText

    // Actions
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var pbSaveLoading: ProgressBar
    private lateinit var btnLogout: MaterialButton

    private var currentVerificationStatus = "unverified"
    private var ifscLookupJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_profile)

        initViews()
        setupListeners()
        observeViewModel()

        viewModel.loadProfile()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        layoutStatusPill = findViewById(R.id.layoutStatusPill)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvStatusText = findViewById(R.id.tvStatusText)
        progressBar = findViewById(R.id.progressBar)

        etStoreName = findViewById(R.id.etStoreName)
        etBusinessName = findViewById(R.id.etBusinessName)
        etBusinessType = findViewById(R.id.etBusinessType)
        etBusinessCategory = findViewById(R.id.etBusinessCategory)
        tvDescCounter = findViewById(R.id.tvDescCounter)
        etStoreDescription = findViewById(R.id.etStoreDescription)

        etGstin = findViewById(R.id.etGstin)
        etPan = findViewById(R.id.etPan)

        etAddressLine1 = findViewById(R.id.etAddressLine1)
        etCity = findViewById(R.id.etCity)
        etState = findViewById(R.id.etState)
        etPincode = findViewById(R.id.etPincode)

        etAccountHolderName = findViewById(R.id.etAccountHolderName)
        layoutAccountHolderWarning = findViewById(R.id.layoutAccountHolderWarning)
        etAccountNumber = findViewById(R.id.etAccountNumber)
        etIfscCode = findViewById(R.id.etIfscCode)
        etBankName = findViewById(R.id.etBankName)
        etBankBranch = findViewById(R.id.etBankBranch)

        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        pbSaveLoading = findViewById(R.id.pbSaveLoading)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Store Description Counter
        etStoreDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvDescCounter.text = "$len / 1000"
                if (len > 900) {
                    tvDescCounter.setTextColor(Color.parseColor("#D97706"))
                } else {
                    tvDescCounter.setTextColor(Color.parseColor("#9AA1AC"))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Account Holder Name Dynamic Warning styling
        etAccountHolderName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAccountHolderStyling(s.isNullOrBlank())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // IFSC dynamic lookup with debounce
        etIfscCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val code = s?.toString()?.trim()?.uppercase() ?: ""
                if (code.length == 11) {
                    ifscLookupJob?.cancel()
                    ifscLookupJob = lifecycleScope.launch {
                        delay(400)
                        viewModel.lookupIfsc(code)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Verification Pill info dialog
        layoutStatusPill.setOnClickListener {
            showVerificationInfoDialog()
        }

        // Save Changes
        btnSaveChanges.setOnClickListener {
            handleSave()
        }

        // Logout of Store
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun updateAccountHolderStyling(isEmpty: Boolean) {
        if (isEmpty) {
            etAccountHolderName.setBackgroundResource(R.drawable.bg_input_warning)
            layoutAccountHolderWarning.visibility = View.VISIBLE
        } else {
            etAccountHolderName.setBackgroundResource(R.drawable.bg_input_white)
            layoutAccountHolderWarning.visibility = View.GONE
        }
    }

    private fun maskValue(valStr: String?): String {
        if (valStr.isNullOrEmpty() || valStr.length < 4) return valStr ?: "—"
        return valStr.take(2) + "****" + valStr.takeLast(2)
    }

    private fun observeViewModel() {
        // 1. Profile Load State
        lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        state.data?.let { populateProfileData(it) }
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@SellerProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        // 2. IFSC Lookup State
        lifecycleScope.launch {
            viewModel.ifscState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        etBankName.setText("Fetching...")
                        etBankBranch.setText("Fetching...")
                    }
                    is UiState.Success -> {
                        state.data?.let { (bank, branch) ->
                            etBankName.setText(bank)
                            etBankBranch.setText(branch)
                        }
                    }
                    is UiState.Error -> {
                        etBankName.setText("Invalid IFSC")
                        etBankBranch.setText("")
                    }
                    else -> {}
                }
            }
        }

        // 3. Save Update State
        lifecycleScope.launch {
            viewModel.updateState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        setSaveLoading(true)
                    }
                    is UiState.Success -> {
                        setSaveLoading(false)
                        Toast.makeText(this@SellerProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateState()
                    }
                    is UiState.Error -> {
                        setSaveLoading(false)
                        Toast.makeText(this@SellerProfileActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetUpdateState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun populateProfileData(profile: SellerProfileResponse) {
        // Section 1: Store Identity
        etStoreName.setText(profile.storeName ?: "")
        etBusinessName.setText(profile.businessName ?: "")
        etBusinessType.setText(profile.businessType ?: "")
        etBusinessCategory.setText(profile.businessCategory ?: "")
        etStoreDescription.setText(profile.storeDescription ?: "")
        val descLen = profile.storeDescription?.length ?: 0
        tvDescCounter.text = "$descLen / 1000"

        // Section 2: Identity Documents (KYC)
        val gstinVal = profile.gstin ?: profile.gstNumber
        etGstin.setText(maskValue(gstinVal))
        etPan.setText(maskValue(profile.panNumber))

        // Section 3: Registered Address
        etAddressLine1.setText(profile.addressLine1 ?: "")
        etCity.setText(profile.city ?: "")
        etState.setText(profile.state ?: "")
        etPincode.setText(profile.pincode ?: "")

        // Section 4: Bank Account
        val holderName = profile.bankAccountName ?: ""
        etAccountHolderName.setText(holderName)
        updateAccountHolderStyling(holderName.isBlank())

        etAccountNumber.setText(profile.bankAccountNumber ?: "")
        etIfscCode.setText(profile.bankIfsc ?: "")
        etBankName.setText(profile.bankName ?: "Fetched from IFSC")
        etBankBranch.setText(profile.bankBranch ?: "Fetched from IFSC")

        // Verification Status
        currentVerificationStatus = profile.kycStatus ?: "unverified"
        updateVerificationPill(currentVerificationStatus)
    }

    private fun updateVerificationPill(status: String) {
        val bg = GradientDrawable()
        bg.cornerRadius = 24f

        when (status.lowercase()) {
            "verified" -> {
                bg.setColor(Color.parseColor("#F0FBF4"))
                layoutStatusPill.background = bg
                ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                ivStatusIcon.setColorFilter(Color.parseColor("#1E7A45"))
                tvStatusText.text = "Verified"
                tvStatusText.setTextColor(Color.parseColor("#1E7A45"))
            }
            "pending", "pending_review" -> {
                bg.setColor(Color.parseColor("#FFFBEB"))
                layoutStatusPill.background = bg
                ivStatusIcon.setImageResource(R.drawable.ic_clock)
                ivStatusIcon.setColorFilter(Color.parseColor("#D97706"))
                tvStatusText.text = "Pending Review"
                tvStatusText.setTextColor(Color.parseColor("#D97706"))
            }
            "rejected" -> {
                bg.setColor(Color.parseColor("#FEF2F2"))
                layoutStatusPill.background = bg
                ivStatusIcon.setImageResource(R.drawable.ic_cancel)
                ivStatusIcon.setColorFilter(Color.parseColor("#DC2626"))
                tvStatusText.text = "Rejected"
                tvStatusText.setTextColor(Color.parseColor("#DC2626"))
            }
            else -> {
                bg.setColor(Color.parseColor("#F3F4F6"))
                layoutStatusPill.background = bg
                ivStatusIcon.setImageResource(R.drawable.ic_clock)
                ivStatusIcon.setColorFilter(Color.parseColor("#6B7280"))
                tvStatusText.text = "Unverified"
                tvStatusText.setTextColor(Color.parseColor("#6B7280"))
            }
        }
    }

    private fun showVerificationInfoDialog() {
        val message = when (currentVerificationStatus.lowercase()) {
            "verified" -> "Your business legal identity and bank account details have been successfully verified by the compliance team."
            "pending", "pending_review" -> "Your KYC documents and business entity details are currently under review by our compliance team."
            "rejected" -> "Your business verification was rejected. Please contact seller support to update your KYC documents."
            else -> "Your business profile is unverified. Ensure your KYC documents and bank details match to complete verification."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Verification Status: ${tvStatusText.text}")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun handleSave() {
        val storeName = etStoreName.text.toString().trim()
        val businessName = etBusinessName.text.toString().trim()
        val businessType = etBusinessType.text.toString().trim()
        val businessCategory = etBusinessCategory.text.toString().trim()
        val storeDescription = etStoreDescription.text.toString().trim()

        val addressLine1 = etAddressLine1.text.toString().trim()
        val city = etCity.text.toString().trim()
        val state = etState.text.toString().trim()
        val pincode = etPincode.text.toString().trim()

        val bankAccountName = etAccountHolderName.text.toString().trim()
        val bankAccountNumber = etAccountNumber.text.toString().trim()
        val bankIfsc = etIfscCode.text.toString().trim().uppercase()
        val bankName = etBankName.text.toString().trim().let { if (it == "Fetched from IFSC" || it == "Invalid IFSC") null else it }
        val bankBranch = etBankBranch.text.toString().trim().let { if (it == "Fetched from IFSC" || it == "Invalid IFSC") null else it }

        // Validation: If bank details are partially filled, Account Holder Name is mandatory
        if ((bankAccountNumber.isNotEmpty() || bankIfsc.isNotEmpty()) && bankAccountName.isEmpty()) {
            etAccountHolderName.requestFocus()
            Toast.makeText(this, "Account Holder Name is required to save bank details", Toast.LENGTH_SHORT).show()
            updateAccountHolderStyling(true)
            return
        }

        val request = UpdateSellerProfileRequest(
            storeName = storeName.ifEmpty { null },
            businessName = businessName.ifEmpty { null },
            businessType = businessType.ifEmpty { null },
            businessCategory = businessCategory.ifEmpty { null },
            storeDescription = storeDescription.ifEmpty { null },
            addressLine1 = addressLine1.ifEmpty { null },
            city = city.ifEmpty { null },
            state = state.ifEmpty { null },
            pincode = pincode.ifEmpty { null },
            bankAccountName = bankAccountName.ifEmpty { null },
            bankAccountNumber = bankAccountNumber.ifEmpty { null },
            bankIfsc = bankIfsc.ifEmpty { null },
            bankName = bankName,
            bankBranch = bankBranch
        )

        viewModel.updateProfile(request)
    }

    private fun setSaveLoading(loading: Boolean) {
        if (loading) {
            btnSaveChanges.isEnabled = false
            btnSaveChanges.text = ""
            pbSaveLoading.visibility = View.VISIBLE
        } else {
            btnSaveChanges.isEnabled = true
            btnSaveChanges.text = "Save changes"
            pbSaveLoading.visibility = View.GONE
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Log out of store")
            .setMessage("Are you sure you want to log out of your seller account?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ ->
                TokenManager.clearAll(this)
                val intent = Intent(this, SellerPhoneLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }
}