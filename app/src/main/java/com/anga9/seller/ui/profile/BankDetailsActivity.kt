package com.anga9.seller.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.UiState
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Bank Details Activity
 * Manage seller bank account information
 */
class BankDetailsActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var etAccountNumber: TextInputEditText
    private lateinit var etConfirmAccountNumber: TextInputEditText
    private lateinit var etIfscCode: TextInputEditText
    private lateinit var etBankName: TextInputEditText
    private lateinit var etBranchName: TextInputEditText
    private lateinit var spinnerAccountType: Spinner
    private lateinit var btnSave: Button

    // Fixed: Seller? -> SellerProfileResponse?
    private var currentProfile: SellerProfileResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_details)

        initViews()
        setupListeners()
        setupSpinner()
        observeViewModel()
        loadProfile()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        etAccountNumber = findViewById(R.id.etAccountNumber)
        etConfirmAccountNumber = findViewById(R.id.etConfirmAccountNumber)
        etIfscCode = findViewById(R.id.etIfscCode)
        etBankName = findViewById(R.id.etBankName)
        etBranchName = findViewById(R.id.etBranchName)
        spinnerAccountType = findViewById(R.id.spinnerAccountType)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { validateAndSave() }
    }

    private fun setupSpinner() {
        val accountTypes = arrayOf("Savings", "Current")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAccountType.adapter = adapter
    }

    // Fixed: loadProfile() takes no arguments
    private fun loadProfile() {
        viewModel.loadProfile()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        currentProfile = state.data
                        displayBankDetails(state.data)
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnSave.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        showToast("Bank details updated successfully")
                        finish()
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        showToast(state.message)
                    }
                    else -> {}
                }
            }
        }
    }

    // Fixed: accepts SellerProfileResponse (matches ViewModel profileState type)
    private fun displayBankDetails(profile: SellerProfileResponse) {
        etAccountNumber.setText(profile.bankAccountNumber ?: "")
        etConfirmAccountNumber.setText(profile.bankAccountNumber ?: "")
        etIfscCode.setText(profile.bankIfsc ?: "")
        etBankName.setText(profile.bankAccountName ?: "")
        etBranchName.setText("")  // not stored separately in API response
    }

    private fun validateAndSave() {
        val accountNumber = etAccountNumber.text.toString().trim()
        val confirmAccountNumber = etConfirmAccountNumber.text.toString().trim()
        val ifscCode = etIfscCode.text.toString().trim().uppercase()
        val bankName = etBankName.text.toString().trim()
        val branchName = etBranchName.text.toString().trim()

        if (accountNumber.isEmpty()) { etAccountNumber.error = "Required"; return }
        if (accountNumber.length < 9 || accountNumber.length > 18) {
            etAccountNumber.error = "Invalid account number"; return
        }
        if (accountNumber != confirmAccountNumber) {
            etConfirmAccountNumber.error = "Account numbers do not match"; return
        }
        if (ifscCode.isEmpty()) { etIfscCode.error = "Required"; return }
        if (!ifscCode.matches(Regex("""^[A-Z]{4}0[A-Z0-9]{6}$"""))) {
            etIfscCode.error = "Invalid IFSC code"; return
        }
        if (bankName.isEmpty()) { etBankName.error = "Required"; return }
        if (branchName.isEmpty()) { etBranchName.error = "Required"; return }

        // Fixed: use updateProfile() — no separate updateBankDetails() in ViewModel
        val request = UpdateSellerProfileRequest(
            bankAccountNumber = accountNumber,
            bankIfsc = ifscCode,
            bankAccountName = bankName
        )
        viewModel.updateProfile(request)
    }
}