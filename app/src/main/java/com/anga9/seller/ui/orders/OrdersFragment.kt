package com.anga9.seller.ui.orders

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.data.repository.BrandRepository
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: SellerOrderAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var svStatusChips: HorizontalScrollView
    private lateinit var llStatusChips: LinearLayout
    private lateinit var vGradientRight: View
    private lateinit var etSearch: EditText

    // Header & Store Context
    private lateinit var headerLayout: LinearLayout
    private lateinit var tvTotalOrdersCount: TextView
    private lateinit var cardStoreContext: MaterialCardView
    private lateinit var tvViewingStore: TextView
    private lateinit var btnManageStore: TextView

    // Empty State Views
    private lateinit var layoutEmptyOrders: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var btnEmptyAction: MaterialButton

    private var selectedStatus = "all"
    private var searchQuery = ""

    private val statusLabels = mapOf(
        "all" to "All Orders",
        "confirmed" to "Confirmed",
        "processing" to "Processing",
        "shipped" to "Shipped",
        "delivered" to "Delivered",
        "cancelled" to "Cancelled"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seller_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupTabs()
        setupSearch()
        loadStoreContext()
        observeViewModel()

        viewModel.loadOrders()
    }

    private fun initViews(root: View) {
        headerLayout = root.findViewById(R.id.headerLayout)
        rvOrders = root.findViewById(R.id.rvOrders)
        swipeRefresh = root.findViewById(R.id.swipeRefresh)
        progressBar = root.findViewById(R.id.progressBar)
        svStatusChips = root.findViewById(R.id.svStatusChips)
        llStatusChips = root.findViewById(R.id.llStatusChips)
        vGradientRight = root.findViewById(R.id.vGradientRight)
        etSearch = root.findViewById(R.id.etSearch)

        tvTotalOrdersCount = root.findViewById(R.id.tvTotalOrdersCount)
        cardStoreContext = root.findViewById(R.id.cardStoreContext)
        tvViewingStore = root.findViewById(R.id.tvViewingStore)
        btnManageStore = root.findViewById(R.id.btnManageStore)

        layoutEmptyOrders = root.findViewById(R.id.layoutEmptyOrders)
        tvEmptyTitle = root.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = root.findViewById(R.id.tvEmptySubtitle)
        btnEmptyAction = root.findViewById(R.id.btnEmptyAction)

        root.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            (activity as? DashboardActivity)?.switchToTab(R.id.nav_home)
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
            loadStoreContext()
        }

        btnManageStore.setOnClickListener {
            (activity as? DashboardActivity)?.switchToTab(R.id.nav_products)
        }

        // Right fade gradient overflow handling
        svStatusChips.viewTreeObserver.addOnGlobalLayoutListener {
            checkFadeGradient()
        }
        svStatusChips.setOnScrollChangeListener { _, _, _, _, _ ->
            checkFadeGradient()
        }
    }

    private fun checkFadeGradient() {
        val canScroll = svStatusChips.canScrollHorizontally(1)
        vGradientRight.visibility = if (canScroll) View.VISIBLE else View.GONE
    }

    private fun loadStoreContext() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val brandRepo = BrandRepository(ctx)
                val res = brandRepo.getBrands()
                if (res is Resource.Success && !res.data.isNullOrEmpty()) {
                    val brands = res.data
                    val activeId = TokenManager.getActiveBrandId(ctx)
                    val brand = brands.find { it.id == activeId } ?: brands.firstOrNull()
                    val brandName = brand?.storeName ?: "FireOn Store"
                    cardStoreContext.visibility = View.VISIBLE
                    tvViewingStore.text = HtmlCompat.fromHtml("Store: <b>$brandName</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                } else {
                    val prefs = ctx.getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val storeName = prefs.getString("store_name", null)
                        ?: prefs.getString("brand_name", "FireOn Store")
                    cardStoreContext.visibility = View.VISIBLE
                    tvViewingStore.text = HtmlCompat.fromHtml("Store: <b>$storeName</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            } catch (e: Exception) {
                cardStoreContext.visibility = View.VISIBLE
                tvViewingStore.text = HtmlCompat.fromHtml("Store: <b>FireOn Store</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    private fun setupTabs() {
        val ctx = context ?: return
        val statuses = listOf("all", "confirmed", "processing", "shipped", "delivered", "cancelled")
        val density = resources.displayMetrics.density

        fun updateChips() {
            llStatusChips.removeAllViews()
            for (status in statuses) {
                val label = statusLabels[status] ?: status.replaceFirstChar { it.uppercase() }
                val chip = LayoutInflater.from(ctx).inflate(R.layout.item_filter_chip, llStatusChips, false) as TextView
                chip.text = label

                val bg = GradientDrawable()
                bg.cornerRadius = 20 * density
                if (status == selectedStatus) {
                    bg.setColor(Color.parseColor("#1A6FD4"))
                    chip.setTextColor(Color.WHITE)
                    chip.setTypeface(null, Typeface.BOLD)
                } else {
                    bg.setColor(Color.parseColor("#F1F5F9"))
                    bg.setStroke((1 * density).toInt(), Color.parseColor("#E2E8F0"))
                    chip.setTextColor(Color.parseColor("#475569"))
                    chip.setTypeface(null, Typeface.NORMAL)
                }
                chip.background = bg

                chip.setOnClickListener {
                    selectedStatus = status
                    updateChips()
                    viewModel.setStatusFilter(status)
                }
                llStatusChips.addView(chip)
            }
            llStatusChips.post { checkFadeGradient() }
        }

        updateChips()
    }

    private fun setupRecyclerView() {
        adapter = SellerOrderAdapter(
            onOrderClick = { order ->
                val intent = Intent(requireContext(), OrderDetailActivity::class.java)
                intent.putExtra("orderId", order.id)
                startActivity(intent)
            }
        )
        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.isNestedScrollingEnabled = false
        rvOrders.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                viewModel.searchOrders(searchQuery)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.allOrders.observe(viewLifecycleOwner) { all ->
            val totalCount = all?.size ?: 0
            tvTotalOrdersCount.text = "$totalCount Orders"
        }

        viewModel.filteredOrders.observe(viewLifecycleOwner) { result ->
            swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    layoutEmptyOrders.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        adapter.submitList(emptyList())
                        showEmptyState()
                    } else {
                        layoutEmptyOrders.visibility = View.GONE
                        rvOrders.visibility = View.VISIBLE
                        adapter.submitList(ArrayList(list))
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    if (adapter.currentList.isEmpty()) {
                        layoutEmptyOrders.visibility = View.VISIBLE
                        rvOrders.visibility = View.GONE
                        tvEmptyTitle.text = "Connection issue"
                        tvEmptySubtitle.text = "Unable to load orders right now.\nPull down to retry."
                        btnEmptyAction.text = "Retry"
                        btnEmptyAction.visibility = View.VISIBLE
                        btnEmptyAction.setOnClickListener { viewModel.loadOrders() }
                    }
                    val msg = result.message ?: "Unable to load orders"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEmptyState() {
        rvOrders.visibility = View.GONE
        layoutEmptyOrders.visibility = View.VISIBLE

        val allCount = viewModel.allOrders.value?.size ?: 0

        if (allCount == 0) {
            tvEmptyTitle.text = "No orders yet"
            tvEmptySubtitle.text = "When customers purchase your products, incoming orders will appear here for fulfillment."
            btnEmptyAction.text = "Manage Products →"
            btnEmptyAction.visibility = View.VISIBLE
            btnEmptyAction.setOnClickListener {
                (activity as? DashboardActivity)?.switchToTab(R.id.nav_products)
            }
        } else if (searchQuery.isNotEmpty()) {
            tvEmptyTitle.text = "No matching orders"
            tvEmptySubtitle.text = "No orders match \"$searchQuery\". Try checking the spelling or search with another order ID."
            btnEmptyAction.text = "Clear Search"
            btnEmptyAction.visibility = View.VISIBLE
            btnEmptyAction.setOnClickListener {
                etSearch.setText("")
            }
        } else {
            val label = statusLabels[selectedStatus] ?: selectedStatus
            tvEmptyTitle.text = "No $label orders"
            tvEmptySubtitle.text = "There are currently no orders in ${label.lowercase()} status."
            btnEmptyAction.text = "View All Orders"
            btnEmptyAction.visibility = View.VISIBLE
            btnEmptyAction.setOnClickListener {
                selectedStatus = "all"
                setupTabs()
                viewModel.setStatusFilter("all")
            }
        }
    }

    fun refreshOrders() {
        if (::swipeRefresh.isInitialized) {
            viewModel.loadOrders()
            loadStoreContext()
        }
    }
}
