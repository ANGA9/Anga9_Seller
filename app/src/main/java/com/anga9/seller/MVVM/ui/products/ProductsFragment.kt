package com.anga9.seller.MVVM.ui.products

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.ui.dashboard.DashboardActivity
import com.anga9.seller.ui.notifications.NotificationsActivity
import com.anga9.seller.utils.UiState
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ProductsFragment : Fragment() {

    private val viewModel: ProductsViewModel by viewModels()

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    // Header views
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnNotifications: ImageView
    private lateinit var btnOverflowMenu: ImageView
    private lateinit var etSearch: EditText

    // Sub-header views
    private lateinit var tvTotalCount: TextView
    private lateinit var btnQuality: TextView
    private lateinit var btnBulkUpload: TextView

    // Filter tabs
    private lateinit var tabAll: TextView
    private lateinit var tabActive: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabDrafts: TextView
    private lateinit var tabRejected: TextView

    // Empty state
    private lateinit var emptyStateContainer: View
    private lateinit var tvEmptyHeading: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var btnEmptyAction: Button

    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var adapter: SellerProductAdapter
    private var currentFilter = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seller_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupInsets()
        setupRecyclerView()
        setupSearch()
        setupTabs()
        setupFab()
        setupOverflowMenu()
        setupSubHeaderActions()
        observeViewModel()

        viewModel.loadProducts(currentFilter)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, statusBar, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(appBarLayout)
    }

    private fun initViews(root: View) {
        appBarLayout = root.findViewById(R.id.appBarLayout)
        recyclerView = root.findViewById(R.id.recyclerViewProducts)
        swipeRefresh = root.findViewById(R.id.swipeRefresh)
        progressBar = root.findViewById(R.id.progressBar)

        // Header
        tvTitle = root.findViewById(R.id.tvTitle)
        btnBack = root.findViewById(R.id.btnBack)
        btnNotifications = root.findViewById(R.id.btnNotifications)
        btnOverflowMenu = root.findViewById(R.id.btnOverflowMenu)
        etSearch = root.findViewById(R.id.etSearch)

        // Sub-header
        tvTotalCount = root.findViewById(R.id.tvTotalCount)
        btnQuality = root.findViewById(R.id.btnQuality)
        btnBulkUpload = root.findViewById(R.id.btnBulkUpload)

        // Tabs
        tabAll = root.findViewById(R.id.tabAll)
        tabActive = root.findViewById(R.id.tabActive)
        tabPending = root.findViewById(R.id.tabPending)
        tabDrafts = root.findViewById(R.id.tabDrafts)
        tabRejected = root.findViewById(R.id.tabRejected)

        // Empty State
        emptyStateContainer = root.findViewById(R.id.emptyStateContainer)
        tvEmptyHeading = root.findViewById(R.id.tvEmptyHeading)
        tvEmptyDesc = root.findViewById(R.id.tvEmptyDesc)
        btnEmptyAction = root.findViewById(R.id.btnEmptyAction)

        fabAddProduct = root.findViewById(R.id.fabAddProduct)

        btnBack.setOnClickListener {
            (activity as? DashboardActivity)?.switchToTab(R.id.nav_home)
        }

        btnNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadProducts(currentFilter)
        }

        btnEmptyAction.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductWizardActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = SellerProductAdapter(
            products = emptyList(),
            onEditClick = { product ->
                val intent = Intent(requireContext(), EditProductActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            },
            onDeleteClick = { product -> showDeleteDialog(product) },
            onStockClick = { product -> showStockUpdateDialog(product) },
            onItemClick = { product -> showProductDetail(product) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
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
        updateTabColors(0)
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
            val popup = PopupMenu(requireContext(), view)
            popup.menu.add(0, 1, 0, "Bulk Edit")
            popup.menu.add(0, 2, 0, "Add Brand")
            popup.menu.add(0, 3, 0, "Export list")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(Intent(requireContext(), BulkEditActivity::class.java))
                        true
                    }
                    2 -> {
                        Toast.makeText(context, "Add Brand feature coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> {
                        Toast.makeText(context, "Export list coming soon", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Quality insights coming soon", Toast.LENGTH_SHORT).show()
        }
        btnBulkUpload.setOnClickListener {
            startActivity(Intent(requireContext(), BulkUploadActivity::class.java))
        }
    }

    private fun setupFab() {
        fabAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductWizardActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                                        } catch (e: Exception) { 0L }
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
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.deleteProductState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                                viewModel.resetDeleteState()
                                viewModel.loadProducts(currentFilter)
                            }
                            is UiState.Error -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show()
                                viewModel.resetStockState()
                                viewModel.loadProducts(currentFilter)
                            }
                            is UiState.Error -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
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
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("Delete Product")
            .setMessage("Delete '${product.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProduct(product.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStockUpdateDialog(product: SellerProduct) {
        val ctx = context ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_stock, null)
        val etStock = dialogView.findViewById<EditText>(R.id.etNewStock)
        val tvCurrentStock = dialogView.findViewById<TextView>(R.id.tvCurrentStock)

        tvCurrentStock.text = "Current Stock: ${product.stock} ${product.unit}"
        etStock.setText(product.stock.toString())

        AlertDialog.Builder(ctx)
            .setTitle("Update Stock - ${product.name}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newStock = etStock.text.toString().trim().toIntOrNull()
                if (newStock == null || newStock < 0) {
                    Toast.makeText(ctx, "Enter valid stock quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateStock(product.id, newStock)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProductDetail(product: SellerProduct) {
        val ctx = context ?: return
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
        AlertDialog.Builder(ctx)
            .setTitle(product.name)
            .setMessage(msg)
            .setPositiveButton("Edit") { _, _ ->
                val intent = Intent(ctx, EditProductActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    fun refreshProducts() {
        if (::viewModel.isInitialized) {
            viewModel.loadProducts(currentFilter)
        }
    }
}
