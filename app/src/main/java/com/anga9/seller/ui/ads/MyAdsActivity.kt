package com.anga9.seller.ui.ads

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.AdCampaignResponse
import com.anga9.seller.utils.UiState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MyAdsActivity : BaseActivity() {

    private val viewModel: AdsViewModel by viewModels()
    private lateinit var recyclerAds: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var cardEmptyState: CardView
    private lateinit var btnCreateAdEmpty: View
    private lateinit var fabCreateAd: FloatingActionButton
    private lateinit var adapter: MyAdAdapter

    private lateinit var layoutStatsRow: View
    private lateinit var layoutStatsRowSkeleton: View
    private lateinit var tvActiveCampaignsCount: TextView
    private lateinit var tvTotalClicksCount: TextView
    private lateinit var tvTotalImpressionsCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ads)
        
        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        viewModel.loadMyCampaigns()
    }

    private fun initViews() {
        recyclerAds = findViewById(R.id.recyclerAds)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        cardEmptyState = findViewById(R.id.cardEmptyState)
        btnCreateAdEmpty = findViewById(R.id.btnCreateAdEmpty)
        fabCreateAd = findViewById(R.id.fabCreateAd)
        
        layoutStatsRow = findViewById(R.id.layoutStatsRow)
        layoutStatsRowSkeleton = findViewById(R.id.layoutStatsRowSkeleton)
        tvActiveCampaignsCount = findViewById(R.id.tvActiveCampaignsCount)
        tvTotalClicksCount = findViewById(R.id.tvTotalClicksCount)
        tvTotalImpressionsCount = findViewById(R.id.tvTotalImpressionsCount)
        
        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = MyAdAdapter(
            onItemClick = { campaign ->
                Toast.makeText(this, "Detail view coming soon", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerAds.layoutManager = LinearLayoutManager(this)
        recyclerAds.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { 
            viewModel.loadMyCampaigns() 
        }
        
        val createAdClickListener = View.OnClickListener {
            handleCreateAdClick()
        }
        
        btnCreateAdEmpty.setOnClickListener(createAdClickListener)
        fabCreateAd.setOnClickListener(createAdClickListener)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.campaignsState.collect { state ->
                when (state) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> {
                        if (!swipeRefresh.isRefreshing) {
                            progressBar.visibility = View.VISIBLE
                            if (adapter.itemCount == 0) {
                                layoutStatsRow.visibility = View.GONE
                                layoutStatsRowSkeleton.visibility = View.VISIBLE
                            }
                        }
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        showCampaigns(state.data)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@MyAdsActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleCreateAdClick() {
        startActivity(Intent(this, CreateAdActivity::class.java))
    }

    private fun showCampaigns(campaigns: List<AdCampaignResponse>) {
        layoutStatsRowSkeleton.visibility = View.GONE
        
        if (campaigns.isEmpty()) {
            recyclerAds.visibility = View.GONE
            cardEmptyState.visibility = View.VISIBLE
            fabCreateAd.visibility = View.GONE
            layoutStatsRow.visibility = View.GONE
        } else {
            recyclerAds.visibility = View.VISIBLE
            cardEmptyState.visibility = View.GONE
            fabCreateAd.visibility = View.VISIBLE
            layoutStatsRow.visibility = View.VISIBLE
            
            // Calculate stats
            val activeCount = campaigns.count { it.status.equals("active", ignoreCase = true) }
            val totalClicks = campaigns.sumOf { it.clicks }
            val totalImpressions = campaigns.sumOf { it.impressions }
            
            tvActiveCampaignsCount.text = activeCount.toString()
            tvTotalClicksCount.text = String.format("%,d", totalClicks)
            tvTotalImpressionsCount.text = String.format("%,d", totalImpressions)
            
            adapter.submitList(campaigns)
        }
    }


    private fun openPreview(campaign: AdCampaignResponse) {
        val intent = Intent(this, AdPreviewActivity::class.java)
        intent.putExtra(AdPreviewActivity.EXTRA_CAMPAIGN_ID, campaign.id)
        intent.putExtra(AdPreviewActivity.EXTRA_MODE, AdPreviewActivity.MODE_VIEW_ONLY)
        
        intent.putExtra(AdPreviewActivity.EXTRA_HEADLINE, campaign.headline)
        intent.putExtra(AdPreviewActivity.EXTRA_CTA, campaign.ctaText ?: "Shop Now")
        
        val isBanner = campaign.placement == "home_hero" || campaign.placement == "category_banner"
        val adType = if (isBanner) com.anga9.seller.data.model.AdType.BANNER else com.anga9.seller.data.model.AdType.IN_FEED
        intent.putExtra(AdPreviewActivity.EXTRA_AD_TYPE, adType)
        
        // Use the same banner URL logic as adapter
        val bannerUrl = campaign.bannerUrl
        var fullUrl = ""
        if (!bannerUrl.isNullOrEmpty()) {
            val supabaseUrl = "https://plfaugkadavxenpqawzw.supabase.co"
            fullUrl = if (bannerUrl.startsWith("http")) {
                bannerUrl
            } else if (bannerUrl.startsWith("/storage/")) {
                "$supabaseUrl$bannerUrl"
            } else if (bannerUrl.startsWith("/")) {
                "$supabaseUrl/storage/v1/object/public/public-assets$bannerUrl"
            } else {
                "$supabaseUrl/storage/v1/object/public/public-assets/$bannerUrl"
            }
        }
        intent.putExtra(AdPreviewActivity.EXTRA_BANNER_URL, fullUrl)
        intent.putExtra(AdPreviewActivity.EXTRA_PRODUCT_NAME, campaign.products?.name ?: "")
        intent.putExtra(AdPreviewActivity.EXTRA_AMOUNT, campaign.budgetInr)
        
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Optionally refresh on resume if coming back from creation
        if (!swipeRefresh.isRefreshing) {
            viewModel.loadMyCampaigns()
        }
    }
}
