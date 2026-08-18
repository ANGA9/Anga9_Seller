package com.anga9.seller.ui.b2b

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.anga9.seller.data_models.RepeatBuyer
import com.anga9.seller.utils.UiState
import com.google.android.material.chip.Chip

class RepeatBuyersActivity : BaseActivity() {

    private val viewModel: B2BViewModel by viewModels()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvBuyers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTotalOutstanding: TextView
    private lateinit var tvTotalBuyers: TextView
    private lateinit var tvRegularBuyers: TextView
    private lateinit var chipAll: Chip
    private lateinit var chipOutstanding: Chip
    private lateinit var chipRegular: Chip
    private lateinit var adapter: RepeatBuyerAdapter

    private var allBuyers = listOf<RepeatBuyer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repeat_buyers)
        initViews()
        setupRecyclerView()
        setupObservers()
        loadData()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvBuyers = findViewById(R.id.rvBuyers)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        tvTotalOutstanding = findViewById(R.id.tvTotalOutstanding)
        tvTotalBuyers = findViewById(R.id.tvTotalBuyers)
        tvRegularBuyers = findViewById(R.id.tvRegularBuyers)
        chipAll = findViewById(R.id.chipAll)
        chipOutstanding = findViewById(R.id.chipOutstanding)
        chipRegular = findViewById(R.id.chipRegular)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        swipeRefresh.setOnRefreshListener { loadData() }
        chipAll.setOnClickListener { applyFilter(BuyerFilter.ALL) }
        chipOutstanding.setOnClickListener { applyFilter(BuyerFilter.WITH_OUTSTANDING) }
        chipRegular.setOnClickListener { applyFilter(BuyerFilter.REGULAR) }
    }

    private fun setupRecyclerView() {
        adapter = RepeatBuyerAdapter(
            onBuyerClick = { buyer -> showBuyerOrders(buyer) },
            onMarkPaidClick = { buyer -> showMarkPaidDialog(buyer) }
        )
        rvBuyers.layoutManager = LinearLayoutManager(this)
        rvBuyers.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.buyers.observe(this) { state ->
            swipeRefresh.isRefreshing = false
            when (state) {
                is UiState.Idle -> {}
                is UiState.Loading -> progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    allBuyers = state.data
                    updateSummaryCards(state.data)
                    applyFilter(viewModel.currentFilter)
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }

        viewModel.markPaidState.observe(this) { state ->
            when (state) {
                is UiState.Success -> showToast("Payment marked as received")
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }

        viewModel.buyerOrders.observe(this) { state ->
            when (state) {
                is UiState.Success -> showBuyerOrdersDialog(state.data)
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }
    }

    private fun loadData() {
        viewModel.loadBuyers(getSellerId())
    }

    private fun updateSummaryCards(buyers: List<RepeatBuyer>) {
        val totalOutstanding = buyers.sumOf { it.outstandingAmount }
        val regularCount = buyers.count { it.isRegular }
        tvTotalBuyers.text = buyers.size.toString()
        tvRegularBuyers.text = regularCount.toString()
        tvTotalOutstanding.text = "₹${String.format("%,.0f", totalOutstanding)}"
        chipOutstanding.text = "Outstanding (${buyers.count { it.outstandingAmount > 0 }})"
        chipRegular.text = "Regular (${regularCount})"
    }

    private fun applyFilter(filter: BuyerFilter) {
        viewModel.currentFilter = filter
        chipAll.isChecked = filter == BuyerFilter.ALL
        chipOutstanding.isChecked = filter == BuyerFilter.WITH_OUTSTANDING
        chipRegular.isChecked = filter == BuyerFilter.REGULAR
        val filtered = viewModel.getFilteredBuyers(allBuyers)
        adapter.submitList(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvBuyers.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showBuyerOrders(buyer: RepeatBuyer) {
        viewModel.loadBuyerOrders(getSellerId(), buyer.buyerId)
    }

    private fun showMarkPaidDialog(buyer: RepeatBuyer) {
        if (buyer.outstandingAmount <= 0) {
            showToast("No outstanding amount for this buyer")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Mark COD Payment Received")
            .setMessage(
                "Buyer: ${buyer.buyerName}\n" +
                "Outstanding: ₹${String.format("%,.0f", buyer.outstandingAmount)}\n\n" +
                "Mark last delivered COD order as paid?"
            )
            .setPositiveButton("Mark as Paid") { _, _ ->
                viewModel.markCodPaid(buyer.lastOrderId, getSellerId())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBuyerOrdersDialog(orders: List<Map<String, Any?>>) {
        if (orders.isEmpty()) {
            showToast("No orders found")
            return
        }
        val orderLines = orders.sortedByDescending {
            (it["createdAt"] as? Long) ?: 0L
        }.joinToString("\n\n") { order ->
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val date = sdf.format(java.util.Date((order["createdAt"] as? Long) ?: 0L))
            val amount = order["totalAmount"] as? Double ?: 0.0
            val status = order["orderStatus"] as? String ?: ""
            val payment = order["paymentMethod"] as? String ?: ""
            val paid = order["codPaymentReceived"] as? Boolean ?: false
            val paymentInfo = if (payment == "COD") {
                if (paid) "COD ✓ Paid" else "COD ⚠ Pending"
            } else "Online ✓"
            "• $date | ₹${String.format("%,.0f", amount)} | ${status.uppercase()} | $paymentInfo"
        }
        AlertDialog.Builder(this)
            .setTitle("Order History")
            .setMessage(orderLines)
            .setPositiveButton("Close", null)
            .show()
    }
}
