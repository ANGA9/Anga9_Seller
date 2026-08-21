package com.anga9.seller.ui.ads

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import coil.load
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.AdCampaign
import com.anga9.seller.data.model.AdPricing
import com.anga9.seller.data.model.AdStatus
import com.anga9.seller.data.model.AdType
import com.anga9.seller.data.model.CreatedBy
import com.anga9.seller.data.model.ManagedBy
import com.anga9.seller.data.model.PaymentStatus
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.UiState
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RequestAdActivity : BaseActivity() {

    private val viewModel: AdsViewModel by viewModels()

    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutBannerType: LinearLayout
    private lateinit var layoutInFeedType: LinearLayout
    private lateinit var layoutSelectProduct: LinearLayout
    private lateinit var ivSelectedProduct: ImageView
    private lateinit var tvSelectedProduct: TextView
    private lateinit var etRequestNotes: TextInputEditText
    private lateinit var layoutApproxPreview: LinearLayout
    private lateinit var btnShowApproxPreview: TextView
    private lateinit var layout7Days: LinearLayout
    private lateinit var layout15Days: LinearLayout
    private lateinit var layout30Days: LinearLayout
    private lateinit var tvPrice7: TextView
    private lateinit var tvPrice15: TextView
    private lateinit var tvPrice30: TextView
    private lateinit var tvBaseAmount: TextView
    private lateinit var tvServiceCharge: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvWalletBalance: TextView
    private lateinit var btnPreviewApprox: TextView
    private lateinit var btnSubmitRequest: TextView

    // State
    private var selectedAdType = AdType.BANNER
    private var selectedDuration = 7
    private var pricing = AdPricing.default()
    private var selectedProductId = ""
    private var selectedProductName = ""
    private var selectedProductImage = ""
    private var selectedProductPrice = 0.0
    private var selectedProductCategory = ""

    private val productPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            selectedProductId = data.getStringExtra("productId") ?: ""
            selectedProductName = data.getStringExtra("productName") ?: ""
            selectedProductImage = data.getStringExtra("productImage") ?: ""
            selectedProductPrice = data.getDoubleExtra("productPrice", 0.0)
            selectedProductCategory = data.getStringExtra("productCategory") ?: ""
            tvSelectedProduct.text = selectedProductName
            tvSelectedProduct.setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
            if (selectedProductImage.isNotEmpty()) {
                ivSelectedProduct.load(selectedProductImage)
            }
            layoutApproxPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_ad)
        initViews()
        setupListeners()
        observeViewModel()
        viewModel.loadPricing()
        loadWalletBalance()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        layoutBannerType = findViewById(R.id.layoutBannerType)
        layoutInFeedType = findViewById(R.id.layoutInFeedType)
        layoutSelectProduct = findViewById(R.id.layoutSelectProduct)
        ivSelectedProduct = findViewById(R.id.ivSelectedProduct)
        tvSelectedProduct = findViewById(R.id.tvSelectedProduct)
        etRequestNotes = findViewById(R.id.etRequestNotes)
        layoutApproxPreview = findViewById(R.id.layoutApproxPreview)
        btnShowApproxPreview = findViewById(R.id.btnShowApproxPreview)
        layout7Days = findViewById(R.id.layout7Days)
        layout15Days = findViewById(R.id.layout15Days)
        layout30Days = findViewById(R.id.layout30Days)
        tvPrice7 = findViewById(R.id.tvPrice7)
        tvPrice15 = findViewById(R.id.tvPrice15)
        tvPrice30 = findViewById(R.id.tvPrice30)
        tvBaseAmount = findViewById(R.id.tvBaseAmount)
        tvServiceCharge = findViewById(R.id.tvServiceCharge)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvWalletBalance = findViewById(R.id.tvWalletBalance)
        btnPreviewApprox = findViewById(R.id.btnPreviewApprox)
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        selectAdType(AdType.BANNER)
        selectDuration(7)
    }

    private fun setupListeners() {
        layoutBannerType.setOnClickListener { selectAdType(AdType.BANNER) }
        layoutInFeedType.setOnClickListener { selectAdType(AdType.IN_FEED) }
        layout7Days.setOnClickListener { selectDuration(7) }
        layout15Days.setOnClickListener { selectDuration(15) }
        layout30Days.setOnClickListener { selectDuration(30) }
        layoutSelectProduct.setOnClickListener {
            productPickerLauncher.launch(Intent(this, AdProductPickerActivity::class.java))
        }
        btnShowApproxPreview.setOnClickListener { openApproxPreview() }
        btnPreviewApprox.setOnClickListener { openApproxPreview() }
        btnSubmitRequest.setOnClickListener { confirmAndSubmit() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.pricingState.collect { state ->
                if (state is com.anga9.seller.utils.Resource.Success) {
                    pricing = state.data?.firstOrNull() ?: pricing
                    updatePriceDisplay()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnSubmitRequest.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        btnSubmitRequest.isEnabled = true
                        Toast.makeText(
                            this@RequestAdActivity,
                            "Ã¢Å“â€¦ Request submitted! Admin will design your ad and send you a preview.",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                        viewModel.resetActionState()
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnSubmitRequest.isEnabled = true
                        Toast.makeText(this@RequestAdActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                        btnSubmitRequest.isEnabled = true
                    }
                }
            }
        }
    }

    private fun selectAdType(type: String) {
        selectedAdType = type
        val activeColor = android.graphics.Color.parseColor("#E3F2FD")
        val inactiveColor = android.graphics.Color.parseColor("#F5F5F5")
        layoutBannerType.setBackgroundColor(if (type == AdType.BANNER) activeColor else inactiveColor)
        layoutInFeedType.setBackgroundColor(if (type == AdType.IN_FEED) activeColor else inactiveColor)
        updatePriceDisplay()
    }

    private fun selectDuration(days: Int) {
        selectedDuration = days
        val activeColor = android.graphics.Color.parseColor("#E3F2FD")
        val inactiveColor = android.graphics.Color.parseColor("#F5F5F5")
        layout7Days.setBackgroundColor(if (days == 7) activeColor else inactiveColor)
        layout15Days.setBackgroundColor(if (days == 15) activeColor else inactiveColor)
        layout30Days.setBackgroundColor(if (days == 30) activeColor else inactiveColor)
        updatePriceDisplay()
    }

    private fun updatePriceDisplay() {
        tvPrice7.text = "Ã¢â€šÂ¹${pricing.getTotalManaged(selectedAdType, 7).toInt()}"
        tvPrice15.text = "Ã¢â€šÂ¹${pricing.getTotalManaged(selectedAdType, 15).toInt()}"
        tvPrice30.text = "Ã¢â€šÂ¹${pricing.getTotalManaged(selectedAdType, 30).toInt()}"
        val base = pricing.getBasePrice(selectedAdType, selectedDuration)
        val service = pricing.getServiceCharge(selectedAdType, selectedDuration)
        tvBaseAmount.text = "Ã¢â€šÂ¹${base.toInt()}"
        tvServiceCharge.text = "+ Ã¢â€šÂ¹${service.toInt()}"
        tvTotalAmount.text = "Ã¢â€šÂ¹${(base + service).toInt()}"
    }

    private fun validate(): Boolean {
        if (selectedProductId.isEmpty()) {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun openApproxPreview() {
        val intent = Intent(this, AdPreviewActivity::class.java).apply {
            putExtra(AdPreviewActivity.EXTRA_MODE, AdPreviewActivity.MODE_VIEW_ONLY)
            putExtra(AdPreviewActivity.EXTRA_AD_TYPE, selectedAdType)
            putExtra(AdPreviewActivity.EXTRA_HEADLINE, "Your headline will be designed by admin")
            putExtra(AdPreviewActivity.EXTRA_CTA, "Shop Now")
            putExtra(AdPreviewActivity.EXTRA_DURATION, selectedDuration)
            putExtra(AdPreviewActivity.EXTRA_AMOUNT, pricing.getTotalManaged(selectedAdType, selectedDuration))
            putExtra(AdPreviewActivity.EXTRA_PRODUCT_NAME, selectedProductName)
            putExtra(AdPreviewActivity.EXTRA_PRODUCT_IMAGE, selectedProductImage)
            putExtra(AdPreviewActivity.EXTRA_PRODUCT_PRICE, selectedProductPrice)
            putExtra(EXTRA_IS_APPROX, true)
        }
        startActivity(intent)
    }

    private fun confirmAndSubmit() {
        if (!validate()) return
        val notes = etRequestNotes.text?.toString()?.trim() ?: ""
        val base = pricing.getBasePrice(selectedAdType, selectedDuration)
        val service = pricing.getServiceCharge(selectedAdType, selectedDuration)
        val total = base + service

        AlertDialog.Builder(this)
            .setTitle("Submit Ad Request")
            .setMessage(
                "Ã¢â€šÂ¹${total.toInt()} will be deducted from your wallet as advance payment.\n\n" +
                "Ã¢â‚¬Â¢ Ad Type: ${if (selectedAdType == AdType.BANNER) "Banner" else "In-Feed"}\n" +
                "Ã¢â‚¬Â¢ Duration: $selectedDuration days\n" +
                "Ã¢â‚¬Â¢ Ad Placement: Ã¢â€šÂ¹${base.toInt()}\n" +
                "Ã¢â‚¬Â¢ Design Service: Ã¢â€šÂ¹${service.toInt()}\n\n" +
                "Admin will design your ad and send you a preview for approval."
            )
            .setPositiveButton("Pay Ã¢â€šÂ¹${total.toInt()} & Submit") { _, _ ->
                val campaign = AdCampaign(
                    adType = selectedAdType,
                    productId = selectedProductId,
                    productName = selectedProductName,
                    productImage = selectedProductImage,
                    productPrice = selectedProductPrice,
                    productCategory = selectedProductCategory,
                    requestNotes = notes,
                    duration = selectedDuration,
                    baseAmount = base,
                    serviceCharge = service,
                    totalAmount = total,
                    createdBy = CreatedBy.SELLER_REQUEST,
                    managedBy = ManagedBy.ADMIN,
                    status = AdStatus.REQUEST_SUBMITTED,
                    paymentStatus = PaymentStatus.PAID
                )
                viewModel.submitAdRequest(campaign)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadWalletBalance() {
        // Load wallet balance from backend (Phase 3E)
        kotlinx.coroutines.MainScope().launch {
            try {
                val walletRepo = com.anga9.seller.data.repository.WalletRepository(this@RequestAdActivity)
                walletRepo.getEarnings().collect { resource ->
                    if (resource is com.anga9.seller.utils.Resource.Success) {
                        val balance = resource.data?.available ?: 0.0
                        tvWalletBalance.text = "\u20B9${String.format("%.2f", balance)}"
                    } else if (resource is com.anga9.seller.utils.Resource.Error) {
                        tvWalletBalance.text = "Unable to load"
                    }
                }
            } catch (e: Exception) {
                tvWalletBalance.text = "Unable to load"
            }
        }
    }

    companion object {
        const val EXTRA_IS_APPROX = "is_approx_preview"
    }
}
