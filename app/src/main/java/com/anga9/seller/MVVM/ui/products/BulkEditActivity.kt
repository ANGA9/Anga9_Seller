package com.anga9.seller.MVVM.ui.products

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.MVVM.data.repository.ProductRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BulkEditActivity : AppCompatActivity() {

    private lateinit var viewModel: BulkEditViewModel
    private lateinit var priceAdapter: BulkEditPriceAdapter
    private lateinit var stockAdapter: BulkEditStockAdapter

    private lateinit var rvPrices: RecyclerView
    private lateinit var rvStock: RecyclerView
    private lateinit var tabPrices: View
    private lateinit var tabStock: View
    private lateinit var tvTabPricesLabel: TextView
    private lateinit var tvTabStockLabel: TextView
    private lateinit var indicatorPrices: View
    private lateinit var indicatorStock: View
    private lateinit var etSearch: EditText
    private lateinit var tvEditedCount: TextView
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    
    private var isPricesTabActive = true
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bulk_edit)

        val repository = ProductRepository(this)
        viewModel = ViewModelProvider(this)[BulkEditViewModel::class.java]
        viewModel.repository = repository

        initViews()
        setupTabs()
        setupSearch()
        setupRecyclerViews()
        setupObservers()

        viewModel.loadProducts()
    }

    private fun initViews() {
        rvPrices = findViewById(R.id.rvPrices)
        rvStock = findViewById(R.id.rvStock)
        tabPrices = findViewById(R.id.tabPrices)
        tabStock = findViewById(R.id.tabStock)
        tvTabPricesLabel = findViewById(R.id.tvTabPricesLabel)
        tvTabStockLabel = findViewById(R.id.tvTabStockLabel)
        indicatorPrices = findViewById(R.id.indicatorPrices)
        indicatorStock = findViewById(R.id.indicatorStock)
        etSearch = findViewById(R.id.etSearch)
        tvEditedCount = findViewById(R.id.tvEditedCount)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            viewModel.saveChanges()
        }
    }

    private fun setupTabs() {
        tabPrices.setOnClickListener {
            if (!isPricesTabActive) {
                isPricesTabActive = true
                updateTabUI()
            }
        }
        tabStock.setOnClickListener {
            if (isPricesTabActive) {
                isPricesTabActive = false
                updateTabUI()
            }
        }
    }

    private fun updateTabUI() {
        if (isPricesTabActive) {
            tvTabPricesLabel.setTextColor(Color.parseColor("#1D4ED8"))
            indicatorPrices.setBackgroundColor(Color.parseColor("#1D4ED8"))
            tvTabStockLabel.setTextColor(Color.parseColor("#6B7280"))
            indicatorStock.setBackgroundColor(Color.parseColor("#E5E7EB"))
            rvPrices.visibility = View.VISIBLE
            rvStock.visibility = View.GONE
        } else {
            tvTabStockLabel.setTextColor(Color.parseColor("#1D4ED8"))
            indicatorStock.setBackgroundColor(Color.parseColor("#1D4ED8"))
            tvTabPricesLabel.setTextColor(Color.parseColor("#6B7280"))
            indicatorPrices.setBackgroundColor(Color.parseColor("#E5E7EB"))
            rvPrices.visibility = View.GONE
            rvStock.visibility = View.VISIBLE
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                filterLists()
            }
        })
    }

    private fun filterLists() {
        val filtered = if (currentSearchQuery.isEmpty()) {
            viewModel.allProducts
        } else {
            viewModel.allProducts.filter { it.name.contains(currentSearchQuery, ignoreCase = true) }
        }
        priceAdapter.submitList(filtered)
        stockAdapter.submitList(filtered)
    }

    private fun setupRecyclerViews() {
        priceAdapter = BulkEditPriceAdapter(viewModel)
        rvPrices.adapter = priceAdapter
        rvPrices.itemAnimator = null 

        stockAdapter = BulkEditStockAdapter(viewModel)
        rvStock.adapter = stockAdapter
        rvStock.itemAnimator = null
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is BulkEditState.Loading -> {
                        progressBar.isVisible = true
                    }
                    is BulkEditState.Success -> {
                        progressBar.isVisible = false
                        filterLists()
                    }
                    is BulkEditState.Error -> {
                        progressBar.isVisible = false
                        Toast.makeText(this@BulkEditActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is BulkEditState.Saving -> {
                        btnSave.text = "Saving..."
                        btnSave.isEnabled = false
                    }
                    is BulkEditState.SaveSuccess -> {
                        btnSave.text = "Save changes"
                        Toast.makeText(this@BulkEditActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is BulkEditState.SaveError -> {
                        btnSave.text = "Save changes"
                        btnSave.isEnabled = true
                        Toast.makeText(this@BulkEditActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.editedCount.collectLatest { count ->
                tvEditedCount.text = "$count product(s) edited"
                if (count > 0 && viewModel.uiState.value !is BulkEditState.Saving) {
                    btnSave.isEnabled = true
                    btnSave.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1D4ED8"))
                } else {
                    btnSave.isEnabled = false
                    btnSave.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
                }
            }
        }
    }
}
