package com.anga9.seller.ui.inventory

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.MVVM.ui.products.AddProductWizardActivity
import com.anga9.seller.R
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.utils.UiState
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {

    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var adapter: InventoryAdapter

    private lateinit var appBarLayout: AppBarLayout
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seller_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners(view)
        observeViewModel()

        viewModel.fetchInventory()
    }

    

    private fun initViews(root: View) {
        appBarLayout = root.findViewById(R.id.appBarLayout)
        swipeRefresh = root.findViewById(R.id.swipeRefresh)
        rvInventory = root.findViewById(R.id.rvInventory)
        etSearch = root.findViewById(R.id.etSearch)
        btnClearSearch = root.findViewById(R.id.btnClearSearch)
        tvActiveFilter = root.findViewById(R.id.tvActiveFilter)

        cardTotal = root.findViewById(R.id.cardTotal)
        cardLowStock = root.findViewById(R.id.cardLowStock)
        cardOutOfStock = root.findViewById(R.id.cardOutOfStock)

        tvTotalValue = root.findViewById(R.id.tvTotalValue)
        tvLowStockValue = root.findViewById(R.id.tvLowStockValue)
        tvOutOfStockValue = root.findViewById(R.id.tvOutOfStockValue)

        layoutEmptyState = root.findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = root.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = root.findViewById(R.id.tvEmptyDesc)
        btnEmptyAction = root.findViewById(R.id.btnEmptyAction)
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter { row ->
            showEditStockBottomSheet(row)
        }
        rvInventory.layoutManager = LinearLayoutManager(requireContext())
        rvInventory.adapter = adapter
    }

    private fun setupListeners(root: View) {
        root.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            (activity as? DashboardActivity)?.switchToTab(R.id.nav_home)
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchInventory()
        }

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
            startActivity(Intent(requireContext(), AddProductWizardActivity::class.java))
        }
    }

    private fun updateFilterUi() {
        val density = resources.displayMetrics.density
        val strokeSelected = (2 * density).toInt()
        val strokeNormal = (1 * density).toInt()
        val defaultBorder = Color.parseColor("#E2E8F0")

        when (activeFilter) {
            StockFilter.ALL -> {
                tvActiveFilter.visibility = View.GONE
                cardTotal.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A6FD4")))
                cardTotal.strokeWidth = strokeSelected
                cardTotal.cardElevation = 2 * density

                cardLowStock.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardLowStock.strokeWidth = strokeNormal
                cardLowStock.cardElevation = 0f

                cardOutOfStock.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardOutOfStock.strokeWidth = strokeNormal
                cardOutOfStock.cardElevation = 0f
            }
            StockFilter.LOW_STOCK -> {
                tvActiveFilter.visibility = View.VISIBLE
                tvActiveFilter.text = "Showing Low Stock products • Tap to clear"
                cardTotal.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardTotal.strokeWidth = strokeNormal
                cardTotal.cardElevation = 0f

                cardLowStock.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#D97706")))
                cardLowStock.strokeWidth = strokeSelected
                cardLowStock.cardElevation = 2 * density

                cardOutOfStock.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardOutOfStock.strokeWidth = strokeNormal
                cardOutOfStock.cardElevation = 0f
            }
            StockFilter.OUT_OF_STOCK -> {
                tvActiveFilter.visibility = View.VISIBLE
                tvActiveFilter.text = "Showing Out of Stock products • Tap to clear"
                cardTotal.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardTotal.strokeWidth = strokeNormal
                cardTotal.cardElevation = 0f

                cardLowStock.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultBorder))
                cardLowStock.strokeWidth = strokeNormal
                cardLowStock.cardElevation = 0f

                cardOutOfStock.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")))
                cardOutOfStock.strokeWidth = strokeSelected
                cardOutOfStock.cardElevation = 2 * density
            }
        }
    }

    private fun applyFilters() {
        val currentState = viewModel.uiState.value
        if (currentState is UiState.Success) {
            val allRows = currentState.data ?: emptyList()
            var filtered = allRows

            if (searchQuery.isNotEmpty()) {
                filtered = filtered.filter {
                    it.product.name.contains(searchQuery, ignoreCase = true)
                }
            }

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
            btnEmptyAction.text = "+ Add Product"
            btnEmptyAction.visibility = View.VISIBLE
            btnEmptyAction.setOnClickListener {
                startActivity(Intent(requireContext(), AddProductWizardActivity::class.java))
            }
        } else if (isFilterEmpty) {
            rvInventory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            tvEmptyTitle.text = "No products found"
            if (searchQuery.isNotEmpty()) {
                tvEmptyDesc.text = "No products matching \"$searchQuery\""
            } else {
                tvEmptyDesc.text = "No products match the selected filter."
            }
            btnEmptyAction.text = "Clear Filter"
            btnEmptyAction.visibility = View.VISIBLE
            btnEmptyAction.setOnClickListener {
                activeFilter = StockFilter.ALL
                etSearch.setText("")
                updateFilterUi()
                applyFilters()
            }
        } else {
            rvInventory.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statSummary.collectLatest { stats ->
                tvTotalValue.text = stats.totalProducts.toString()
                tvLowStockValue.text = stats.lowStock.toString()
                tvOutOfStockValue.text = stats.outOfStock.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        swipeRefresh.isRefreshing = true
                    }
                    is UiState.Success -> {
                        swipeRefresh.isRefreshing = false
                        applyFilters()
                    }
                    is UiState.Error -> {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateStockState.collectLatest { state ->
                if (state is UiState.Error) {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                } else if (state is UiState.Success) {
                    Toast.makeText(context, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditStockBottomSheet(row: InventoryRow) {
        val ctx = context ?: return
        val bottomSheet = BottomSheetDialog(ctx)
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

    fun refreshInventory() {
        if (isAdded) {
            viewModel.fetchInventory()
        }
    }
}
