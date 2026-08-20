package com.anga9.seller.ui.ads

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateAdActivity : AppCompatActivity() {

    private val viewModel: CreateAdViewModel by viewModels()

    // Views
    private lateinit var btnBack: View
    
    // Basics
    private lateinit var btnSelectProduct: View
    private lateinit var tvSelectedProduct: TextView
    private lateinit var tvProductError: TextView
    
    private lateinit var btnSelectPlacement: View
    private lateinit var tvSelectedPlacement: TextView
    private lateinit var tvPlacementError: TextView

    // Creative
    private lateinit var btnUploadBanner: View
    private lateinit var ivBannerThumbnail: ImageView
    private lateinit var tvUploadText: TextView
    private lateinit var ivUploadIcon: ImageView
    private lateinit var pbUpload: ProgressBar
    private lateinit var tvImageError: TextView
    
    private lateinit var etHeadline: EditText
    private lateinit var tvHeadlineError: TextView
    
    private lateinit var btnSelectCta: View
    private lateinit var tvSelectedCta: TextView
    private lateinit var tvCtaError: TextView

    // Live Preview
    private lateinit var ivPreviewBgPlaceholder: ImageView
    private lateinit var ivPreviewBgImage: ImageView
    private lateinit var tvPreviewHeadline: TextView
    private lateinit var tvPreviewCta: TextView
    private lateinit var tvPreviewCaption: TextView

    // Schedule & Budget
    private lateinit var btnStartDate: View
    private lateinit var tvStartDate: TextView
    private lateinit var tvStartDateError: TextView
    
    private lateinit var btnEndDate: View
    private lateinit var tvEndDate: TextView
    private lateinit var tvEndDateError: TextView
    private lateinit var tvDurationInfo: TextView
    
    private lateinit var etBudget: EditText
    private lateinit var tvBudgetError: TextView

    // Submit
    private lateinit var btnSubmit: TextView
    private lateinit var pbSubmit: ProgressBar

    // Data
    private var productsList = listOf<SellerProductResponse>()
    
    private val placementOptions = mapOf(
        "home_hero" to "Homepage Hero Banner",
        "category_top" to "Category Top",
        "search_sidebar" to "Search Sidebar"
    )
    
    private val placementDimensions = mapOf(
        "home_hero" to "1200×400 recommended",
        "category_top" to "800×200 recommended",
        "search_sidebar" to "300×600 recommended"
    )

    private val ctaOptions = listOf("Shop Now", "Buy Now", "Explore", "Learn More", "Order Now")

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.setBannerUri(it) 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_ad)
        
        initViews()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        
        btnSelectProduct = findViewById(R.id.btnSelectProduct)
        tvSelectedProduct = findViewById(R.id.tvSelectedProduct)
        tvProductError = findViewById(R.id.tvProductError)
        
        btnSelectPlacement = findViewById(R.id.btnSelectPlacement)
        tvSelectedPlacement = findViewById(R.id.tvSelectedPlacement)
        tvPlacementError = findViewById(R.id.tvPlacementError)
        
        btnUploadBanner = findViewById(R.id.btnUploadBanner)
        ivBannerThumbnail = findViewById(R.id.ivBannerThumbnail)
        tvUploadText = findViewById(R.id.tvUploadText)
        ivUploadIcon = findViewById(R.id.ivUploadIcon)
        pbUpload = findViewById(R.id.pbUpload)
        tvImageError = findViewById(R.id.tvImageError)
        
        etHeadline = findViewById(R.id.etHeadline)
        tvHeadlineError = findViewById(R.id.tvHeadlineError)
        
        btnSelectCta = findViewById(R.id.btnSelectCta)
        tvSelectedCta = findViewById(R.id.tvSelectedCta)
        tvCtaError = findViewById(R.id.tvCtaError)
        
        ivPreviewBgPlaceholder = findViewById(R.id.ivPreviewBgPlaceholder)
        ivPreviewBgImage = findViewById(R.id.ivPreviewBgImage)
        tvPreviewHeadline = findViewById(R.id.tvPreviewHeadline)
        tvPreviewCta = findViewById(R.id.tvPreviewCta)
        tvPreviewCaption = findViewById(R.id.tvPreviewCaption)
        
        btnStartDate = findViewById(R.id.btnStartDate)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvStartDateError = findViewById(R.id.tvStartDateError)
        
        btnEndDate = findViewById(R.id.btnEndDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        tvEndDateError = findViewById(R.id.tvEndDateError)
        tvDurationInfo = findViewById(R.id.tvDurationInfo)
        
        etBudget = findViewById(R.id.etBudget)
        etBudget.setText("500")
        tvBudgetError = findViewById(R.id.tvBudgetError)
        
        btnSubmit = findViewById(R.id.btnSubmit)
        pbSubmit = findViewById(R.id.pbSubmit)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnSelectProduct.setOnClickListener { showProductSheet() }
        
        btnSelectPlacement.setOnClickListener { showPlacementSheet() }
        
        btnUploadBanner.setOnClickListener { imagePickerLauncher.launch("image/*") }
        
        etHeadline.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setHeadline(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        btnSelectCta.setOnClickListener { showCtaSheet() }
        
        btnStartDate.setOnClickListener { pickDateTime { viewModel.setStartDate(it) } }
        
        btnEndDate.setOnClickListener { pickDateTime { viewModel.setEndDate(it) } }
        
        etBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val budget = s?.toString()?.toDoubleOrNull()
                viewModel.setBudget(budget)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        btnSubmit.setOnClickListener { viewModel.submitCampaign() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.productsListState.collectLatest { state ->
                if (state is UiState.Success) {
                    productsList = state.data
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.selectedProduct.collectLatest { product ->
                tvSelectedProduct.text = product?.name ?: "Choose a product"
                tvSelectedProduct.setTextColor(if (product != null) 0xFF1A1D23.toInt() else 0xFF9AA1AC.toInt())
            }
        }
        
        lifecycleScope.launch {
            viewModel.placement.collectLatest { placement ->
                if (placement != null) {
                    val readable = placementOptions[placement] ?: placement
                    tvSelectedPlacement.text = readable
                    tvSelectedPlacement.setTextColor(0xFF1A1D23.toInt())
                    
                    val dimensions = placementDimensions[placement] ?: ""
                    tvPreviewCaption.text = "$readable · $dimensions"
                } else {
                    tvSelectedPlacement.text = "Choose placement area"
                    tvSelectedPlacement.setTextColor(0xFF9AA1AC.toInt())
                    tvPreviewCaption.text = "Select a placement area to see recommended dimensions"
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.bannerUri.collectLatest { uri ->
                if (uri != null) {
                    ivUploadIcon.visibility = View.GONE
                    tvUploadText.text = "Change banner"
                    ivBannerThumbnail.visibility = View.VISIBLE
                    ivBannerThumbnail.load(uri) {
                        transformations(RoundedCornersTransformation(10f))
                    }
                    
                    ivPreviewBgPlaceholder.visibility = View.GONE
                    ivPreviewBgImage.visibility = View.VISIBLE
                    ivPreviewBgImage.load(uri)
                } else {
                    ivUploadIcon.visibility = View.VISIBLE
                    tvUploadText.text = "Tap to upload banner"
                    ivBannerThumbnail.visibility = View.GONE
                    
                    ivPreviewBgPlaceholder.visibility = View.VISIBLE
                    ivPreviewBgImage.visibility = View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.headline.collectLatest { headline ->
                val display = if (headline.isBlank()) "Your headline here" else headline
                tvPreviewHeadline.text = display
                tvPreviewHeadline.alpha = if (headline.isBlank()) 0.7f else 1.0f
            }
        }
        
        lifecycleScope.launch {
            viewModel.ctaText.collectLatest { cta ->
                tvSelectedCta.text = cta ?: "Choose CTA"
                tvSelectedCta.setTextColor(if (cta != null) 0xFF1A1D23.toInt() else 0xFF9AA1AC.toInt())
                
                val display = cta ?: "Button"
                tvPreviewCta.text = display
                tvPreviewCta.alpha = if (cta == null) 0.7f else 1.0f
            }
        }
        
        lifecycleScope.launch {
            viewModel.startDate.collectLatest { time ->
                if (time != null) {
                    tvStartDate.text = formatDate(time)
                    tvStartDate.setTextColor(0xFF1A1D23.toInt())
                } else {
                    tvStartDate.text = "Select start date"
                    tvStartDate.setTextColor(0xFF9AA1AC.toInt())
                }
                updateDuration()
            }
        }
        
        lifecycleScope.launch {
            viewModel.endDate.collectLatest { time ->
                if (time != null) {
                    tvEndDate.text = formatDate(time)
                    tvEndDate.setTextColor(0xFF1A1D23.toInt())
                } else {
                    tvEndDate.text = "Select end date"
                    tvEndDate.setTextColor(0xFF9AA1AC.toInt())
                }
                updateDuration()
            }
        }
        
        lifecycleScope.launch {
            viewModel.validationErrors.collectLatest { errors ->
                tvProductError.visibility = if (errors.containsKey("product")) View.VISIBLE else {
                    tvProductError.text = errors["product"]
                    View.GONE
                }
                tvPlacementError.visibility = if (errors.containsKey("placement")) View.VISIBLE else View.GONE
                tvImageError.visibility = if (errors.containsKey("banner")) View.VISIBLE else View.GONE
                
                if (errors.containsKey("headline")) {
                    tvHeadlineError.visibility = View.VISIBLE
                    tvHeadlineError.text = errors["headline"]
                } else {
                    tvHeadlineError.visibility = View.GONE
                }
                
                tvCtaError.visibility = if (errors.containsKey("cta")) View.VISIBLE else View.GONE
                tvStartDateError.visibility = if (errors.containsKey("start_date")) View.VISIBLE else View.GONE
                
                if (errors.containsKey("end_date")) {
                    tvEndDateError.visibility = View.VISIBLE
                    tvEndDateError.text = errors["end_date"]
                } else {
                    tvEndDateError.visibility = View.GONE
                }
                
                if (errors.containsKey("budget")) {
                    tvBudgetError.visibility = View.VISIBLE
                    tvBudgetError.text = errors["budget"]
                } else {
                    tvBudgetError.visibility = View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.submitState.collectLatest { state ->
                when (state) {
                    is UiState.Idle -> {
                        btnSubmit.text = "Submit for review"
                        btnSubmit.alpha = 1.0f
                        btnSubmit.isClickable = true
                        pbSubmit.visibility = View.GONE
                    }
                    is UiState.Loading -> {
                        btnSubmit.text = ""
                        btnSubmit.alpha = 0.7f
                        btnSubmit.isClickable = false
                        pbSubmit.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        Toast.makeText(this@CreateAdActivity, "Campaign request submitted!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        btnSubmit.text = "Submit for review"
                        btnSubmit.alpha = 1.0f
                        btnSubmit.isClickable = true
                        pbSubmit.visibility = View.GONE
                        Toast.makeText(this@CreateAdActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateDuration() {
        val start = viewModel.startDate.value
        val end = viewModel.endDate.value
        if (start != null && end != null) {
            tvDurationInfo.visibility = View.VISIBLE
            if (end <= start) {
                tvDurationInfo.text = "Invalid duration"
                tvDurationInfo.setTextColor(0xFFD32F2F.toInt())
            } else {
                val diff = end - start
                val days = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                tvDurationInfo.text = "Runs for $days days"
                tvDurationInfo.setTextColor(0xFF2851C4.toInt())
            }
        } else {
            tvDurationInfo.visibility = View.GONE
        }
    }

    private fun showProductSheet() {
        if (productsList.isEmpty()) {
            Toast.makeText(this, "Loading products...", Toast.LENGTH_SHORT).show()
            viewModel.loadProducts()
            return
        }
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        val listView = view.findViewById<ListView>(R.id.listView)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productsList.map { it.name })
        listView.adapter = adapter
        
        listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.setProduct(productsList[position])
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun showPlacementSheet() {
        val keys = placementOptions.keys.toList()
        val values = placementOptions.values.toList()
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        val listView = view.findViewById<ListView>(R.id.listView)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, values)
        listView.adapter = adapter
        
        listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.setPlacement(keys[position])
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun showCtaSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        val listView = view.findViewById<ListView>(R.id.listView)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ctaOptions)
        listView.adapter = adapter
        
        listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.setCtaText(ctaOptions[position])
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun pickDateTime(onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onPicked(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeInMillis))
    }
}
