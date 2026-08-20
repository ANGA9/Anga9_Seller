package com.anga9.seller.ui.dashboard

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var prefs: SharedPreferences
    private lateinit var recentOrderAdapter: RecentOrderAdapter

    // Layout views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var homeContent: NestedScrollView
    private lateinit var moreContent: View

    // Header
    private lateinit var tvInitials: TextView
    private lateinit var tvStoreName: TextView
    private lateinit var tvSellerId: TextView
    private lateinit var ivVerifiedBadge: ImageView
    private lateinit var vNotificationBadge: View
    private lateinit var flNotificationContainer: FrameLayout

    // Banners
    private lateinit var tvOfflineBanner: TextView
    private lateinit var cvOnboardingBanner: MaterialCardView

    // Time Toggle
    private lateinit var toggleTimeRange: MaterialButtonToggleGroup

    // Stats
    private lateinit var cardOrdersStat: View
    private lateinit var cardRevenueStat: View
    
    // Wallet
    private lateinit var tvWalletBalance: TextView
    private lateinit var tvNextPayout: TextView
    private lateinit var btnWithdraw: MaterialButton

    // Action Needed
    private lateinit var llActionNeededContainer: LinearLayout

    // Quick Actions
    private lateinit var btnQuickAddProduct: View
    private lateinit var btnQuickOrders: View
    private lateinit var btnQuickSupport: View
    private lateinit var btnQuickAnalytics: View

    // Recent Orders
    private lateinit var rvRecentOrders: RecyclerView
    private lateinit var tvViewAllOrders: TextView
    private lateinit var layoutEmptyOrders: View
    private lateinit var btnEmptyAction: MaterialButton

    private lateinit var bottomNavigation: BottomNavigationView

    private var isKycApproved = false
    private var currentStats: SellerDashboardStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        initViews()
        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()

        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_home
        val sellerId = prefs.getString(Constants.PREF_SELLER_ID, "") ?: ""
        if (sellerId.isNotEmpty()) viewModel.loadDashboard()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        homeContent = findViewById(R.id.homeContent)
        moreContent = findViewById(R.id.moreContent)

        val layoutHeader = findViewById<View>(R.id.layoutHeader)
        tvInitials = layoutHeader.findViewById(R.id.tvInitials)
        tvStoreName = layoutHeader.findViewById(R.id.tvStoreName)
        tvSellerId = layoutHeader.findViewById(R.id.tvSellerId)
        ivVerifiedBadge = layoutHeader.findViewById(R.id.ivVerifiedBadge)
        vNotificationBadge = layoutHeader.findViewById(R.id.vNotificationBadge)
        flNotificationContainer = layoutHeader.findViewById(R.id.flNotificationContainer)

        tvOfflineBanner = findViewById(R.id.layoutOfflineBanner)
        
        cvOnboardingBanner = findViewById(R.id.layoutOnboarding)

        toggleTimeRange = findViewById(R.id.toggleTimeRange)
        cardOrdersStat = findViewById(R.id.cardOrdersStat)
        cardRevenueStat = findViewById(R.id.cardRevenueStat)

        val cardWalletRow = findViewById<View>(R.id.cardWalletRow)
        tvWalletBalance = cardWalletRow.findViewById(R.id.tvWalletBalance)
        tvNextPayout = cardWalletRow.findViewById(R.id.tvNextPayout)
        btnWithdraw = cardWalletRow.findViewById(R.id.btnWithdraw)

        llActionNeededContainer = findViewById(R.id.llActionNeededContainer)

        btnQuickAddProduct = findViewById(R.id.btnQuickAddProduct)
        btnQuickOrders = findViewById(R.id.btnQuickOrders)
        btnQuickSupport = findViewById(R.id.btnQuickSupport)
        btnQuickAnalytics = findViewById(R.id.btnQuickAnalytics)

        rvRecentOrders = findViewById(R.id.rvRecentOrders)
        tvViewAllOrders = findViewById(R.id.tvViewAllOrders)
        
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders)
        btnEmptyAction = layoutEmptyOrders.findViewById(R.id.btnEmptyAction)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        setupStatCard(cardOrdersStat, "Orders", R.drawable.ic_outline_inventory_2_24)
        setupStatCard(cardRevenueStat, "Revenue", R.drawable.ic_baseline_monetization_on_24)
        setupQuickAction(btnQuickAddProduct, "Add Product", R.drawable.ic_baseline_add_24)
        setupQuickAction(btnQuickOrders, "Orders", R.drawable.ic_outline_inventory_2_24)
        setupQuickAction(btnQuickSupport, "Support", R.drawable.ic_baseline_help_outline_24)
        setupQuickAction(btnQuickAnalytics, "Analytics", R.drawable.ic_baseline_insert_chart_outlined_24)
    }

    private fun setupStatCard(cardView: View, label: String, iconRes: Int) {
        val tvLabel = cardView.findViewById<TextView>(R.id.tvStatLabel)
        tvLabel?.text = label
    }

    private fun setupQuickAction(tileView: View, label: String, iconRes: Int) {
        val tvLabel = tileView.findViewById<TextView>(R.id.tvActionLabel)
        val ivIcon = tileView.findViewById<ImageView>(R.id.ivActionIcon)
        tvLabel?.text = label
        ivIcon?.setImageResource(iconRes)
    }

    private fun setupRecyclerView() {
        recentOrderAdapter = RecentOrderAdapter { order ->
            Toast.makeText(this, "Order #${order.orderId.takeLast(6).uppercase()}", Toast.LENGTH_SHORT).show()
        }
        rvRecentOrders.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = recentOrderAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadDashboard()
        }

        flNotificationContainer.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.notifications.NotificationsActivity::class.java))
        }

        cvOnboardingBanner.setOnClickListener {
            // Navigate to KYC / Onboarding
            startActivity(Intent(this@DashboardActivity, com.anga9.seller.auth.KycStatusActivity::class.java))
        }

        btnQuickAddProduct.setOnClickListener { navigateToAddProduct() }
        btnQuickOrders.setOnClickListener { navigateToOrders() }
        btnQuickSupport.setOnClickListener { 
            startActivity(Intent(this, com.anga9.seller.ui.support.SupportHomeActivity::class.java))
        }
        btnQuickAnalytics.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.analytics.AnalyticsActivity::class.java))
        }

        tvViewAllOrders.setOnClickListener { navigateToOrders() }
        btnEmptyAction.setOnClickListener {
            // Dummy share action
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out my store on ANGA9!")
            }
            startActivity(Intent.createChooser(intent, "Share store link"))
        }
        
        btnWithdraw.setOnClickListener { navigateToWallet() }

        setupMoreScreenListeners()
    }

    private fun setupMoreScreenListeners() {
        val moreContent = findViewById<View>(R.id.moreContent)

        // Setup labels
        moreContent.findViewById<TextView>(R.id.tvMenuLabel)?.apply {
            if (this.parent == moreContent.findViewById<View>(R.id.rowMyProfile)) text = "My profile"
        }
        // Since we are using include, we must bind labels specifically:
        moreContent.findViewById<View>(R.id.rowMyProfile)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "My profile"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_user)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.profile.SellerProfileActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowMyStorefront)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "My storefront"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_storefront)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.storefront.StorefrontActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowDataPrivacy)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Data & Privacy"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_privacy_shield)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.privacy.DataPrivacyActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowMyAdCampaigns)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "My ad campaigns"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_megaphone)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.ads.MyAdsActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowPromotionalDeals)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Promotional Deals"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_tag)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.deals.PromotionalDealsActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowInventory)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Inventory Management"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_package) // Fallback to package icon
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.inventory.InventoryActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowRepeatBuyers)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Repeat buyers"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_repeat)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.b2b.RepeatBuyersActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowCustomerReviews)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Customer reviews"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_star_outline)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.reviews.ReviewsActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowMyReturns)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "My returns"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_undo)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.returns.MyReturnsActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowHelpAndSupport)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).text = "Help and support"
            it.findViewById<ImageView>(R.id.ivLeadingIcon).setImageResource(R.drawable.ic_more_help)
            it.setOnClickListener { startActivity(Intent(this, com.anga9.seller.ui.support.SupportHomeActivity::class.java)) }
        }
        moreContent.findViewById<View>(R.id.rowLogout)?.let {
            it.findViewById<TextView>(R.id.tvMenuLabel).apply { 
                text = "Log out"
                setTextColor(android.graphics.Color.parseColor("#D8342A"))
            }
            it.findViewById<ImageView>(R.id.ivLeadingIcon).apply {
                setImageResource(R.drawable.ic_more_logout)
                imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D8342A"))
            }
            it.findViewById<ImageView>(R.id.ivTrailingChevron).visibility = View.GONE
            it.setOnClickListener { showLogoutDialog() }
        }

        moreContent.findViewById<View>(R.id.labelAccount)?.findViewById<TextView>(R.id.tvSectionLabel)?.text = "Account"
        moreContent.findViewById<View>(R.id.labelBusiness)?.findViewById<TextView>(R.id.tvSectionLabel)?.text = "Business"
        moreContent.findViewById<View>(R.id.labelOrdersAndSupport)?.findViewById<TextView>(R.id.tvSectionLabel)?.text = "Orders and Support"

        moreContent.findViewById<View>(R.id.includeProfileSummary)?.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.profile.SellerProfileActivity::class.java))
        }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            moreContent.findViewById<TextView>(R.id.tvAppVersion)?.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            moreContent.findViewById<TextView>(R.id.tvAppVersion)?.text = "Version 1.0"
        }
        moreContent.findViewById<TextView>(R.id.tvTerms)?.setOnClickListener {
            com.anga9.seller.ui.legal.LegalActivity.startTerms(this)
        }
        moreContent.findViewById<TextView>(R.id.tvPrivacy)?.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.privacy.DataPrivacyActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home  -> { showHomeContent(); true }
                R.id.nav_orders   -> { navigateToOrders(); true }
                R.id.nav_products -> { navigateToProducts(); true }
                R.id.nav_wallet   -> { navigateToWallet(); true }
                R.id.nav_more     -> { showMoreContent(); true }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun loadDashboard() {
        val sellerId = TokenManager.getUserId(this) ?: prefs.getString(Constants.PREF_SELLER_ID, "") ?: ""
        if (sellerId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            logout()
            return
        }

        checkKycStatusForBanner(sellerId)

        val cachedKycStatus = prefs.getString("cached_kyc_status", "") ?: ""
        if (cachedKycStatus == Constants.KYC_APPROVED) {
            isKycApproved = true
        }

        lifecycleScope.launch {
            viewModel.dashboardState.collect { state ->
                when (state) {
                    is UiState.Idle    -> viewModel.loadDashboard()
                    is UiState.Loading -> {
                        if (!swipeRefresh.isRefreshing) swipeRefresh.isRefreshing = true
                    }
                    is UiState.Success -> {
                        swipeRefresh.isRefreshing = false
                        updateUI(state.data)
                    }
                    is UiState.Error -> {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@DashboardActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.sellerProfile.collect { profile ->
                updateMoreProfileUI(profile)
            }
        }
    }

    private fun updateUI(stats: SellerDashboardStats) {
        currentStats = stats
        
        // Header
        var sellerName = prefs.getString("seller_business_name", "Seller") ?: "Seller"
        if (sellerName.isBlank()) sellerName = "Seller"
        tvStoreName.text = sellerName
        val sellerId = TokenManager.getUserId(this) ?: "000"
        tvSellerId.text = "ID: SELL-${sellerId.takeLast(4).uppercase()}"
        tvInitials.text = sellerName.take(2).uppercase()
        
        if (isKycApproved) {
            ivVerifiedBadge.visibility = View.VISIBLE
        } else {
            ivVerifiedBadge.visibility = View.GONE
        }

        // Stats Cards
        updateStatCardValue(cardOrdersStat, stats.todayOrders.toString(), "12% vs last period", true)
        updateStatCardValue(cardRevenueStat, formatCurrency(stats.totalRevenue), "8% vs last period", true)
        
        // Wallet Row
        tvWalletBalance.text = formatCurrency(stats.walletBalance)
        if (stats.pendingPayout > 0) {
            tvNextPayout.text = "Pending payout: ${formatCurrency(stats.pendingPayout)}"
        } else {
            tvNextPayout.text = "No pending payouts"
        }

        // Action Needed
        llActionNeededContainer.removeAllViews()
        if (stats.pendingOrders > 0) {
            val actionCard = layoutInflater.inflate(R.layout.layout_action_needed_card, llActionNeededContainer, false)
            actionCard.findViewById<TextView>(R.id.tvActionTitle).text = "${stats.pendingOrders} orders need confirmation"
            actionCard.findViewById<TextView>(R.id.tvActionSubtitle).text = "Confirm within 24 hours to avoid penalties"
            actionCard.setOnClickListener { navigateToOrders() }
            llActionNeededContainer.addView(actionCard)
        }
        if (stats.lowStockProducts > 0) {
            val actionCard = layoutInflater.inflate(R.layout.layout_action_needed_card, llActionNeededContainer, false)
            actionCard.findViewById<TextView>(R.id.tvActionTitle).text = "${stats.lowStockProducts} products low on stock"
            actionCard.findViewById<TextView>(R.id.tvActionSubtitle).text = "Restock soon to avoid losing ranking"
            actionCard.setOnClickListener { navigateToProducts() }
            llActionNeededContainer.addView(actionCard)
        }

        // Recent Orders
        updateRecentOrders(stats)
    }

    private fun updateStatCardValue(cardView: View, value: String, trend: String, isPositive: Boolean) {
        val tvValue = cardView.findViewById<TextView>(R.id.tvStatValue)
        val tvTrend = cardView.findViewById<TextView>(R.id.tvTrendValue)
        val ivTrend = cardView.findViewById<ImageView>(R.id.ivTrendIcon)
        
        tvValue?.text = value
        tvTrend?.text = trend
        
        if (isPositive) {
            tvTrend?.setTextColor(getColor(R.color.seller_success))
            ivTrend?.setColorFilter(getColor(R.color.seller_success))
            ivTrend?.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
        } else {
            tvTrend?.setTextColor(getColor(R.color.seller_danger))
            ivTrend?.setColorFilter(getColor(R.color.seller_danger))
            ivTrend?.setImageResource(R.drawable.ic_baseline_arrow_downward_24)
        }
    }

    private fun updateRecentOrders(stats: SellerDashboardStats) {
        val orders = stats.recentOrders
        if (orders.isEmpty()) {
            rvRecentOrders.visibility = View.GONE
            layoutEmptyOrders.visibility = View.VISIBLE
        } else {
            rvRecentOrders.visibility = View.VISIBLE
            layoutEmptyOrders.visibility = View.GONE
            recentOrderAdapter.updateOrders(orders.take(3)) // Show only top 3
        }

        // Update badges for More Options screen
        val moreContent = findViewById<View>(R.id.moreContent)
        
        moreContent.findViewById<View>(R.id.rowMyReturns)?.findViewById<TextView>(R.id.tvBadgeCount)?.apply {
            if (stats.pendingReturns > 0) {
                visibility = View.VISIBLE
                text = stats.pendingReturns.toString()
            } else {
                visibility = View.GONE
            }
        }
        moreContent.findViewById<View>(R.id.rowHelpAndSupport)?.findViewById<TextView>(R.id.tvBadgeCount)?.apply {
            if (stats.openTickets > 0) {
                visibility = View.VISIBLE
                text = stats.openTickets.toString()
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun updateMoreProfileUI(profile: com.anga9.seller.network.model.SellerProfileResponse?) {
        val moreContent = findViewById<View>(R.id.moreContent)
        val profileCard = moreContent.findViewById<View>(R.id.includeProfileSummary) ?: return

        val viewSkeletonAvatar = profileCard.findViewById<View>(R.id.viewSkeletonAvatar)
        val viewSkeletonName = profileCard.findViewById<View>(R.id.viewSkeletonName)
        val viewSkeletonId = profileCard.findViewById<View>(R.id.viewSkeletonId)
        val tvAvatar = profileCard.findViewById<TextView>(R.id.tvAvatar)
        val tvStoreName = profileCard.findViewById<TextView>(R.id.tvStoreName)
        val ivVerifiedBadge = profileCard.findViewById<ImageView>(R.id.ivVerifiedBadge)
        val tvSellerId = profileCard.findViewById<TextView>(R.id.tvSellerId)

        if (profile == null) {
            // Show skeletons
            viewSkeletonAvatar?.visibility = View.VISIBLE
            viewSkeletonName?.visibility = View.VISIBLE
            viewSkeletonId?.visibility = View.VISIBLE
            tvAvatar?.visibility = View.INVISIBLE
            tvStoreName?.visibility = View.INVISIBLE
            ivVerifiedBadge?.visibility = View.INVISIBLE
            tvSellerId?.visibility = View.INVISIBLE
            return
        }

        // Hide skeletons
        viewSkeletonAvatar?.visibility = View.GONE
        viewSkeletonName?.visibility = View.GONE
        viewSkeletonId?.visibility = View.GONE
        tvAvatar?.visibility = View.VISIBLE
        tvStoreName?.visibility = View.VISIBLE
        tvSellerId?.visibility = View.VISIBLE

        val businessName = profile.businessName?.takeIf { it.isNotBlank() } ?: profile.name?.takeIf { it.isNotBlank() } ?: "Seller"
        val ownerName = profile.ownerName?.takeIf { it.isNotBlank() } ?: businessName
        val initial = if (ownerName.isNotEmpty()) ownerName.substring(0, 1).uppercase() else "S"
        
        tvStoreName?.text = businessName
        tvAvatar?.text = initial
        tvSellerId?.text = if (!profile.id.isNullOrBlank()) "ID: ${profile.id}" else ""
        
        ivVerifiedBadge?.visibility = if (profile.kycStatus == "verified") View.VISIBLE else View.GONE
    }

    private fun checkKycStatusForBanner(sellerId: String) {
        lifecycleScope.launch {
            try {
                val profileRepo = com.anga9.seller.data.repository.ProfileRepository(applicationContext)
                val result = profileRepo.getSellerProfile()
                result.fold(
                    onSuccess = { profile ->
                        val kycStatus = profile.kycStatus ?: ""
                        if (kycStatus == "verified") {
                            isKycApproved = true
                            cvOnboardingBanner.visibility = View.GONE
                        } else {
                            isKycApproved = false
                            cvOnboardingBanner.visibility = View.VISIBLE
                        }
                    },
                    onFailure = {
                        val cachedStatus = prefs.getString("cached_kyc_status", "") ?: ""
                        isKycApproved = (cachedStatus == "verified")
                        cvOnboardingBanner.visibility = if (isKycApproved) View.GONE else View.VISIBLE
                    }
                )
            } catch (_: Exception) {}
        }
    }

    private fun formatCurrency(amount: Double): String =
        "\u20B9" + String.format("%,.0f", amount)

    private fun requireKyc(action: () -> Unit) {
        if (isKycApproved) action() else showKycPendingDialog()
    }

    private fun showKycPendingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Waiting for Approval")
            .setMessage("Your account is under review. You can access all features once your KYC is approved.\n\nThis usually takes 24-48 hours.")
            .setPositiveButton("View Status") { _, _ ->
                startActivity(Intent(this, com.anga9.seller.auth.KycStatusActivity::class.java))
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun navigateToOrders()    { requireKyc { startActivity(Intent(this, com.anga9.seller.ui.orders.MyOrdersActivity::class.java)) } }
    private fun navigateToProducts()  { requireKyc { startActivity(Intent(this, com.anga9.seller.MVVM.ui.products.MyProductsActivity::class.java)) } }
    private fun navigateToAddProduct(){ requireKyc { startActivity(Intent(this, com.anga9.seller.MVVM.ui.products.AddProductWizardActivity::class.java)) } }
    private fun navigateToWallet()    { requireKyc { startActivity(Intent(this, com.anga9.seller.ui.wallet.EarningsActivity::class.java)) } }

    private fun showHomeContent() {
        homeContent.visibility = View.VISIBLE
        moreContent.visibility = View.GONE
    }

    private fun showMoreContent() {
        homeContent.visibility = View.GONE
        moreContent.visibility = View.VISIBLE
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Kya aap logout karna chahte hain?")
            .setPositiveButton("Haan") { _, _ -> logout() }
            .setNegativeButton("Nahi", null)
            .show()
    }

    private fun logout() {
        TokenManager.clearAll(this)
        prefs.edit().clear().apply()
        val intent = Intent(this, com.anga9.seller.auth.SellerPhoneLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (moreContent.visibility == View.VISIBLE) {
            showHomeContent()
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Kya aap app band karna chahte hain?")
            .setPositiveButton("Haan") { _, _ -> finishAffinity() }
            .setNegativeButton("Nahi", null)
            .show()
    }
}
