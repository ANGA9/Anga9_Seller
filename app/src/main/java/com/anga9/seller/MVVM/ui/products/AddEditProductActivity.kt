package com.anga9.seller.MVVM.ui.products

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.CreateProductRequest
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputEditText

class AddEditProductActivity : AppCompatActivity() {

    private val viewModel: ProductsViewModel by viewModels()

    // Card 1: Media
    private lateinit var tvImageCount: TextView
    private lateinit var btnAddImages: Button
    private lateinit var rvProductImages: RecyclerView
    private lateinit var btnAddVideos: Button
    private lateinit var rvProductVideos: RecyclerView
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ProductImageAdapter

    // Card 2: Basic Details
    private lateinit var etProductName: TextInputEditText
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSubcategory: Spinner

    // Card 3: Pricing & Inventory
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etWholesalePrice: TextInputEditText
    private lateinit var etInitialStock: TextInputEditText
    private lateinit var etMinOrderQty: TextInputEditText
    private lateinit var spinnerUnit: Spinner

    // Card 4: Logistics
    private lateinit var etWeightKg: TextInputEditText
    private lateinit var etBrand: TextInputEditText
    private lateinit var etCountryOfOrigin: TextInputEditText
    private lateinit var etHsnCode: TextInputEditText
    private lateinit var etGstRate: TextInputEditText
    private lateinit var etSku: TextInputEditText

    // Card 5: Policies
    private lateinit var etReturnPolicy: TextInputEditText
    private lateinit var etWarranty: TextInputEditText
    private lateinit var etSearchTags: TextInputEditText

    // Submit
    private lateinit var btnSubmitProduct: Button

    private var categories = listOf<String>()
    private var subcategories = listOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris.take(10 - selectedImageUris.size))
            updateImagePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_product)
        initViews()
        observeViewModel()
        setupSpinners()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        tvImageCount = findViewById(R.id.tvImageCount)
        btnAddImages = findViewById(R.id.btnAddImages)
        rvProductImages = findViewById(R.id.rvProductImages)
        btnAddVideos = findViewById(R.id.btnAddVideos)
        rvProductVideos = findViewById(R.id.rvProductVideos)

        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerSubcategory = findViewById(R.id.spinnerSubcategory)

        etProductPrice = findViewById(R.id.etProductPrice)
        etWholesalePrice = findViewById(R.id.etWholesalePrice)
        etInitialStock = findViewById(R.id.etInitialStock)
        etMinOrderQty = findViewById(R.id.etMinOrderQty)
        spinnerUnit = findViewById(R.id.spinnerUnit)

        etWeightKg = findViewById(R.id.etWeightKg)
        etBrand = findViewById(R.id.etBrand)
        etCountryOfOrigin = findViewById(R.id.etCountryOfOrigin)
        etHsnCode = findViewById(R.id.etHsnCode)
        etGstRate = findViewById(R.id.etGstRate)
        etSku = findViewById(R.id.etSku)

        etReturnPolicy = findViewById(R.id.etReturnPolicy)
        etWarranty = findViewById(R.id.etWarranty)
        etSearchTags = findViewById(R.id.etSearchTags)

        btnSubmitProduct = findViewById(R.id.btnSubmitProduct)

        imageAdapter = ProductImageAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            imageAdapter.notifyItemRangeChanged(position, selectedImageUris.size)
            updateImagePreview()
        }
        rvProductImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvProductImages.adapter = imageAdapter

        btnAddImages.setOnClickListener {
            if (selectedImageUris.size < 10) imagePickerLauncher.launch("image/*")
            else Toast.makeText(this, "Maximum 10 images allowed", Toast.LENGTH_SHORT).show()
        }
        
        btnAddVideos.setOnClickListener {
            Toast.makeText(this, "Video upload coming soon", Toast.LENGTH_SHORT).show()
        }

        btnSubmitProduct.setOnClickListener {
            submitProduct()
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categories.isNotEmpty()) {
                    val selectedCategory = categories[position]
                    spinnerSubcategory.isEnabled = false
                    viewModel.loadSubcategories(selectedCategory)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateImagePreview() {
        tvImageCount.text = "${selectedImageUris.size}/10 images selected"
        rvProductImages.visibility = if (selectedImageUris.isEmpty()) View.GONE else View.VISIBLE
        imageAdapter.notifyDataSetChanged()
    }

    private fun setupSpinners() {
        val unitOptions = arrayOf("piece", "kg", "box", "set", "meter")
        spinnerUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitOptions)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.categoriesState.collect { state ->
                if (state is UiState.Success) {
                    categories = state.data
                    spinnerCategory.adapter = ArrayAdapter(this@AddEditProductActivity,
                        android.R.layout.simple_spinner_item, categories)
                        .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.subcategoriesState.collect { state ->
                if (state is UiState.Success) {
                    subcategories = state.data
                    if (subcategories.isEmpty()) {
                        spinnerSubcategory.isEnabled = false
                        spinnerSubcategory.adapter = ArrayAdapter(this@AddEditProductActivity,
                            android.R.layout.simple_spinner_item, listOf("No subcategories"))
                    } else {
                        spinnerSubcategory.isEnabled = true
                        spinnerSubcategory.adapter = ArrayAdapter(this@AddEditProductActivity,
                            android.R.layout.simple_spinner_item, subcategories)
                            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.createState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        btnSubmitProduct.isEnabled = false
                        btnSubmitProduct.text = "Submitting..."
                    }
                    is UiState.Success -> {
                        btnSubmitProduct.isEnabled = true
                        btnSubmitProduct.text = "Submit for Review"
                        Toast.makeText(this@AddEditProductActivity, "Product submitted successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is UiState.Error -> {
                        btnSubmitProduct.isEnabled = true
                        btnSubmitProduct.text = "Submit for Review"
                        Toast.makeText(this@AddEditProductActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun submitProduct() {
        val name = etProductName.text.toString().trim()
        val desc = etProductDescription.text.toString().trim()
        val priceStr = etProductPrice.text.toString().trim()
        val wholesaleStr = etWholesalePrice.text.toString().trim()

        if (name.isEmpty()) { etProductName.error = "Required"; return }
        if (desc.isEmpty()) { etProductDescription.error = "Required"; return }
        if (priceStr.isEmpty()) { etProductPrice.error = "Required"; return }
        if (wholesaleStr.isEmpty()) { etWholesalePrice.error = "Required"; return }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        val wholesalePrice = wholesaleStr.toDoubleOrNull() ?: 0.0

        if (price <= 0) { etProductPrice.error = "Invalid price"; return }
        if (wholesalePrice <= 0) { etWholesalePrice.error = "Invalid wholesale price"; return }
        if (wholesalePrice > price) { etWholesalePrice.error = "Cannot exceed MRP"; return }

        val categoryId = if (categories.isNotEmpty()) categories[spinnerCategory.selectedItemPosition] else "general"

        // Collect tags
        val tagsInput = etSearchTags.text.toString().trim()
        val tagList = if (tagsInput.isNotEmpty()) tagsInput.split(",").map { it.trim() } else null

        val request = CreateProductRequest(
            name = name,
            slug = name.lowercase().replace(Regex("[^a-z0-9]"), "-"),
            description = desc,
            basePrice = price,
            salePrice = wholesalePrice,
            minOrderQty = etMinOrderQty.text.toString().toIntOrNull() ?: 1,
            categoryIds = listOf(categoryId),
            unit = spinnerUnit.selectedItem?.toString() ?: "piece",
            initialStock = etInitialStock.text.toString().toIntOrNull() ?: 0,
            countryOfOrigin = etCountryOfOrigin.text.toString().ifEmpty { "India" },
            gstRate = etGstRate.text.toString().toDoubleOrNull() ?: 18.0,
            tags = tagList,
            hsnCode = etHsnCode.text.toString().ifEmpty { null },
            brand = etBrand.text.toString().ifEmpty { null },
            weightKg = etWeightKg.text.toString().toDoubleOrNull(),
            returnPolicy = etReturnPolicy.text.toString().ifEmpty { null },
            warranty = etWarranty.text.toString().ifEmpty { null },
            sku = etSku.text.toString().ifEmpty { null }
        )

        viewModel.createProduct(request)
    }
}
