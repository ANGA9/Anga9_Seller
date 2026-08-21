package com.anga9.seller.ui.orders

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.data.repository.BrandRepository
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MyOrdersActivity : AppCompatActivity() {

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
    private lateinit var tvTotalOrdersCount: TextView
    private lateinit var cardStoreContext: MaterialCardView
    private lateinit var tvViewingStore: TextView
    private lateinit var btnManageStore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        initViews()
        setupRecyclerView()
        setupTabs()
        setupSearch()
        loadStoreContext()
        observeViewModel()

        viewModel.loadOrders()
    }

    private fun initViews() {
        rvOrders = findViewById(R.id.rvOrders)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        svStatusChips = findViewById(R.id.svStatusChips)
        llStatusChips = findViewById(R.id.llStatusChips)
        vGradientRight = findViewById(R.id.vGradientRight)
        etSearch = findViewById(R.id.etSearch)

        tvTotalOrdersCount = findViewById(R.id.tvTotalOrdersCount)
        cardStoreContext = findViewById(R.id.cardStoreContext)
        tvViewingStore = findViewById(R.id.tvViewingStore)
        btnManageStore = findViewById(R.id.btnManageStore)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
            loadStoreContext()
        }

        btnManageStore.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.MVVM.ui.products.MyProductsActivity::class.java))
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
        lifecycleScope.launch {
            try {
                val brandRepo = BrandRepository(this@MyOrdersActivity)
                val res = brandRepo.getBrands()
                if (res is Resource.Success && !res.data.isNullOrEmpty()) {
                    val brands = res.data
                    val activeId = TokenManager.getActiveBrandId(this@MyOrdersActivity)
                    val brand = brands.find { it.id == activeId } ?: brands.firstOrNull()
                    val brandName = brand?.storeName ?: "FireOn Store"
                    cardStoreContext.visibility = View.VISIBLE
                    tvViewingStore.text = HtmlCompat.fromHtml("Viewing: <b>$brandName</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                } else {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
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
        val statuses = listOf("all", "confirmed", "processing", "shipped", "delivered", "cancelled")
        var selectedStatus = "all"

        fun updateChips() {
            llStatusChips.removeAllViews()
            for (status in statuses) {
                val style = OrderStatusConfig.config[status]!!
                val chip = LayoutInflater.from(this).inflate(R.layout.item_filter_chip, llStatusChips, false) as TextView
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
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("orderId", order.id)
                startActivity(intent)
            }
        )
        rvOrders.layoutManager = LinearLayoutManager(this)
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
        viewModel.allOrders.observe(this) { all ->
            val totalCount = all?.size ?: 0
            tvTotalOrdersCount.text = totalCount.toString()
        }

        viewModel.filteredOrders.observe(this) { result ->
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
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}