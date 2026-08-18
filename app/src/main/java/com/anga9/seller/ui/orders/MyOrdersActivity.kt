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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerOrderResponse
import com.anga9.seller.utils.Resource

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)
        
        initViews()
        setupTabs()
        setupRecyclerView()
        setupSearch()
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
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
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
        viewModel.filteredOrders.observe(this) { result ->
            swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val size = result.data?.size ?: 0
                    android.util.Log.d("MyOrdersActivity", "Submitting list of size: $size")
                    if (result.data.isNullOrEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        adapter.submitList(emptyList())
                    } else {
                        tvEmpty.visibility = View.GONE
                        val newList = result.data?.toMutableList()
                        adapter.submitList(newList) {
                            android.util.Log.d("MyOrdersActivity", "submitList completed! Current adapter size: ${adapter.currentList.size}")
                            if (adapter.currentList.isNotEmpty()) {
                                rvOrders.post {
                                    adapter.notifyDataSetChanged()
                                    swipeRefresh.requestLayout()
                                    rvOrders.scrollToPosition(0)
                                }
                                rvOrders.postDelayed({
                                    if (rvOrders.childCount == 0 && adapter.itemCount > 0) {
                                        adapter.notifyDataSetChanged()
                                        swipeRefresh.requestLayout()
                                    }
                                }, 250)
                            }
                        }
                        android.util.Log.d("MyOrdersActivity", "submitList called")
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}