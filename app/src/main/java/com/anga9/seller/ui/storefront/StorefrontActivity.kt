package com.anga9.seller.ui.storefront

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StorefrontActivity : BaseActivity() {

    private lateinit var viewModel: StorefrontViewModel

    // UI Elements
    private lateinit var btnBack: ImageView
    private lateinit var switchPublish: SwitchCompat
    private lateinit var tvStoreUrl: TextView
    private lateinit var btnCopyUrl: ImageView
    private lateinit var layoutStoreUrl: View
    private lateinit var btnPreview: MaterialButton
    
    // Visuals & Profile
    private lateinit var cardBannerUpload: FrameLayout
    private lateinit var ivBannerPreview: ImageView
    private lateinit var bannerOverlay: View
    private lateinit var layoutBannerPlaceholder: View
    private lateinit var progressBannerUpload: ProgressBar
    private lateinit var etAbout: EditText
    private lateinit var tvAboutCount: TextView
    
    // Social Links
    private lateinit var etWebsite: EditText
    private lateinit var etInstagram: EditText
    private lateinit var etFacebook: EditText
    private lateinit var etTwitter: EditText
    
    // Save
    private lateinit var btnSave: MaterialButton
    private lateinit var progressSave: ProgressBar
    private lateinit var layoutLoading: View
    
    // State
    private var currentBannerUrl: String = ""
    private var selectedBannerUri: Uri? = null
    private var sellerId: String = ""

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedBannerUri = uri
            showSelectedImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storefront)

        viewModel = ViewModelProvider(this)[StorefrontViewModel::class.java]
        sellerId = TokenManager.getUserId(this) ?: ""

        initViews()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        switchPublish = findViewById(R.id.switchPublish)
        tvStoreUrl = findViewById(R.id.tvStoreUrl)
        btnCopyUrl = findViewById(R.id.btnCopyUrl)
        layoutStoreUrl = findViewById(R.id.layoutStoreUrl)
        btnPreview = findViewById(R.id.btnPreview)
        
        cardBannerUpload = findViewById(R.id.cardBannerUpload)
        ivBannerPreview = findViewById(R.id.ivBannerPreview)
        bannerOverlay = findViewById(R.id.bannerOverlay)
        layoutBannerPlaceholder = findViewById(R.id.layoutBannerPlaceholder)
        progressBannerUpload = findViewById(R.id.progressBannerUpload)
        
        etAbout = findViewById(R.id.etAbout)
        tvAboutCount = findViewById(R.id.tvAboutCount)
        
        etWebsite = findViewById(R.id.etWebsite)
        etInstagram = findViewById(R.id.etInstagram)
        etFacebook = findViewById(R.id.etFacebook)
        etTwitter = findViewById(R.id.etTwitter)
        
        btnSave = findViewById(R.id.btnSave)
        progressSave = findViewById(R.id.progressSave)
        layoutLoading = findViewById(R.id.layoutLoading)
        
        // Set URL text
        tvStoreUrl.text = "anga9.com/sellers/$sellerId"
        
        // Update publish card visual state based on switch
        switchPublish.setOnCheckedChangeListener { _, isChecked ->
            updatePublishCardState(isChecked)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnCopyUrl.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Store URL", tvStoreUrl.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        btnPreview.setOnClickListener {
            val url = "https://anga9.com/sellers/$sellerId"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        
        cardBannerUpload.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        etAbout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvAboutCount.text = "${s?.length ?: 0} / 10000"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        btnSave.setOnClickListener {
            handleSave()
        }
    }

    private fun updatePublishCardState(isPublished: Boolean) {
        val card = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublish)
        if (isPublished) {
            card.setCardBackgroundColor(android.graphics.Color.parseColor("#ECFDF5")) // green-50
            card.strokeColor = android.graphics.Color.parseColor("#D1FAE5") // green-100
        } else {
            card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            card.strokeColor = android.graphics.Color.parseColor("#E5E7EB")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is StorefrontUiState.Loading -> {
                        layoutLoading.visibility = View.VISIBLE
                    }
                    is StorefrontUiState.Success -> {
                        layoutLoading.visibility = View.GONE
                        populateData(state.profile)
                    }
                    is StorefrontUiState.Error -> {
                        layoutLoading.visibility = View.GONE
                        Toast.makeText(this@StorefrontActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.saveState.collectLatest { state ->
                when (state) {
                    is StorefrontSaveState.Idle -> {
                        setSaveLoading(false)
                    }
                    is StorefrontSaveState.Saving -> {
                        setSaveLoading(true)
                    }
                    is StorefrontSaveState.Success -> {
                        setSaveLoading(false)
                        Toast.makeText(this@StorefrontActivity, "Storefront updated", Toast.LENGTH_SHORT).show()
                        viewModel.resetSaveState()
                    }
                    is StorefrontSaveState.Error -> {
                        setSaveLoading(false)
                        Toast.makeText(this@StorefrontActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetSaveState()
                    }
                }
            }
        }
    }

    private fun populateData(profile: SellerProfileResponse) {
        switchPublish.isChecked = profile.storefrontPublished
        updatePublishCardState(profile.storefrontPublished)
        
        currentBannerUrl = profile.storefrontBannerUrl ?: ""
        if (currentBannerUrl.isNotEmpty()) {
            layoutBannerPlaceholder.visibility = View.GONE
            ivBannerPreview.visibility = View.VISIBLE
            bannerOverlay.visibility = View.VISIBLE
            ivBannerPreview.load(currentBannerUrl)
        }
        
        etAbout.setText(profile.aboutMd ?: "")
        
        profile.socialLinks?.let { social ->
            etWebsite.setText(social["website"] ?: "")
            etInstagram.setText(social["instagram"] ?: "")
            etFacebook.setText(social["facebook"] ?: "")
            etTwitter.setText(social["twitter"] ?: "")
        }
    }

    private fun showSelectedImage(uri: Uri) {
        layoutBannerPlaceholder.visibility = View.GONE
        ivBannerPreview.visibility = View.VISIBLE
        bannerOverlay.visibility = View.VISIBLE
        ivBannerPreview.load(uri)
    }

    private fun setSaveLoading(isLoading: Boolean) {
        btnSave.text = if (isLoading) "Saving..." else "Save changes"
        btnSave.isEnabled = !isLoading
        progressSave.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        if (isLoading) {
            // Also show upload progress if banner is new
            if (selectedBannerUri != null) {
                progressBannerUpload.visibility = View.VISIBLE
                layoutBannerPlaceholder.visibility = View.GONE
            }
        } else {
            progressBannerUpload.visibility = View.GONE
            if (selectedBannerUri == null && currentBannerUrl.isEmpty()) {
                layoutBannerPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    private fun handleSave() {
        val about = etAbout.text.toString()
        val published = switchPublish.isChecked
        
        val socialLinks = mutableMapOf<String, String>()
        
        fun extractUrl(key: String, et: EditText) {
            val url = et.text.toString().trim()
            if (url.isNotEmpty()) {
                socialLinks[key] = url
            }
        }
        
        extractUrl("website", etWebsite)
        extractUrl("instagram", etInstagram)
        extractUrl("facebook", etFacebook)
        extractUrl("twitter", etTwitter)

        // Validation: Ensure valid URLs
        for ((_, url) in socialLinks) {
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                Toast.makeText(this, "Please enter valid URLs for social links", Toast.LENGTH_SHORT).show()
                return
            }
        }

        viewModel.saveStorefront(
            bannerUri = selectedBannerUri,
            existingBannerUrl = currentBannerUrl,
            aboutMd = about,
            published = published,
            socialLinks = socialLinks
        )
    }
}
