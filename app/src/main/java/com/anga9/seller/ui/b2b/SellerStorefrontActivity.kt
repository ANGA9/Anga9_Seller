package com.anga9.seller.ui.b2b

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import coil.load
import coil.transform.CircleCropTransformation
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.anga9.seller.data_models.SellerStorefront
import com.anga9.seller.utils.UiState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SellerStorefrontActivity : BaseActivity() {

    private val viewModel: B2BViewModel by viewModels()

    private lateinit var ivBanner: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var tvBusinessName: TextView
    private lateinit var tvBadge: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvProductCount: TextView
    private lateinit var etBio: TextInputEditText
    private lateinit var etWhatsapp: TextInputEditText
    private lateinit var etResponseTime: TextInputEditText
    private lateinit var switchEnabled: SwitchMaterial
    private lateinit var chipGroupCerts: ChipGroup
    private lateinit var btnAddCert: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnChangeBanner: ImageButton

    private var currentStorefront: SellerStorefront? = null
    private val certifications = mutableListOf<String>()

    private val bannerPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadBanner(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_storefront)
        initViews()
        setupObservers()
        viewModel.loadStorefront(getSellerId())
    }

    private fun initViews() {
        ivBanner = findViewById(R.id.ivBanner)
        ivProfile = findViewById(R.id.ivProfile)
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvBadge = findViewById(R.id.tvBadge)
        tvCity = findViewById(R.id.tvCity)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        tvProductCount = findViewById(R.id.tvProductCount)
        etBio = findViewById(R.id.etBio)
        etWhatsapp = findViewById(R.id.etWhatsapp)
        etResponseTime = findViewById(R.id.etResponseTime)
        switchEnabled = findViewById(R.id.switchStorefrontEnabled)
        chipGroupCerts = findViewById(R.id.chipGroupCerts)
        btnAddCert = findViewById(R.id.btnAddCert)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        btnChangeBanner = findViewById(R.id.btnChangeBanner)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnChangeBanner.setOnClickListener { bannerPicker.launch("image/*") }
        btnAddCert.setOnClickListener { showAddCertDialog() }
        btnSave.setOnClickListener { saveStorefront() }
    }

    private fun setupObservers() {
        viewModel.storefront.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {}
                is UiState.Loading -> progressBar.visibility = View.VISIBLE
                is UiState.Success<*> -> {
                    progressBar.visibility = View.GONE
                    val sf = state.data as? SellerStorefront ?: return@observe
                    currentStorefront = sf
                    populateUI(sf)
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }

        viewModel.updateState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {}
                is UiState.Loading -> {
                    btnSave.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }
                is UiState.Success<*> -> {
                    btnSave.isEnabled = true
                    progressBar.visibility = View.GONE
                    showToast("Storefront updated successfully")
                }
                is UiState.Error -> {
                    btnSave.isEnabled = true
                    progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }

        viewModel.bannerUpload.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {}
                is UiState.Loading -> progressBar.visibility = View.VISIBLE
                is UiState.Success<*> -> {
                    progressBar.visibility = View.GONE
                    val url = state.data as? String
                    if (url != null) ivBanner.load(url)
                    showToast("Banner updated")
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }
    }

    private fun populateUI(sf: SellerStorefront) {
        tvBusinessName.text = sf.businessName
        tvBadge.text = sf.badgeType.uppercase()
        tvCity.text = "${sf.city}, ${sf.state}"
        tvProductCount.text = "${sf.totalProducts} Products"
        val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
        tvMemberSince.text = "Member since ${sdf.format(java.util.Date(sf.memberSince))}"
        etBio.setText(sf.storefrontBio)
        etWhatsapp.setText(sf.whatsappNumber)
        etResponseTime.setText(sf.responseTimeHours.toString())
        switchEnabled.isChecked = sf.isStorefrontEnabled

        if (!sf.profilePhotoUrl.isNullOrEmpty()) {
            ivProfile.load(sf.profilePhotoUrl) {
                transformations(CircleCropTransformation())
            }
        }
        if (!sf.bannerImageUrl.isNullOrEmpty()) {
            ivBanner.load(sf.bannerImageUrl) { scale(coil.size.Scale.FILL) }
        }

        certifications.clear()
        certifications.addAll(sf.certifications)
        refreshCertChips()
    }

    private fun refreshCertChips() {
        chipGroupCerts.removeAllViews()
        certifications.forEach { cert ->
            val chip = Chip(this).apply {
                text = cert
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    certifications.remove(cert)
                    refreshCertChips()
                }
            }
            chipGroupCerts.addView(chip)
        }
    }

    private fun showAddCertDialog() {
        val certOptions = arrayOf("FSSAI", "ISO 9001", "AGMARK", "Organic India", "Other")
        AlertDialog.Builder(this)
            .setTitle("Add Certification")
            .setItems(certOptions) { _, which ->
                if (which == certOptions.size - 1) {
                    showCustomCertDialog()
                } else {
                    val cert = certOptions[which]
                    if (!certifications.contains(cert)) {
                        certifications.add(cert)
                        refreshCertChips()
                    }
                }
            }.show()
    }

    private fun showCustomCertDialog() {
        val input = EditText(this).apply { hint = "Enter certification name" }
        AlertDialog.Builder(this)
            .setTitle("Custom Certification")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val cert = input.text.toString().trim()
                if (cert.isNotEmpty() && !certifications.contains(cert)) {
                    certifications.add(cert)
                    refreshCertChips()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveStorefront() {
        val bio = etBio.text.toString().trim()
        val whatsapp = etWhatsapp.text.toString().trim()
        val responseTime = etResponseTime.text.toString().trim().toIntOrNull() ?: 24
        val updates = mapOf(
            "storefrontBio" to bio,
            "whatsappNumber" to whatsapp,
            "responseTimeHours" to responseTime,
            "isStorefrontEnabled" to switchEnabled.isChecked,
            "certifications" to certifications
        )
        viewModel.updateStorefront(getSellerId(), updates)
    }

    private fun uploadBanner(uri: Uri) {
        val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return
        viewModel.uploadBanner(getSellerId(), bytes)
    }
}