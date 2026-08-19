package com.anga9.seller.MVVM.ui.products

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.utils.UiState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MyProductsActivity : AppCompatActivity() {

    private val viewModel: ProductsViewModel by viewModels()

    // UI Components
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    
    // Header
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnNotifications: ImageView
    private lateinit var btnOverflowMenu: ImageView
    private lateinit var etSearch: EditText
    
    // Sub-header
    private lateinit var tvTotalCount: TextView
    private lateinit var btnQuality: View
    private lateinit var btnBulkUpload: View
    
    // Tabs
    private lateinit var tabAll: TextView
    private lateinit var tabActive: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabDrafts: TextView
    private lateinit var tabRejected: TextView
    
    // Empty State
    private lateinit var emptyStateContainer: View
    private lateinit var tvEmptyHeading: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var btnEmptyAction: Button
    
    // Offline Banner
    private lateinit var tvOfflineBanner: TextView
    
    private lateinit var fabAddProduct: FloatingActionButton

    private lateinit var adapter: SellerProductAdapter
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_products)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupTabs()
        setupFab()
        setupOverflowMenu()
        setupSubHeaderActions()
        observeViewModel()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        
        // Header
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnOverflowMenu = findViewById(R.id.btnOverflowMenu)
        etSearch = findViewById(R.id.etSearch)
        
        // Sub-header
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnQuality = findViewById(R.id.btnQuality)
        btnBulkUpload = findViewById(R.id.btnBulkUpload)
        
        // Tabs
        tabAll = findViewById(R.id.tabAll)
        tabActive = findViewById(R.id.tabActive)
        tabPending = findViewById(R.id.tabPending)
        tabDrafts = findViewById(R.id.tabDrafts)
        tabRejected = findViewById(R.id.tabRejected)
        
        // Empty State
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        tvEmptyHeading = findViewById(R.id.tvEmptyHeading)
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc)
        btnEmptyAction = findViewById(R.id.btnEmptyAction)
        
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        fabAddProduct = findViewById(R.id.fabAddProduct)

        btnBack.setOnClickListener { finish() }

        btnNotifications.setOnClickListener {
            startActivity(Intent(this, com.anga9.seller.ui.notifications.NotificationsActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadProducts(currentFilter)
        }
        
        btnEmptyAction.setOnClickListener {
            startActivity(Intent(this, AddProductWizardActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = SellerProductAdapter(
            products = emptyList(),
            onEditClick = { product ->
                val intent = Intent(this, EditProductActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            },
            onDeleteClick = { product -> showDeleteDialog(product) },
            onStockClick = { product -> showStockUpdateDialog(product) },
            onItemClick = { product -> showProductDetail(product) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                if (q.isNotEmpty()) viewModel.searchProducts(q)
                else viewModel.loadProducts(currentFilter)
            }
        })
    }

    private fun setupTabs() {
        val tabs = listOf(tabAll, tabActive, tabPending, tabDrafts, tabRejected)
        // Backend maps Active to "approved"
        val filters = listOf("all", "active", "pending", "draft", "rejected")

        fun updateTabColors(selectedIndex: Int) {
            tabs.forEachIndexed { index, tab ->
                if (index == selectedIndex) {
                    tab.setBackgroundResource(R.drawable.shape_pill_selected)
                    tab.setTextColor(Color.WHITE)
                    tab.setTypeface(null, Typeface.BOLD)
                } else {
                    tab.setBackgroundResource(R.drawable.shape_pill_unselected)
                    tab.setTextColor(Color.parseColor("#5B6472"))
                    tab.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                currentFilter = filters[index]
                updateTabColors(index)
                updateEmptyStateTexts(currentFilter)
                viewModel.loadProducts(currentFilter)
            }
        }
        updateTabColors(0) // Default: All selected
    }

    private fun updateEmptyStateTexts(filter: String) {
        when (filter) {
            "all" -> {
                tvEmptyHeading.text = "No products yet"
                tvEmptyDesc.text = "Start by adding your first product. Once approved, it will be listed on the ANGA9 marketplace."
                btnEmptyAction.visibility = View.VISIBLE
            }
            "active" -> {
                tvEmptyHeading.text = "No active products"
                tvEmptyDesc.text = "Products that have been approved will appear here."
                btnEmptyAction.visibility = View.GONE
            }
            "pending" -> {
                tvEmptyHeading.text = "No products pending review"
                tvEmptyDesc.text = "Products awaiting approval by the ANGA9 team will appear here."
                btnEmptyAction.visibility = View.GONE
            }
            "draft" -> {
                tvEmptyHeading.text = "No drafts"
                tvEmptyDesc.text = "Products you started but haven't published yet will appear here."
                btnEmptyAction.visibility = View.GONE
            }
            "rejected" -> {
                tvEmptyHeading.text = "No rejected products"
                tvEmptyDesc.text = "Products that did not meet the guidelines will appear here."
                btnEmptyAction.visibility = View.GONE
            }
        }
    }

    private fun setupOverflowMenu() {
        btnOverflowMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Bulk Edit")
            popup.menu.add(0, 2, 0, "Add Brand")
            popup.menu.add(0, 3, 0, "Export list")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(Intent(this@MyProductsActivity, BulkEditActivity::class.java))
                        true
                    }
                    2 -> {
                        Toast.makeText(this, "Add Brand clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> {
                        Toast.makeText(this, "Export list clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    private fun setupSubHeaderActions() {
        btnQuality.setOnClickListener {
            Toast.makeText(this, "Quality insights coming soon", Toast.LENGTH_SHORT).show()
        }
        btnBulkUpload.setOnClickListener {
            startActivity(Intent(this, BulkUploadActivity::class.java))
        }
    }

    private fun setupFab() {
        fabAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductWizardActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.productsState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                if (!swipeRefresh.isRefreshing) progressBar.visibility = View.VISIBLE
                                emptyStateContainer.visibility = View.GONE
                                recyclerView.visibility = View.GONE
                            }
                            is UiState.Success -> {
                                progressBar.visibility = View.GONE
                                swipeRefresh.isRefreshing = false
                                val sellerProducts = state.data.map { r ->
                                    SellerProduct(
                                        id = r.id,
                                        name = r.name,
                                        description = r.description ?: "",
                                        price = r.basePrice ?: r.mrp ?: r.price,
                                        wholesalePrice = r.salePrice ?: r.price,
                                        imageUrl = r.images?.firstOrNull() ?: r.imageUrl ?: "",
                                        imageUrls = r.images ?: emptyList(),
                                        category = r.category ?: "",
                                        subcategory = r.subcategory ?: "",
                                        stock = r.stock,
                                        gstPercent = r.gstRate?.toInt() ?: 5,
                                        hsnCode = r.hsnCode ?: "",
                                        status = r.status,
                                        isActive = r.isActive,
                                        moq = r.minOrderQty ?: 1,
                                        unit = r.unit ?: "piece",
                                        createdAt = try {
                                            if (!r.createdAt.isNullOrEmpty()) {
                                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(r.createdAt)?.time ?: 0L
                                            } else 0L
                                        } catch(e: Exception) { 0L }
                                    )
                                }
                                adapter.updateProducts(sellerProducts)
                                tvTotalCount.text = "${state.data.size} products"
                                
                                if (state.data.isEmpty()) {
                                    emptyStateContainer.visibility = View.VISIBLE
                                    recyclerView.visibility = View.GONE
                                } else {
                                    emptyStateContainer.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                }
                            }
                            is UiState.Error -> {
                                progressBar.visibility = View.GONE
                                swipeRefresh.isRefreshing = false
                                Toast.makeText(this@MyProductsActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.deleteProductState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(this@MyProductsActivity, "Product deleted", Toast.LENGTH_SHORT).show()
                                viewModel.resetDeleteState()
                                viewModel.loadProducts(currentFilter)
                            }
                            is UiState.Error -> {
                                Toast.makeText(this@MyProductsActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetDeleteState()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.updateStockState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(this@MyProductsActivity, "Stock updated", Toast.LENGTH_SHORT).show()
                                viewModel.resetStockState()
                                viewModel.loadProducts(currentFilter)
                            }
                            is UiState.Error -> {
                                Toast.makeText(this@MyProductsActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetStockState()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteDialog(product: SellerProduct) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Delete '${product.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProduct(product.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStockUpdateDialog(product: SellerProduct) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_stock, null)
        val etStock = dialogView.findViewById<EditText>(R.id.etNewStock)
        val tvCurrentStock = dialogView.findViewById<TextView>(R.id.tvCurrentStock)

        tvCurrentStock.text = "Current Stock: ${product.stock} ${product.unit}"
        etStock.setText(product.stock.toString())

        AlertDialog.Builder(this)
            .setTitle("Update Stock - ${product.name}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newStock = etStock.text.toString().trim().toIntOrNull()
                if (newStock == null || newStock < 0) {
                    Toast.makeText(this, "Enter valid stock quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateStock(product.id, newStock)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProductDetail(product: SellerProduct) {
        val msg = buildString {
            appendLine("Name: ${product.name}")
            appendLine("Price: ₹${product.price} / ${product.unit}")
            appendLine("Stock: ${product.stock}")
            appendLine("MOQ: ${product.moq}")
            appendLine("GST: ${product.gstPercent}%")
            appendLine("SKU: ${product.sku.ifEmpty { "N/A" }}")
            appendLine("Status: ${product.status.uppercase()}")
            if (product.bulkPricing.isNotEmpty()) {
                appendLine("\nBulk Pricing:")
                product.bulkPricing.forEach { tier ->
                    appendLine("  ${tier.minQuantity}-${tier.maxQuantity} units: ₹${tier.pricePerUnit}")
                }
            }
            if (product.rejectionReason.isNotEmpty()) {
                appendLine("\nRejection Reason: ${product.rejectionReason}")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(product.name)
            .setMessage(msg)
            .setPositiveButton("Edit") { _, _ ->
                val intent = Intent(this, EditProductActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (!isTaskRoot) {
            viewModel.loadProducts(currentFilter)
        }
    }
}