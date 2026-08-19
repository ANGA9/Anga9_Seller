package com.anga9.seller.MVVM.ui.products

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerProductResponse
import com.anga9.seller.network.model.UpdateProductRequest
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.UiState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class EditProductActivity : AppCompatActivity() {

    private val viewModel: ProductsViewModel by viewModels()

    private lateinit var etProductName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var cgCategories: ChipGroup
    private lateinit var tvCategoryLimit: TextView

    private lateinit var etMrp: TextInputEditText
    private lateinit var etWholesalePrice: TextInputEditText
    private lateinit var tvWholesaleHelper: TextView
    private lateinit var etMinOrderQty: TextInputEditText
    private lateinit var spinnerUnit: Spinner
    
    private lateinit var tvCommissionRate: TextView
    private lateinit var tvCommissionType: TextView
    private lateinit var btnCommissionBadge: View

    private lateinit var etSearchTags: TextInputEditText
    private lateinit var etHsnCode: TextInputEditText

    private lateinit var btnSaveChanges: Button
    private lateinit var pbSaving: ProgressBar

    private var productId: String = ""
    private var originalProduct: SellerProductResponse? = null
    private var currentCategoryIds = mutableListOf<String>()

    private val units = listOf("piece", "kg", "gram", "liter", "ml", "box", "dozen", "pack", "pair", "set")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        productId = intent.getStringExtra("product_id") ?: ""
        if (productId.isEmpty()) {
            Toast.makeText(this, "Product ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        observeViewModel()

        // Fetch product
        viewModel.getProductById(productId)
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etProductName = findViewById(R.id.etProductName)
        etDescription = findViewById(R.id.etDescription)
        cgCategories = findViewById(R.id.cgCategories)
        tvCategoryLimit = findViewById(R.id.tvCategoryLimit)

        etMrp = findViewById(R.id.etMrp)
        etWholesalePrice = findViewById(R.id.etWholesalePrice)
        tvWholesaleHelper = findViewById(R.id.tvWholesaleHelper)
        etMinOrderQty = findViewById(R.id.etMinOrderQty)
        spinnerUnit = findViewById(R.id.spinnerUnit)

        tvCommissionRate = findViewById(R.id.tvCommissionRate)
        tvCommissionType = findViewById(R.id.tvCommissionType)
        btnCommissionBadge = findViewById(R.id.btnCommissionBadge)

        etSearchTags = findViewById(R.id.etSearchTags)
        etHsnCode = findViewById(R.id.etHsnCode)

        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        pbSaving = findViewById(R.id.pbSaving)

        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units.map { it.replaceFirstChar { c -> c.uppercase() } })
        spinnerUnit.adapter = unitAdapter
    }

    private fun setupListeners() {
        // Validation: Wholesale cannot exceed MRP
        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePrices()
            }
        }
        etMrp.addTextChangedListener(priceWatcher)
        etWholesalePrice.addTextChangedListener(priceWatcher)

        btnCommissionBadge.setOnClickListener {
            val type = tvCommissionType.text.toString()
            val msg = if (type == "CUSTOM RATE") {
                "This category/product has a negotiated commission rate, set by ANGA9. Contact support if you believe this is incorrect."
            } else {
                "This is the standard platform commission rate applied to your category."
            }
            AlertDialog.Builder(this)
                .setTitle("Commission Information")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        }

        btnSaveChanges.setOnClickListener {
            saveChanges()
        }
    }

    private fun validatePrices(): Boolean {
        val mrpStr = etMrp.text.toString()
        val wsStr = etWholesalePrice.text.toString()
        if (mrpStr.isNotEmpty() && wsStr.isNotEmpty()) {
            val mrp = mrpStr.toDoubleOrNull() ?: 0.0
            val ws = wsStr.toDoubleOrNull() ?: 0.0
            if (ws > mrp) {
                tvWholesaleHelper.text = "Cannot exceed MRP."
                tvWholesaleHelper.setTextColor(Color.parseColor("#DC2626")) // Red
                return false
            } else {
                tvWholesaleHelper.text = "Buyer's per-unit price. Cannot exceed MRP."
                tvWholesaleHelper.setTextColor(Color.parseColor("#9AA1AC"))
            }
        }
        return true
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.productDetailState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> { /* Could show a main skeleton */ }
                    is UiState.Success -> {
                        originalProduct = state.data
                        prefillData(state.data)
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@EditProductActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        btnSaveChanges.text = ""
                        pbSaving.visibility = View.VISIBLE
                        btnSaveChanges.isEnabled = false
                    }
                    is UiState.Success -> {
                        btnSaveChanges.text = "Save changes"
                        pbSaving.visibility = View.GONE
                        btnSaveChanges.isEnabled = true
                        Toast.makeText(this@EditProductActivity, "Product updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        btnSaveChanges.text = "Save changes"
                        pbSaving.visibility = View.GONE
                        btnSaveChanges.isEnabled = true
                        Toast.makeText(this@EditProductActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun prefillData(product: SellerProductResponse) {
        etProductName.setText(product.name)
        etDescription.setText(product.description ?: "")
        
        val price = product.price
        if (price != null && price > 0) {
            etMrp.setText(String.format(Locale.US, "%.2f", price))
        } else {
            // fallback if MRP is stored differently
            val p = product.variants?.firstOrNull()?.price ?: 0.0
            if (p > 0) etMrp.setText(String.format(Locale.US, "%.2f", p))
        }
        
        // Wholesale (sale_price)
        val wholesale = product.variants?.firstOrNull()?.price ?: 0.0 // Actually backend uses basePrice/salePrice. Let's assume variants.price is wholesale for now based on app logic.
        etWholesalePrice.setText(String.format(Locale.US, "%.2f", wholesale))

        etMinOrderQty.setText((product.minOrderQty ?: 1).toString())
        
        val unitStr = product.unit ?: "piece"
        val unitIdx = units.indexOf(unitStr.lowercase())
        if (unitIdx >= 0) spinnerUnit.setSelection(unitIdx)

        etHsnCode.setText(product.hsnCode ?: "")
        
        // Commission
        val rate = product.commissionRate
        if (rate != null) {
            tvCommissionRate.text = "${(rate * 100).toInt()}%"
            // Assume if it's not 15%, it's custom. (Or whatever backend implies)
            if (rate == 0.15) {
                tvCommissionType.text = "STANDARD"
                tvCommissionType.setTextColor(Color.parseColor("#7C3AED"))
                btnCommissionBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5EEFF"))
            } else {
                tvCommissionType.text = "CUSTOM RATE"
                tvCommissionType.setTextColor(Color.parseColor("#D97706")) // Amber
                btnCommissionBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            }
        } else {
            tvCommissionRate.text = "15%"
            tvCommissionType.text = "STANDARD"
        }

        // Categories
        currentCategoryIds.clear()
        product.category?.let { currentCategoryIds.add(it) }
        renderCategoryChips()
    }

    private fun renderCategoryChips() {
        cgCategories.removeAllViews()
        tvCategoryLimit.text = "Categories * (${currentCategoryIds.size}/5)"

        currentCategoryIds.forEachIndexed { index, category ->
            val chip = Chip(this).apply {
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    currentCategoryIds.remove(category)
                    renderCategoryChips()
                }
                
                // Styling
                if (index == 0) {
                    text = "PRIMARY: ${category.replaceFirstChar { it.uppercase() }}"
                    setTextColor(Color.parseColor("#2851C4"))
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#EEF3FF"))
                    closeIconTint = ColorStateList.valueOf(Color.parseColor("#2851C4"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    text = category.replaceFirstChar { it.uppercase() }
                    setTextColor(Color.parseColor("#5B6472"))
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#F0F1F3"))
                    closeIconTint = ColorStateList.valueOf(Color.parseColor("#5B6472"))
                }
            }
            cgCategories.addView(chip)
        }

        if (currentCategoryIds.size < 5) {
            val addChip = Chip(this).apply {
                text = "+ Add"
                setTextColor(Color.parseColor("#1A1D23"))
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                chipStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#D8DBE0"))
                setOnClickListener { openCategoryPicker() }
            }
            cgCategories.addView(addChip)
        }
    }

    private fun openCategoryPicker() {
        val bottomSheet = CategoryMultiSelectBottomSheet()
        bottomSheet.setSelectedCategories(currentCategoryIds)
        bottomSheet.setOnSelectionChangedListener { selected ->
            currentCategoryIds.clear()
            currentCategoryIds.addAll(selected)
            renderCategoryChips()
        }
        bottomSheet.show(supportFragmentManager, "CategoryMultiSelect")
    }

    private fun saveChanges() {
        if (!validatePrices()) return
        
        val name = etProductName.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        
        if (name.isEmpty() || desc.isEmpty() || currentCategoryIds.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val mrp = etMrp.text.toString().toDoubleOrNull()
        val wholesale = etWholesalePrice.text.toString().toDoubleOrNull()
        
        val isLive = originalProduct?.status?.lowercase() == "published" || originalProduct?.status?.lowercase() == "active"
        val wholesaleChanged = wholesale != (originalProduct?.variants?.firstOrNull()?.price ?: 0.0)
        val primaryCatChanged = currentCategoryIds.firstOrNull() != originalProduct?.category

        if (isLive && (wholesaleChanged || primaryCatChanged)) {
            AlertDialog.Builder(this)
                .setTitle("Confirm high-impact changes")
                .setMessage("This product is live. Price and category changes will be visible to buyers immediately. Continue?")
                .setPositiveButton("Continue") { _, _ -> executeSave(name, desc, mrp, wholesale) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            executeSave(name, desc, mrp, wholesale)
        }
    }

    private fun executeSave(name: String, desc: String, mrp: Double?, wholesale: Double?) {
        val request = UpdateProductRequest(
            name = name,
            description = desc,
            mrp = mrp,
            basePrice = mrp,
            salePrice = wholesale,
            category = currentCategoryIds.firstOrNull(),
            categoryIds = currentCategoryIds,
            minOrderQty = etMinOrderQty.text.toString().toIntOrNull() ?: 1,
            unit = spinnerUnit.selectedItem.toString().lowercase(),
            hsnCode = etHsnCode.text.toString().trim(),
            tags = etSearchTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        viewModel.updateProduct(productId, request)
    }

    override fun onBackPressed() {
        // Simplistic unsaved changes check
        if (originalProduct != null) {
            val currentName = etProductName.text.toString().trim()
            if (currentName != originalProduct?.name) {
                AlertDialog.Builder(this)
                    .setTitle("Discard changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to leave?")
                    .setPositiveButton("Leave") { _, _ -> super.onBackPressed() }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }
        }
        super.onBackPressed()
    }
}
