package com.anga9.seller.ui.inventory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.MVVM.ui.products.AddProductWizardActivity
import com.anga9.seller.R
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryActivity : AppCompatActivity() {

    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var adapter: InventoryAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvInventory: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var tvActiveFilter: TextView

    private lateinit var cardTotal: MaterialCardView
    private lateinit var cardLowStock: MaterialCardView
    private lateinit var cardOutOfStock: MaterialCardView
    
    private lateinit var tvTotalValue: TextView
    private lateinit var tvLowStockValue: TextView
    private lateinit var tvOutOfStockValue: TextView

    private lateinit var layoutEmptyState: MaterialCardView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var btnEmptyAction: MaterialButton

    private var activeFilter: StockFilter = StockFilter.ALL
    private var searchQuery: String = ""

    enum class StockFilter { ALL, LOW_STOCK, OUT_OF_STOCK }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.fetchInventory()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvInventory = findViewById(R.id.rvInventory)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        tvActiveFilter = findViewById(R.id.tvActiveFilter)

        cardTotal = findViewById(R.id.cardTotal)
        cardLowStock = findViewById(R.id.cardLowStock)
        cardOutOfStock = findViewById(R.id.cardOutOfStock)

        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvLowStockValue = findViewById(R.id.tvLowStockValue)
        tvOutOfStockValue = findViewById(R.id.tvOutOfStockValue)

        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc)
        btnEmptyAction = findViewById(R.id.btnEmptyAction)
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter { row ->
            showEditStockBottomSheet(row)
        }
        rvInventory.layoutManager = LinearLayoutManager(this)
        rvInventory.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchInventory()
        }

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        // Filters
        cardTotal.setOnClickListener {
            activeFilter = StockFilter.ALL
            updateFilterUi()
            applyFilters()
        }

        cardLowStock.setOnClickListener {
            activeFilter = if (activeFilter == StockFilter.LOW_STOCK) StockFilter.ALL else StockFilter.LOW_STOCK
            updateFilterUi()
            applyFilters()
        }

        cardOutOfStock.setOnClickListener {
            activeFilter = if (activeFilter == StockFilter.OUT_OF_STOCK) StockFilter.ALL else StockFilter.OUT_OF_STOCK
            updateFilterUi()
            applyFilters()
        }

        tvActiveFilter.setOnClickListener {
            activeFilter = StockFilter.ALL
            updateFilterUi()
            applyFilters()
        }

        btnEmptyAction.setOnClickListener {
            startActivity(Intent(this, AddProductWizardActivity::class.java))
        }
    }

    private fun updateFilterUi() {
        when (activeFilter) {
            StockFilter.ALL -> {
                tvActiveFilter.visibility = View.GONE
                cardTotal.strokeWidth = 2
                cardLowStock.strokeWidth = 1
                cardOutOfStock.strokeWidth = 1
            }
            StockFilter.LOW_STOCK -> {
                tvActiveFilter.visibility = View.VISIBLE
                tvActiveFilter.text = "Showing Low Stock products • Tap to clear"
                cardTotal.strokeWidth = 1
                cardLowStock.strokeWidth = 3
                cardOutOfStock.strokeWidth = 1
            }
            StockFilter.OUT_OF_STOCK -> {
                tvActiveFilter.visibility = View.VISIBLE
                tvActiveFilter.text = "Showing Out of Stock products • Tap to clear"
                cardTotal.strokeWidth = 1
                cardLowStock.strokeWidth = 1
                cardOutOfStock.strokeWidth = 3
            }
        }
    }

    private fun applyFilters() {
        val currentState = viewModel.uiState.value
        if (currentState is UiState.Success) {
            val allRows = currentState.data ?: emptyList()
            var filtered = allRows

            // 1. Search Filter
            if (searchQuery.isNotEmpty()) {
                filtered = filtered.filter { 
                    it.product.name.contains(searchQuery, ignoreCase = true) 
                }
            }

            // 2. Status Filter
            filtered = when (activeFilter) {
                StockFilter.LOW_STOCK -> filtered.filter { 
                    val qty = it.stock?.effectiveQuantity ?: it.stock?.quantity ?: it.stock?.stock ?: 0
                    val threshold = it.stock?.lowStockThreshold ?: 10
                    qty in 1..threshold 
                }
                StockFilter.OUT_OF_STOCK -> filtered.filter { 
                    val qty = it.stock?.effectiveQuantity ?: it.stock?.quantity ?: it.stock?.stock ?: 0
                    qty <= 0 
                }
                StockFilter.ALL -> filtered
            }

            adapter.submitList(filtered)
            handleEmptyState(allRows.isEmpty(), filtered.isEmpty())
        }
    }

    private fun handleEmptyState(isTrueEmpty: Boolean, isFilterEmpty: Boolean) {
        if (isTrueEmpty) {
            rvInventory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            tvEmptyTitle.text = "No products found"
            tvEmptyDesc.text = "You haven't added any products to manage inventory."
            btnEmptyAction.visibility = View.VISIBLE
        } else if (isFilterEmpty) {
            rvInventory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            tvEmptyTitle.text = "No products found"
            if (searchQuery.isNotEmpty()) {
                tvEmptyDesc.text = "No products matching \"$searchQuery\""
            } else {
                tvEmptyDesc.text = "No products match the selected filter."
            }
            btnEmptyAction.visibility = View.GONE
        } else {
            rvInventory.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.statSummary.collectLatest { stats ->
                tvTotalValue.text = stats.totalProducts.toString()
                tvLowStockValue.text = stats.lowStock.toString()
                tvOutOfStockValue.text = stats.outOfStock.toString()
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        swipeRefresh.isRefreshing = true
                    }
                    is UiState.Success -> {
                        swipeRefresh.isRefreshing = false
                        applyFilters() // Re-apply current filters to new data
                    }
                    is UiState.Error -> {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@InventoryActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateStockState.collectLatest { state ->
                if (state is UiState.Error) {
                    Toast.makeText(this@InventoryActivity, state.message, Toast.LENGTH_SHORT).show()
                } else if (state is UiState.Success) {
                    Toast.makeText(this@InventoryActivity, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditStockBottomSheet(row: InventoryRow) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_stock, null)
        bottomSheet.setContentView(view)

        val tvProductName = view.findViewById<TextView>(R.id.tvProductName)
        val etStock = view.findViewById<EditText>(R.id.etStock)
        val etThreshold = view.findViewById<EditText>(R.id.etThreshold)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)

        tvProductName.text = row.product.name
        val currentQty = row.stock?.effectiveQuantity ?: row.stock?.quantity ?: row.stock?.stock ?: 0
        etStock.setText(currentQty.toString())
        etThreshold.setText(row.stock?.lowStockThreshold?.toString() ?: "10")

        btnClose.setOnClickListener { bottomSheet.dismiss() }

        btnSave.setOnClickListener {
            val stock = etStock.text.toString().toIntOrNull() ?: 0
            val threshold = etThreshold.text.toString().toIntOrNull() ?: 10
            
            viewModel.updateStock(row.product.id, stock, threshold)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }
}
