package com.anga9.seller.ui.wallet

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.ui.profile.ProfileViewModel
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch

/**
 * BankDetailsActivity (wallet package)
 * Uses ProfileViewModel + PATCH /api/users/seller-profile
 */
class BankDetailsActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var etAccountNumber: EditText
    private lateinit var etConfirmAccountNumber: EditText
    private lateinit var etHolderName: EditText
    private lateinit var etIfsc: EditText
    private lateinit var btnSaveBank: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVerificationNote: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_bank_details)
        initViews()
        setupObservers()
        viewModel.loadProfile()
    }

    private fun initViews() {
        etAccountNumber        = findViewById(R.id.etAccountNumber)
        etConfirmAccountNumber = findViewById(R.id.etConfirmAccountNumber)
        etHolderName           = findViewById(R.id.etHolderName)
        etIfsc                 = findViewById(R.id.etIfsc)
        btnSaveBank            = findViewById(R.id.btnSaveBank)
        progressBar            = findViewById(R.id.progressBar)
        tvVerificationNote     = findViewById(R.id.tvVerificationNote)

        // Hide fields not supported by new API
        try { findViewById<View>(R.id.etBankName)?.visibility        = View.GONE } catch (_: Exception) {}
        try { findViewById<View>(R.id.etBranch)?.visibility          = View.GONE } catch (_: Exception) {}
        try { findViewById<View>(R.id.spinnerAccountType)?.visibility = View.GONE } catch (_: Exception) {}
        try { findViewById<View>(R.id.etUpiId)?.visibility            = View.GONE } catch (_: Exception) {}
        try { findViewById<View>(R.id.btnSaveUpi)?.visibility         = View.GONE } catch (_: Exception) {}

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        btnSaveBank.setOnClickListener { saveBankDetails() }
    }

    private fun setupObservers() {
        // Load existing bank details
        lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        val p = state.data
                        etAccountNumber.setText(p.bankAccountNumber ?: "")
                        etConfirmAccountNumber.setText(p.bankAccountNumber ?: "")
                        etHolderName.setText(p.bankAccountName ?: "")
                        etIfsc.setText(p.bankIfsc ?: "")
                        tvVerificationNote.text = "Bank details pending admin verification after update."
                    }
                    else -> {}
                }
            }
        }

        // Observe update result
        lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnSaveBank.isEnabled  = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        btnSaveBank.isEnabled  = true
                        Toast.makeText(this@BankDetailsActivity, "Saved. Pending admin verification.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnSaveBank.isEnabled  = true
                        Toast.makeText(this@BankDetailsActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun saveBankDetails() {
        val accountNumber  = etAccountNumber.text.toString().trim()
        val confirmAccount = etConfirmAccountNumber.text.toString().trim()
        val holderName     = etHolderName.text.toString().trim()
        val ifsc           = etIfsc.text.toString().trim().uppercase()

        if (accountNumber.isEmpty() || holderName.isEmpty() || ifsc.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show(); return
        }
        if (accountNumber != confirmAccount) {
            Toast.makeText(this, "Account numbers do not match", Toast.LENGTH_SHORT).show(); return
        }
        if (accountNumber.length < 9 || accountNumber.length > 18) {
            Toast.makeText(this, "Invalid account number (9-18 digits)", Toast.LENGTH_SHORT).show(); return
        }
        if (!ifsc.matches(Regex("""^[A-Z]{4}0[A-Z0-9]{6}$"""))) {
            Toast.makeText(this, "Invalid IFSC code (e.g. SBIN0001234)", Toast.LENGTH_SHORT).show(); return
        }

        val request = UpdateSellerProfileRequest(
            bankAccountNumber = accountNumber,
            bankIfsc          = ifsc,
            bankAccountName   = holderName
        )
        viewModel.updateProfile(request)
    }
}