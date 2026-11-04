package com.anga9.seller.ui.orders

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: SellerOrderAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
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

    private fun {
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = v.resources.displayMetrics.density
            val vPad = (12 * density).toInt()
            v.setPadding(v.paddingLeft, statusBar + vPad, v.paddingRight, vPad)
            insets
        }
        ViewCompat.requestApplyInsets(headerLayout)
    }

    private fun initViews(root: View) {
        headerLayout = root.findViewById(R.id.headerLayout)
        rvOrders = root.findViewById(R.id.rvOrders)
        swipeRefresh = root.findViewById(R.id.swipeRefresh)
        progressBar = root.findViewById(R.id.progressBar)
        tvEmpty = root.findViewById(R.id.tvEmpty)
        svStatusChips = root.findViewById(R.id.svStatusChips)
        llStatusChips = root.findViewById(R.id.llStatusChips)
        vGradientRight = root.findViewById(R.id.vGradientRight)
        etSearch = root.findViewById(R.id.etSearch)

        tvTotalOrdersCount = root.findViewById(R.id.tvTotalOrdersCount)
        cardStoreContext = root.findViewById(R.id.cardStoreContext)
        tvViewingStore = root.findViewById(R.id.tvViewingStore)
        btnManageStore = root.findViewById(R.id.btnManageStore)

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
                    tvViewingStore.text = HtmlCompat.fromHtml("Viewing: <b>$brandName</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                } else {
                    val prefs = ctx.getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val storeName = prefs.getString("store_name", null)
                        ?: prefs.getString("brand_name", "FireOn Store")
                    cardStoreContext.visibility = View.VISIBLE
                    tvViewingStore.text = HtmlCompat.fromHtml("Viewing: <b>$storeName</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            } catch (e: Exception) {
                cardStoreContext.visibility = View.VISIBLE
                tvViewingStore.text = HtmlCompat.fromHtml("Viewing: <b>FireOn Store</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    private fun setupTabs() {
        val ctx = context ?: return
        val statuses = listOf("all", "confirmed", "processing", "shipped", "delivered", "cancelled")
        var selectedStatus = "all"

        fun updateChips() {
            llStatusChips.removeAllViews()
            for (status in statuses) {
                val style = OrderStatusConfig.config[status]!!
                val chip = LayoutInflater.from(ctx).inflate(R.layout.item_filter_chip, llStatusChips, false) as TextView
                chip.text = style.label

                val bg = GradientDrawable()
                bg.cornerRadius = 64f
                if (status == selectedStatus) {
                    bg.setColor(Color.parseColor("#111318"))
                    chip.setTextColor(Color.WHITE)
                } else {
                    bg.setColor(style.getBgColor())
                    chip.setTextColor(style.getTextColor())
                    if (style.border != null) {
                        bg.setStroke(2, style.getBorderColor())
                    }
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
                viewModel.searchOrders(s?.toString() ?: "")
            }
        })
    }

    private fun observeViewModel() {
        viewModel.allOrders.observe(viewLifecycleOwner) { all ->
            val totalCount = all?.size ?: 0
            tvTotalOrdersCount.text = totalCount.toString()
        }

        viewModel.filteredOrders.observe(viewLifecycleOwner) { result ->
            swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        adapter.submitList(emptyList())
                    } else {
                        tvEmpty.visibility = View.GONE
                        adapter.submitList(ArrayList(list))
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    if (adapter.currentList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "No internet connection\nPull down to retry"
                    }
                    val msg = result.message ?: "Unable to load orders"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
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

