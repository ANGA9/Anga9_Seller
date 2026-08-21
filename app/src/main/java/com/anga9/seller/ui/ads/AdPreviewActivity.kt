package com.anga9.seller.ui.ads

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.AdStatus
import com.anga9.seller.data.model.AdType
import com.anga9.seller.utils.UiState
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class AdPreviewActivity : BaseActivity() {

    private val viewModel: AdsViewModel by viewModels()

    // Views
    private lateinit var tabPreviewType: TabLayout
    private lateinit var layoutBannerPreview: LinearLayout
    private lateinit var layoutInFeedPreview: LinearLayout
    private lateinit var ivBannerPreview: ImageView
    private lateinit var tvBannerHeadline: TextView
    private lateinit var tvBannerCta: TextView
    private lateinit var layoutBannerPlaceholder: View
    private var tvBannerSellerName: TextView? = null
    private lateinit var ivInFeedProduct: ImageView
    private lateinit var tvInFeedProductName: TextView
    private lateinit var tvInFeedPrice: TextView
    private lateinit var tvInFeedCta: TextView
    private lateinit var tvSummaryAdType: TextView
    private lateinit var tvSummaryDuration: TextView
    private lateinit var tvSummaryAmount: TextView
    private lateinit var btnEdit: TextView
    private lateinit var btnConfirmPay: TextView

    // Data from intent
    private var campaignId = ""
    private var mode = MODE_VIEW_ONLY
    private var adType = AdType.BANNER
    private var headline = ""
    private var ctaText = "Shop Now"
    private var duration = 7
    private var amount = 0.0
    private var bannerUrl = ""
    private var productName = ""
    private var productImage = ""
    private var productPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_preview)
        extractIntentData()
        initViews()
        setupTabs()
        setupListeners()
        populatePreview()
        observeViewModel()

        // If viewing existing campaign, load from Firestore
        if (mode == MODE_VIEW_ONLY && campaignId.isNotEmpty()) {
            viewModel.loadCampaignById(campaignId)
            viewModel.markPreviewViewed(campaignId)
        }
    }

    private fun extractIntentData() {
        campaignId = intent.getStringExtra(EXTRA_CAMPAIGN_ID) ?: ""
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VIEW_ONLY
        adType = intent.getStringExtra(EXTRA_AD_TYPE) ?: AdType.BANNER
        headline = intent.getStringExtra(EXTRA_HEADLINE) ?: ""
        ctaText = intent.getStringExtra(EXTRA_CTA) ?: "Shop Now"
        duration = intent.getIntExtra(EXTRA_DURATION, 7)
        amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        bannerUrl = intent.getStringExtra(EXTRA_BANNER_URL) ?: ""
        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: ""
        productImage = intent.getStringExtra(EXTRA_PRODUCT_IMAGE) ?: ""
        productPrice = intent.getDoubleExtra(EXTRA_PRODUCT_PRICE, 0.0)
    }

    private fun initViews() {
        tabPreviewType = findViewById(R.id.tabPreviewType)
        layoutBannerPreview = findViewById(R.id.layoutBannerPreview)
        layoutInFeedPreview = findViewById(R.id.layoutInFeedPreview)
        ivBannerPreview = findViewById(R.id.ivBannerPreview)
        tvBannerHeadline = findViewById(R.id.tvBannerHeadline)
        tvBannerCta = findViewById(R.id.tvBannerCta)
        layoutBannerPlaceholder = findViewById(R.id.layoutBannerPlaceholder)
        tvBannerSellerName = findViewById(R.id.tvBannerSellerName)
        ivInFeedProduct = findViewById(R.id.ivInFeedProduct)
        tvInFeedProductName = findViewById(R.id.tvInFeedProductName)
        tvInFeedPrice = findViewById(R.id.tvInFeedPrice)
        tvInFeedCta = findViewById(R.id.tvInFeedCta)
        tvSummaryAdType = findViewById(R.id.tvSummaryAdType)
        tvSummaryDuration = findViewById(R.id.tvSummaryDuration)
        tvSummaryAmount = findViewById(R.id.tvSummaryAmount)
        btnEdit = findViewById(R.id.btnEdit)
        btnConfirmPay = findViewById(R.id.btnConfirmPay)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Adjust buttons based on mode
        when (mode) {
            MODE_VIEW_ONLY -> {
                btnEdit.visibility = View.GONE
                btnConfirmPay.text = "Close"
                btnConfirmPay.setOnClickListener { finish() }
            }
            MODE_SELLER_REVIEW -> {
                btnEdit.text = "Request Changes"
                btnConfirmPay.text = "Approve & Go Live"
            }
            MODE_CONFIRM_PAY -> {
                btnEdit.text = "← Edit Ad"
                btnConfirmPay.text = "Confirm & Pay →"
            }
        }
    }

    private fun setupTabs() {
        tabPreviewType.addTab(tabPreviewType.newTab().setText("Banner View"))
        tabPreviewType.addTab(tabPreviewType.newTab().setText("In-Feed View"))

        if (adType == AdType.IN_FEED) {
            tabPreviewType.getTabAt(1)?.select()
            showInFeedPreview()
        } else {
            showBannerPreview()
        }

        tabPreviewType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) showBannerPreview() else showInFeedPreview()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupListeners() {
        btnEdit.setOnClickListener {
            when (mode) {
                MODE_CONFIRM_PAY -> {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                MODE_SELLER_REVIEW -> showFeedbackDialog()
            }
        }
        btnConfirmPay.setOnClickListener {
            when (mode) {
                MODE_CONFIRM_PAY -> confirmPayment()
                MODE_SELLER_REVIEW -> confirmApprovePreview()
                MODE_VIEW_ONLY -> finish()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.campaignState.collect { state ->
                if (state is UiState.Success) {
                    val campaign = state.data
                    headline = campaign.headline
                    ctaText = campaign.ctaText
                    adType = campaign.adType
                    bannerUrl = campaign.bannerImageUrl
                    productName = campaign.productName
                    productImage = campaign.productImage
                    productPrice = campaign.productPrice
                    duration = campaign.duration
                    amount = campaign.totalAmount
                    populatePreview()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        when (state.data) {
                            "submitted" -> {
                                Toast.makeText(
                                    this@AdPreviewActivity,
                                    "✅ Ad submitted for review! You'll be notified once approved.",
                                    Toast.LENGTH_LONG
                                ).show()
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                            "preview_approved" -> {
                                Toast.makeText(
                                    this@AdPreviewActivity,
                                    "✅ Preview approved! Admin will publish your ad shortly.",
                                    Toast.LENGTH_LONG
                                ).show()
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                            "feedback_sent" -> {
                                Toast.makeText(this@AdPreviewActivity, "Feedback sent to admin.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        viewModel.resetActionState()
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@AdPreviewActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun populatePreview() {
        // Banner preview
        tvBannerHeadline.text = headline.ifEmpty { "Your Headline Here" }
        tvBannerCta.text = ctaText.ifEmpty { "Shop Now" }
        if (bannerUrl.isNotEmpty()) {
            ivBannerPreview.visibility = View.VISIBLE
            layoutBannerPlaceholder.visibility = View.GONE
            ivBannerPreview.load(bannerUrl)
        } else {
            layoutBannerPlaceholder.visibility = View.VISIBLE
        }

        // In-feed preview
        tvInFeedProductName.text = productName.ifEmpty { "Product Name" }
        tvInFeedPrice.text = if (productPrice > 0) "₹${productPrice.toInt()}" else "₹999"
        tvInFeedCta.text = ctaText.ifEmpty { "Shop Now" }
        if (productImage.isNotEmpty()) {
            ivInFeedProduct.load(productImage)
        }

        // Summary
        tvSummaryAdType.text = if (adType == AdType.BANNER) "Banner" else "In-Feed"
        tvSummaryDuration.text = "$duration days"
        tvSummaryAmount.text = "₹${amount.toInt()}"
    }

    private fun showBannerPreview() {
        layoutBannerPreview.visibility = View.VISIBLE
        layoutInFeedPreview.visibility = View.GONE
    }

    private fun showInFeedPreview() {
        layoutBannerPreview.visibility = View.GONE
        layoutInFeedPreview.visibility = View.VISIBLE
    }

    private fun confirmPayment() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Payment")
            .setMessage("₹${amount.toInt()} will be deducted from your wallet.\n\nYour ad will go live after admin approval (usually within 24 hours).")
            .setPositiveButton("Pay ₹${amount.toInt()}") { _, _ ->
                viewModel.payAndSubmit(campaignId, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmApprovePreview() {
        AlertDialog.Builder(this)
            .setTitle("Approve Design?")
            .setMessage("Are you happy with this ad design? Admin will publish it after your approval.")
            .setPositiveButton("Yes, Approve") { _, _ ->
                viewModel.approvePreview(campaignId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFeedbackDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Describe what changes you want..."
            setPadding(48, 32, 48, 32)
            maxLines = 4
        }
        AlertDialog.Builder(this)
            .setTitle("Request Changes")
            .setMessage("Tell admin what you'd like changed:")
            .setView(input)
            .setPositiveButton("Send Feedback") { _, _ ->
                val feedback = input.text.toString().trim()
                if (feedback.isEmpty()) {
                    Toast.makeText(this, "Please describe the changes needed", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.requestPreviewChanges(campaignId, feedback)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val EXTRA_CAMPAIGN_ID = "campaign_id"
        const val EXTRA_MODE = "mode"
        const val EXTRA_AD_TYPE = "ad_type"
        const val EXTRA_HEADLINE = "headline"
        const val EXTRA_CTA = "cta_text"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_BANNER_URL = "banner_url"
        const val EXTRA_PRODUCT_NAME = "product_name"
        const val EXTRA_PRODUCT_IMAGE = "product_image"
        const val EXTRA_PRODUCT_PRICE = "product_price"
        const val MODE_VIEW_ONLY = "view_only"
        const val MODE_CONFIRM_PAY = "confirm_pay"
        const val MODE_SELLER_REVIEW = "seller_review"
    }
}
