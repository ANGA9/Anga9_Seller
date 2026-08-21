package com.anga9.seller.ui.dashboard

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.BaseActivity
import com.anga9.seller.MVVM.ui.products.AddProductWizardActivity
import com.anga9.seller.MVVM.ui.products.MyProductsActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.RecentOrderItem
import com.anga9.seller.data.model.SellerDashboardStats
import com.anga9.seller.network.model.SellerProfileResponse
import com.anga9.seller.network.model.TopProductItem
import com.anga9.seller.ui.ads.MyAdsActivity
import com.anga9.seller.ui.analytics.AnalyticsActivity
import com.anga9.seller.ui.deals.PromotionalDealsActivity
import com.anga9.seller.ui.disputes.SellerDisputesActivity
import com.anga9.seller.ui.inventory.InventoryActivity
import com.anga9.seller.ui.notifications.NotificationsActivity
import com.anga9.seller.ui.orders.MyOrdersActivity
import com.anga9.seller.ui.orders.OrderDetailActivity
import com.anga9.seller.ui.profile.SellerProfileActivity
import com.anga9.seller.ui.reviews.ReviewsActivity
import com.anga9.seller.ui.storefront.StorefrontActivity
import com.anga9.seller.ui.support.SupportHomeActivity
import com.anga9.seller.ui.wallet.EarningsActivity
import com.anga9.seller.ui.wallet.PayoutsActivity
import com.anga9.seller.utils.AppFormatters
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardActivity : BaseActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var prefs: SharedPreferences

    // Root Drawer & Navigation
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigation: BottomNavigationView

    // Layout views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var homeContent: NestedScrollView

    // Header
    private lateinit var btnDrawerOpen: ImageView
    private lateinit var tvInitials: TextView
    private lateinit var tvStoreName: TextView
    private lateinit var tvSellerId: TextView
    private lateinit var ivVerifiedBadge: ImageView
    private lateinit var vNotificationBadge: View
    private lateinit var flNotificationContainer: FrameLayout

    // Drawer Views
    private lateinit var tvDrawerInitials: TextView
    private lateinit var tvDrawerStoreName: TextView
    private lateinit var tvDrawerSellerId: TextView
    private lateinit var ivDrawerVerified: ImageView

    // Banners & Offline Fullscreen
    private lateinit var tvOfflineBanner: View
    private lateinit var cvOnboardingBanner: MaterialCardView
    private lateinit var layoutOfflineFullscreen: View
    private lateinit var btnOfflineRetry: MaterialButton

    // Time Toggle
    private lateinit var toggleTimeRange: MaterialButtonToggleGroup

    // 2x2 Stat Cards
    private lateinit var cardOrdersStat: View
    private lateinit var cardRevenueStat: View
    private lateinit var cardActiveProductsStat: View
    private lateinit var cardToFulfillStat: View

    // Revenue Trend Card
    private lateinit var sparklineRevenueTrend: SparklineView

    // Wallet Row
    private lateinit var tvWalletBalance: TextView
    private lateinit var tvNextPayout: TextView
    private lateinit var btnWithdraw: MaterialButton

    // Quick Actions
    private lateinit var btnQuickAddProduct: View
    private lateinit var btnQuickOrders: View
    private lateinit var btnQuickSupport: View

    // Recent Orders
    private lateinit var rvRecentOrders: RecyclerView
    private lateinit var tvViewAllOrders: TextView
    private lateinit var layoutEmptyOrders: View
    private lateinit var recentOrderAdapter: RecentOrderAdapter

    // Top Products
    private lateinit var rvTopProducts: RecyclerView
    private lateinit var tvViewAllTopProducts: TextView
    private lateinit var tvEmptyTopProducts: TextView
    private lateinit var topProductsAdapter: TopProductsAdapter

    private var currentPeriodLabel = "30d"
    private lateinit var networkMonitor: com.anga9.seller.utils.NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        networkMonitor = com.anga9.seller.utils.NetworkMonitor(this)

        initViews()
        setupDrawer()
        setupRecyclerViews()
        setupListeners()
        setupBottomNavigation()
        observeViewModel()

        viewModel.loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_home
        viewModel.refresh()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        homeContent = findViewById(R.id.homeContent)

        // Header
        val layoutHeader = findViewById<View>(R.id.layoutHeader)
        btnDrawerOpen = layoutHeader.findViewById(R.id.btnDrawerOpen)
        tvInitials = layoutHeader.findViewById(R.id.tvInitials)
        tvStoreName = layoutHeader.findViewById(R.id.tvStoreName)
        tvSellerId = layoutHeader.findViewById(R.id.tvSellerId)
        ivVerifiedBadge = layoutHeader.findViewById(R.id.ivVerifiedBadge)
        vNotificationBadge = layoutHeader.findViewById(R.id.vNotificationBadge)
        flNotificationContainer = layoutHeader.findViewById(R.id.flNotificationContainer)

        // Drawer Header Views
        tvDrawerInitials = findViewById(R.id.tvDrawerInitials)
        tvDrawerStoreName = findViewById(R.id.tvDrawerStoreName)
        tvDrawerSellerId = findViewById(R.id.tvDrawerSellerId)
        ivDrawerVerified = findViewById(R.id.ivDrawerVerified)

        // Banners & Fullscreen Offline
        tvOfflineBanner = findViewById(R.id.layoutOfflineBanner)
        cvOnboardingBanner = findViewById(R.id.layoutOnboarding)
        layoutOfflineFullscreen = findViewById(R.id.layoutOfflineFullscreen)
        btnOfflineRetry = layoutOfflineFullscreen.findViewById(R.id.btnOfflineRetry)

        btnOfflineRetry.setOnClickListener {
            btnOfflineRetry.isEnabled = false
            btnOfflineRetry.text = "Checking..."
            viewModel.refresh()
            btnOfflineRetry.postDelayed({
                btnOfflineRetry.isEnabled = true
                btnOfflineRetry.text = "Retry Connection"
            }, 1200)
        }

        // Time Range
        toggleTimeRange = findViewById(R.id.toggleTimeRange)

        // Stat Cards
        cardOrdersStat = findViewById(R.id.cardOrdersStat)
        cardRevenueStat = findViewById(R.id.cardRevenueStat)
        cardActiveProductsStat = findViewById(R.id.cardActiveProductsStat)
        cardToFulfillStat = findViewById(R.id.cardToFulfillStat)

        setupStatCardInitial(cardOrdersStat, "Orders")
        setupStatCardInitial(cardRevenueStat, "Revenue")
        setupStatCardInitial(cardActiveProductsStat, "Active Products")
        setupStatCardInitial(cardToFulfillStat, "To Fulfill")

        // Revenue Trend
        sparklineRevenueTrend = findViewById(R.id.sparklineRevenueTrend)

        // Wallet Row
        val cardWalletRow = findViewById<View>(R.id.cardWalletRow)
        tvWalletBalance = cardWalletRow.findViewById(R.id.tvWalletBalance)
        tvNextPayout = cardWalletRow.findViewById(R.id.tvNextPayout)
        btnWithdraw = cardWalletRow.findViewById(R.id.btnWithdraw)

        // Quick Actions
        btnQuickAddProduct = findViewById(R.id.btnQuickAddProduct)
        btnQuickOrders = findViewById(R.id.btnQuickOrders)
        btnQuickSupport = findViewById(R.id.btnQuickSupport)

        setupQuickAction(btnQuickAddProduct, "Add Product", R.drawable.ic_baseline_add_24)
        setupQuickAction(btnQuickOrders, "Orders", R.drawable.ic_orders)
        setupQuickAction(btnQuickSupport, "Support", R.drawable.ic_baseline_help_outline_24)

        // Recent Orders
        rvRecentOrders = findViewById(R.id.rvRecentOrders)
        tvViewAllOrders = findViewById(R.id.tvViewAllOrders)
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders)

        // Top Products
        rvTopProducts = findViewById(R.id.rvTopProducts)
        tvViewAllTopProducts = findViewById(R.id.tvViewAllTopProducts)
        tvEmptyTopProducts = findViewById(R.id.tvEmptyTopProducts)
    }

    private fun setupStatCardInitial(cardView: View, label: String) {
        cardView.findViewById<TextView>(R.id.tvStatLabel)?.text = label
    }

    private fun setupQuickAction(tileView: View, label: String, iconRes: Int) {
        tileView.findViewById<TextView>(R.id.tvActionLabel)?.text = label
        tileView.findViewById<ImageView>(R.id.ivActionIcon)?.setImageResource(iconRes)
    }

    private fun setupRecyclerViews() {
        recentOrderAdapter = RecentOrderAdapter { order ->
            val intent = Intent(this, OrderDetailActivity::class.java).apply {
                putExtra("order_id", order.orderId)
                putExtra("order_number", order.orderNumber)
            }
            startActivity(intent)
        }
        rvRecentOrders.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = recentOrderAdapter
            isNestedScrollingEnabled = false
        }

        topProductsAdapter = TopProductsAdapter {
            startActivity(Intent(this, MyProductsActivity::class.java))
        }
        rvTopProducts.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = topProductsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupDrawer() {
        btnDrawerOpen.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.drawerHeader).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SellerProfileActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerEarnings).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, EarningsActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerPayouts).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, PayoutsActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerAds).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerDisputes).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SellerDisputesActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerDeals).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, PromotionalDealsActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerReviews).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ReviewsActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerStorefront).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, StorefrontActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerSupport).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SupportHomeActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerProfile).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SellerProfileActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerPrivacy).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, com.anga9.seller.ui.privacy.DataPrivacyActivity::class.java))
        }

        findViewById<View>(R.id.rowDrawerLogout).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showLogoutConfirmation()
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        flNotificationContainer.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        cvOnboardingBanner.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.auth.KycStatusActivity::class.java))
        }

        toggleTimeRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnToday -> {
                        currentPeriodLabel = "today"
                        viewModel.setPeriod("today")
                    }
                    R.id.btn7Days -> {
                        currentPeriodLabel = "7d"
                        viewModel.setPeriod("7d")
                    }
                    R.id.btn30Days -> {
                        currentPeriodLabel = "30d"
                        viewModel.setPeriod("30d")
                    }
                }
            }
        }

        btnWithdraw.setOnClickListener {
            startActivity(Intent(this, EarningsActivity::class.java))
        }

        btnQuickAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductWizardActivity::class.java))
        }

        btnQuickOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        btnQuickSupport.setOnClickListener {
            startActivity(Intent(this, SupportHomeActivity::class.java))
        }

        tvViewAllOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        tvViewAllTopProducts.setOnClickListener {
            startActivity(Intent(this, MyProductsActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                sysBarInsets.bottom
            )
            insets
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_orders -> {
                    startActivity(Intent(this, MyOrdersActivity::class.java))
                    false
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, MyProductsActivity::class.java))
                    false
                }
                R.id.nav_inventory -> {
                    startActivity(Intent(this, InventoryActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            networkMonitor.isConnected.collectLatest { isConnected ->
                if (!isConnected) {
                    if (viewModel.dashboardState.value !is UiState.Success) {
                        layoutOfflineFullscreen.visibility = View.VISIBLE
                        homeContent.visibility = View.GONE
                        tvOfflineBanner.visibility = View.GONE
                    } else {
                        layoutOfflineFullscreen.visibility = View.GONE
                        homeContent.visibility = View.VISIBLE
                        tvOfflineBanner.visibility = View.VISIBLE
                    }
                } else {
                    layoutOfflineFullscreen.visibility = View.GONE
                    homeContent.visibility = View.VISIBLE
                    tvOfflineBanner.visibility = View.GONE
                    viewModel.refresh()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.sellerProfile.collectLatest { profile ->
                profile?.let { updateProfileUI(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.dashboardState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        swipeRefresh.isRefreshing = true
                    }
                    is UiState.Success -> {
                        swipeRefresh.isRefreshing = false
                        layoutOfflineFullscreen.visibility = View.GONE
                        homeContent.visibility = View.VISIBLE
                        bindDashboardStats(state.data)
                    }
                    is UiState.Error -> {
                        swipeRefresh.isRefreshing = false
                        if (!networkMonitor.isOnline() && viewModel.dashboardState.value !is UiState.Success) {
                            layoutOfflineFullscreen.visibility = View.VISIBLE
                            homeContent.visibility = View.GONE
                        } else if (networkMonitor.isOnline()) {
                            Toast.makeText(this@DashboardActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    UiState.Idle -> {
                        swipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun updateProfileUI(profile: SellerProfileResponse) {
        val storeName = profile.storeName ?: profile.businessName ?: "My Store"
        val initials = storeName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "S" }

        tvStoreName.text = storeName
        tvInitials.text = initials
        tvDrawerStoreName.text = storeName
        tvDrawerInitials.text = initials

        val sellerId = profile.id.ifEmpty { TokenManager.getEffectiveSellerId(this) ?: "" }
        val idText = if (sellerId.isNotEmpty()) "ID: ${sellerId.take(8).uppercase()}" else "Seller"
        tvSellerId.text = idText
        tvDrawerSellerId.text = idText

        val isVerified = profile.verificationStatus == "verified" || profile.isVerified
        ivVerifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE
        ivDrawerVerified.visibility = if (isVerified) View.VISIBLE else View.GONE
        cvOnboardingBanner.visibility = if (isVerified) View.GONE else View.VISIBLE
    }

    private fun bindDashboardStats(stats: SellerDashboardStats) {
        // 1. Orders Card (Zero-state-safe delta)
        bindStatCard(
            cardView = cardOrdersStat,
            label = "Total Orders",
            value = stats.totalOrders.toString(),
            current = stats.totalOrders.toDouble(),
            previous = stats.previousOrders.toDouble(),
            metricName = "orders"
        )

        // 2. Revenue Card (Zero-state-safe delta)
        bindStatCard(
            cardView = cardRevenueStat,
            label = "Total Revenue",
            value = AppFormatters.formatINR(stats.totalRevenue),
            current = stats.totalRevenue,
            previous = stats.previousRevenue,
            metricName = "revenue"
        )

        // 3. Active Products (Green tinted)
        bindTintedCard(
            cardView = cardActiveProductsStat,
            label = "Active Products",
            value = stats.activeProducts.toString(),
            subtitle = "Live on marketplace",
            textColor = 0xFF15803D.toInt()
        )

        // 4. To Fulfill (Amber tinted actionable count)
        bindTintedCard(
            cardView = cardToFulfillStat,
            label = "Orders to Fulfill",
            value = stats.pendingOrders.toString(),
            subtitle = if (stats.pendingOrders > 0) "Needs immediate shipping" else "All orders fulfilled",
            textColor = if (stats.pendingOrders > 0) 0xFFB45309.toInt() else 0xFF6B7280.toInt()
        )

        // 5. Revenue Trend Sparkline & Badge
        sparklineRevenueTrend.setData(stats.revenueTrend)
        val badgeText = when (currentPeriodLabel) {
            "today" -> "Today"
            "7d" -> "Last 7 days"
            else -> "Last 30 days"
        }
        findViewById<TextView>(R.id.tvRevenueTrendBadge)?.text = badgeText

        // 6. Wallet Row
        tvWalletBalance.text = AppFormatters.formatINR(stats.walletBalance)
        if (stats.pendingPayout > 0.0) {
            tvNextPayout.text = "${AppFormatters.formatINR(stats.pendingPayout)} pending payout"
            tvNextPayout.setTextColor(0xFFB45309.toInt())
        } else {
            tvNextPayout.text = "No pending payouts"
            tvNextPayout.setTextColor(0xFF6B7280.toInt())
        }

        // 7. Recent Orders
        if (stats.recentOrders.isNotEmpty()) {
            rvRecentOrders.visibility = View.VISIBLE
            layoutEmptyOrders.visibility = View.GONE
            recentOrderAdapter.updateOrders(stats.recentOrders)
        } else {
            rvRecentOrders.visibility = View.GONE
            layoutEmptyOrders.visibility = View.VISIBLE
        }

        // 8. Top Products
        if (stats.topProducts.isNotEmpty()) {
            rvTopProducts.visibility = View.VISIBLE
            tvEmptyTopProducts.visibility = View.GONE
            topProductsAdapter.submitList(stats.topProducts)
        } else {
            rvTopProducts.visibility = View.GONE
            tvEmptyTopProducts.visibility = View.VISIBLE
        }
    }

    private fun bindStatCard(
        cardView: View,
        label: String,
        value: String,
        current: Double,
        previous: Double,
        metricName: String
    ) {
        cardView.findViewById<TextView>(R.id.tvStatLabel)?.text = label
        cardView.findViewById<TextView>(R.id.tvStatValue)?.text = value

        val tvTrendValue = cardView.findViewById<TextView>(R.id.tvTrendValue)
        val ivTrendIcon = cardView.findViewById<ImageView>(R.id.ivTrendIcon)

        val delta = AppFormatters.formatDelta(
            current = current,
            previous = previous,
            metricName = metricName,
            periodLabel = currentPeriodLabel
        )

        tvTrendValue?.text = delta.text

        when (delta.isPositive) {
            true -> {
                ivTrendIcon?.visibility = View.VISIBLE
                ivTrendIcon?.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                ivTrendIcon?.setColorFilter(0xFF15803D.toInt())
                tvTrendValue?.setTextColor(0xFF15803D.toInt())
            }
            false -> {
                ivTrendIcon?.visibility = View.VISIBLE
                ivTrendIcon?.setImageResource(R.drawable.ic_baseline_arrow_downward_24)
                ivTrendIcon?.setColorFilter(0xFFB91C1C.toInt())
                tvTrendValue?.setTextColor(0xFFB91C1C.toInt())
            }
            null -> {
                ivTrendIcon?.visibility = View.GONE
                tvTrendValue?.setTextColor(0xFF6B7280.toInt())
            }
        }
    }

    private fun bindTintedCard(
        cardView: View,
        label: String,
        value: String,
        subtitle: String,
        textColor: Int
    ) {
        cardView.findViewById<TextView>(R.id.tvStatLabel)?.text = label
        cardView.findViewById<TextView>(R.id.tvStatValue)?.apply {
            text = value
            setTextColor(textColor)
        }
        cardView.findViewById<ImageView>(R.id.ivTrendIcon)?.visibility = View.GONE
        cardView.findViewById<TextView>(R.id.tvTrendValue)?.apply {
            text = subtitle
            setTextColor(0xFF6B7280.toInt())
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of your seller account?")
            .setPositiveButton("Log Out") { _, _ ->
                TokenManager.clearAll(this)
                val intent = Intent(this, com.anga9.seller.auth.SellerPhoneLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
