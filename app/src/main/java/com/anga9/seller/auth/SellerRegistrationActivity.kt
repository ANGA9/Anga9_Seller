package com.anga9.seller.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.utils.UiState
import com.anga9.seller.utils.TokenManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import android.text.Editable
import android.text.TextWatcher

class SellerRegistrationActivity : BaseActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var layoutStep1: View
    private lateinit var layoutStep2: View
    private lateinit var layoutStep3: View
    private lateinit var layoutStep4: View
    private lateinit var layoutStep5: View
    private lateinit var layoutStep6: View
    private lateinit var layoutStep7: View

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStepLabel: TextView

    // Step 1
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var cbTermsAgree: CheckBox

    // Step 2
    private lateinit var etBusinessName: EditText
    private lateinit var spinnerBusinessType: Spinner
    private lateinit var etBusinessCategory: EditText
    private lateinit var etStoreDescription: EditText

    // Step 3
    private lateinit var etAddress1: EditText
    private lateinit var etAddress2: EditText
    private lateinit var etCity: EditText
    private lateinit var spinnerState: Spinner
    private lateinit var etPincode: EditText

    // Step 4
    private lateinit var etGstin: EditText
    // private lateinit var btnUploadGst: Button
    // private lateinit var tvGstFileName: TextView
    private lateinit var etPan: EditText
    // private lateinit var btnUploadPan: Button
    // private lateinit var tvPanFileName: TextView
    private lateinit var etAadhaar: EditText
    // private lateinit var btnUploadAadhaar: Button
    // private lateinit var tvAadhaarFileName: TextView

    // Step 5
    private lateinit var etBankName: EditText
    private lateinit var etAccountNum: EditText
    private lateinit var etConfirmAccountNum: EditText
    private lateinit var etIfsc: EditText
    private lateinit var etBankBankName: EditText
    private lateinit var etBankBranch: EditText

    // Step 6
    private lateinit var cbSameAsBusiness: CheckBox
    private lateinit var layoutCustomPickup: View
    private lateinit var etPickupAddress: EditText

    // Step 7
    private lateinit var tvReviewDetails: TextView

    private lateinit var btnBack: Button
    private lateinit var btnNext: Button
    private lateinit var loadingOverlay: View

    private var currentStep = 1
    private val totalSteps = 7

    private var currentUploadTarget = ""
    private var gstUri: Uri? = null
    private var panUri: Uri? = null
    private var aadhaarUri: Uri? = null

    private val pickDocLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { handleDocumentPicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_registration)
        initViews()
        setupSpinners()
        setupListeners()
        
        // Auto-fill and lock phone number
        val savedPhone = TokenManager.getUserPhone(this)
        if (!savedPhone.isNullOrBlank()) {
            etPhone.setText(savedPhone)
        }
        etPhone.isEnabled = false
        
        setupObservers()
        showStep(1)
    }

    private fun setupObservers() {
        viewModel.fullRegistrationState.observe(this) { state ->
            when (state) {
                is com.anga9.seller.utils.UiState.Idle -> { }
                is com.anga9.seller.utils.UiState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    btnNext.isEnabled = false
                }
                is com.anga9.seller.utils.UiState.Success -> {
                    loadingOverlay.visibility = View.GONE
                    btnNext.isEnabled = true
                    Toast.makeText(this, "Registration Submitted Successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, KycStatusActivity::class.java))
                    finishAffinity()
                }
                is com.anga9.seller.utils.UiState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    btnNext.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initViews() {
        layoutStep1 = findViewById(R.id.layoutStep1)
        layoutStep2 = findViewById(R.id.layoutStep2)
        layoutStep3 = findViewById(R.id.layoutStep3)
        layoutStep4 = findViewById(R.id.layoutStep4)
        layoutStep5 = findViewById(R.id.layoutStep5)
        layoutStep6 = findViewById(R.id.layoutStep6)
        layoutStep7 = findViewById(R.id.layoutStep7)

        progressBar = findViewById(R.id.stepProgressBar)
        tvStepLabel = findViewById(R.id.tvStepLabel)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        cbTermsAgree = findViewById(R.id.cbTermsAgree)

        etBusinessName = findViewById(R.id.etBusinessName)
        spinnerBusinessType = findViewById(R.id.spinnerBusinessType)
        etBusinessCategory = findViewById(R.id.etBusinessCategory)
        etStoreDescription = findViewById(R.id.etStoreDescription)

        etAddress1 = findViewById(R.id.etAddress1)
        etAddress2 = findViewById(R.id.etAddress2)
        etCity = findViewById(R.id.etCity)
        spinnerState = findViewById(R.id.spinnerState)
        etPincode = findViewById(R.id.etPincode)

        etGstin = findViewById(R.id.etGstin)
        // btnUploadGst = findViewById(R.id.btnUploadGst)
        // tvGstFileName = findViewById(R.id.tvGstFileName)
        etPan = findViewById(R.id.etPan)
        // btnUploadPan = findViewById(R.id.btnUploadPan)
        // tvPanFileName = findViewById(R.id.tvPanFileName)
        etAadhaar = findViewById(R.id.etAadhaar)
        // btnUploadAadhaar = findViewById(R.id.btnUploadAadhaar)
        // tvAadhaarFileName = findViewById(R.id.tvAadhaarFileName)

        etBankName = findViewById(R.id.etBankName)
        etAccountNum = findViewById(R.id.etAccountNum)
        etConfirmAccountNum = findViewById(R.id.etConfirmAccountNum)
        etIfsc = findViewById(R.id.etIfsc)
        etBankBankName = findViewById(R.id.etBankBankName)
        etBankBranch = findViewById(R.id.etBankBranch)

        cbSameAsBusiness = findViewById(R.id.cbSameAsBusiness)
        layoutCustomPickup = findViewById(R.id.layoutCustomPickup)
        etPickupAddress = findViewById(R.id.etPickupAddress)

        tvReviewDetails = findViewById(R.id.tvReviewDetails)

        btnBack = findViewById(R.id.btnBack)
        btnNext = findViewById(R.id.btnNext)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupSpinners() {
        val businessTypes = arrayOf("Select business type", "Individual", "Proprietorship", "Partnership", "Pvt Ltd", "LLP", "Other")
        spinnerBusinessType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, businessTypes)

        val states = arrayOf("Select state", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal", "Delhi")
        spinnerState.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, states)
    }

    private fun setupListeners() {
        btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < totalSteps) {
                    currentStep++
                    showStep(currentStep)
                } else {
                    submitRegistration()
                }
            }
        }

        btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                showStep(currentStep)
            }
        }

        cbSameAsBusiness.setOnCheckedChangeListener { _, isChecked ->
            layoutCustomPickup.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // btnUploadGst.setOnClickListener {
        //     currentUploadTarget = "GST"
        //     pickDocLauncher.launch("*/*")
        // }
        // btnUploadPan.setOnClickListener {
        //     currentUploadTarget = "PAN"
        //     pickDocLauncher.launch("*/*")
        // }
        // btnUploadAadhaar.setOnClickListener {
        //     currentUploadTarget = "AADHAAR"
        //     pickDocLauncher.launch("*/*")
        // }

        etPincode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    fetchPincodeDetails(s.toString())
                }
            }
        })
    }

    private fun fetchPincodeDetails(pincode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.postalpincode.in/pincode/$pincode"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val status = jsonArray.getJSONObject(0).getString("Status")
                if (status == "Success") {
                    val postOffices = jsonArray.getJSONObject(0).getJSONArray("PostOffice")
                    val firstOffice = postOffices.getJSONObject(0)
                    val district = firstOffice.getString("District")
                    val state = firstOffice.getString("State")

                    withContext(Dispatchers.Main) {
                        etCity.setText(district)
                        
                        // Select state in spinner
                        val adapter = spinnerState.adapter
                        for (i in 0 until adapter.count) {
                            if (adapter.getItem(i).toString().equals(state, ignoreCase = true)) {
                                spinnerState.setSelection(i)
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore failure and let user type manually
                e.printStackTrace()
            }
        }
    }

    private fun handleDocumentPicked(uri: Uri) {
        // when (currentUploadTarget) {
        //     "GST" -> {
        //         gstUri = uri
        //         tvGstFileName.visibility = View.VISIBLE
        //         tvGstFileName.text = "✓ GST Document Selected"
        //     }
        //     "PAN" -> {
        //         panUri = uri
        //         tvPanFileName.visibility = View.VISIBLE
        //         tvPanFileName.text = "✓ PAN Document Selected"
        //     }
        //     "AADHAAR" -> {
        //         aadhaarUri = uri
        //         tvAadhaarFileName.visibility = View.VISIBLE
        //         tvAadhaarFileName.text = "✓ Aadhaar Document Selected"
        //     }
        // }
    }

    private fun showStep(step: Int) {
        layoutStep1.visibility = View.GONE
        layoutStep2.visibility = View.GONE
        layoutStep3.visibility = View.GONE
        layoutStep4.visibility = View.GONE
        layoutStep5.visibility = View.GONE
        layoutStep6.visibility = View.GONE
        layoutStep7.visibility = View.GONE

        btnBack.visibility = if (step > 1) View.VISIBLE else View.GONE
        btnNext.text = if (step == totalSteps) "Submit" else "Next"
        
        tvStepLabel.text = "Step $step of $totalSteps"
        progressBar.progress = step

        when (step) {
            1 -> layoutStep1.visibility = View.VISIBLE
            2 -> layoutStep2.visibility = View.VISIBLE
            3 -> layoutStep3.visibility = View.VISIBLE
            4 -> layoutStep4.visibility = View.VISIBLE
            5 -> layoutStep5.visibility = View.VISIBLE
            6 -> layoutStep6.visibility = View.VISIBLE
            7 -> {
                layoutStep7.visibility = View.VISIBLE
                updateReviewSummary()
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        // Add basic validations
        when (currentStep) {
            1 -> {
                if (etFullName.text.isBlank()) return showError(etFullName, "Required")
                if (etEmail.text.isBlank()) return showError(etEmail, "Required")
                if (!cbTermsAgree.isChecked) {
                    Toast.makeText(this, "Please agree to terms", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            2 -> {
                if (etBusinessName.text.isBlank()) return showError(etBusinessName, "Required")
                if (spinnerBusinessType.selectedItemPosition == 0) {
                    Toast.makeText(this, "Select Business Type", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            3 -> {
                if (etAddress1.text.isBlank()) return showError(etAddress1, "Required")
                if (etCity.text.isBlank()) return showError(etCity, "Required")
                if (spinnerState.selectedItemPosition == 0) {
                    Toast.makeText(this, "Select State", Toast.LENGTH_SHORT).show()
                    return false
                }
                if (etPincode.text.length != 6) return showError(etPincode, "Invalid Pincode")
            }
            5 -> {
                if (etBankName.text.isBlank()) return showError(etBankName, "Required")
                if (etAccountNum.text.isBlank()) return showError(etAccountNum, "Required")
                if (etAccountNum.text.toString() != etConfirmAccountNum.text.toString()) {
                    return showError(etConfirmAccountNum, "Mismatch")
                }
                if (etIfsc.text.isBlank()) return showError(etIfsc, "Required")
            }
            6 -> {
                if (!cbSameAsBusiness.isChecked && etPickupAddress.text.isBlank()) {
                    return showError(etPickupAddress, "Required")
                }
            }
        }
        return true
    }

    private fun showError(et: EditText, msg: String): Boolean {
        et.error = msg
        et.requestFocus()
        return false
    }

    private fun updateReviewSummary() {
        val summary = """
            Name: ${etFullName.text}
            Email: ${etEmail.text}
            
            Business: ${etBusinessName.text}
            Type: ${spinnerBusinessType.selectedItem}
            
            Address: ${etAddress1.text}, ${etCity.text}, ${etPincode.text}
            
            GSTIN: ${etGstin.text.ifBlank { "N/A" }}
            PAN: ${etPan.text.ifBlank { "N/A" }}
            
            Bank: ${etBankName.text}, IFSC: ${etIfsc.text}
        """.trimIndent()
        tvReviewDetails.text = summary
    }

    private fun submitRegistration() {
        val selectedType = spinnerBusinessType.selectedItem.toString()
        val mappedType = when (selectedType) {
            "Individual" -> "individual"
            "Proprietorship" -> "proprietorship"
            "Partnership" -> "partnership"
            "Pvt Ltd" -> "pvt_ltd"
            "LLP" -> "llp"
            "Other" -> "other"
            else -> ""
        }

        val request = com.anga9.seller.network.model.UpdateSellerProfileRequest(
            ownerName = etFullName.text.toString(),
            name = etFullName.text.toString(),
            businessName = etBusinessName.text.toString(),
            businessType = mappedType,
            businessCategory = etBusinessCategory.text.toString(),
            storeDescription = etStoreDescription.text.toString(),
            addressLine1 = etAddress1.text.toString(),
            addressLine2 = etAddress2.text.toString(),
            city = etCity.text.toString(),
            state = spinnerState.selectedItem.toString(),
            pincode = etPincode.text.toString(),
            gstin = etGstin.text.toString(),
            panNumber = etPan.text.toString(),
            aadhaarNumber = etAadhaar.text.toString(),
            bankAccountName = etBankName.text.toString(),
            bankAccountNumber = etAccountNum.text.toString(),
            bankIfsc = etIfsc.text.toString(),
            bankName = etBankBankName.text.toString(),
            bankBranch = etBankBranch.text.toString(),
            pickupAddressSame = cbSameAsBusiness.isChecked,
            pickupAddress = etPickupAddress.text.toString()
        )
        viewModel.submitFullRegistration(request)
    }
}
