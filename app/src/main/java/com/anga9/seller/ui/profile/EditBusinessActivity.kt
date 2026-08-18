package com.anga9.seller.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.UpdateSellerProfileRequest
import com.anga9.seller.utils.UiState
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Edit Business Info - Full screen activity
 * Professional form with sections: Basic Info, About, Address
 */
class EditBusinessActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var progressBar: ProgressBar

    // Basic Info
    private lateinit var etBusinessName: TextInputEditText
    private lateinit var etOwnerName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etBusinessType: TextInputEditText

    // About (extra fields - stored locally, not in backend model)
    private lateinit var etBusinessDescription: TextInputEditText
    private lateinit var etBusinessCategory: TextInputEditText
    private lateinit var etYearEstablished: TextInputEditText

    // Address
    private lateinit var etAddress: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etPinCode: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_business)
        initViews()
        setupListeners()
        observeViewModel()
        // Load profile using no-arg method (new backend API)
        viewModel.loadProfile()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        etBusinessName = findViewById(R.id.etBusinessName)
        etOwnerName = findViewById(R.id.etOwnerName)
        etPhone = findViewById(R.id.etPhone)
        etBusinessType = findViewById(R.id.etBusinessType)
        etBusinessDescription = findViewById(R.id.etBusinessDescription)
        etBusinessCategory = findViewById(R.id.etBusinessCategory)
        etYearEstablished = findViewById(R.id.etYearEstablished)
        etAddress = findViewById(R.id.etAddress)
        etCity = findViewById(R.id.etCity)
        etState = findViewById(R.id.etState)
        etPinCode = findViewById(R.id.etPinCode)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { validateAndSave() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                if (state is UiState.Success) {
                    val profile = state.data
                    // Map SellerProfileResponse fields to form fields
                    etBusinessName.setText(profile.businessName ?: "")
                    etOwnerName.setText(profile.ownerName ?: "")
                    etPhone.setText((profile.phone ?: "").removePrefix("+91"))
                    etBusinessType.setText(profile.businessType ?: "")
                    // Fields not in backend model - leave empty or use city/state/pincode
                    etBusinessDescription.setText("")  // not in SellerProfileResponse
                    etBusinessCategory.setText("")     // not in SellerProfileResponse
                    etYearEstablished.setText("")      // not in SellerProfileResponse
                    etAddress.setText("")              // not in SellerProfileResponse
                    etCity.setText(profile.city ?: "")
                    etState.setText(profile.state ?: "")
                    etPinCode.setText(profile.pincode ?: "")
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
                        showToast("Business info updated")
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

    private fun validateAndSave() {
        val businessName = etBusinessName.text.toString().trim()
        val ownerName = etOwnerName.text.toString().trim()

        if (businessName.isEmpty()) {
            etBusinessName.error = "Required"
            return
        }
        if (ownerName.isEmpty()) {
            etOwnerName.error = "Required"
            return
        }

        val city = etCity.text.toString().trim()
        val state = etState.text.toString().trim()
        val pincode = etPinCode.text.toString().trim()

        // Build UpdateSellerProfileRequest with available fields
        val request = UpdateSellerProfileRequest(
            name = ownerName,
            businessName = businessName,
            businessType = etBusinessType.text.toString().trim().ifEmpty { null },
            ownerName = ownerName,
            city = city.ifEmpty { null },
            state = state.ifEmpty { null },
            pincode = pincode.ifEmpty { null }
        )

        viewModel.updateProfile(request)
    }
}