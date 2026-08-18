package com.anga9.seller.ui.deals

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.ui.ads.ProductPickerAdapter
import com.anga9.seller.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProductPickerBottomSheet(
    private val onProductSelected: (SellerProduct) -> Unit
) : BottomSheetDialogFragment() {

    private val productRepository by lazy { ProductRepository(requireContext()) }
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var adapter: ProductPickerAdapter
    private var allProducts: List<SellerProduct> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_product_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupSearch()
        loadProducts()
    }

    private fun initViews(view: View) {
        recyclerProducts = view.findViewById(R.id.recyclerProducts)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        etSearch = view.findViewById(R.id.etSearch)
    }

    private fun setupRecyclerView() {
        recyclerProducts.layoutManager = LinearLayoutManager(context)
        adapter = ProductPickerAdapter { product ->
            onProductSelected(product)
            dismiss()
        }
        recyclerProducts.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterProducts(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadProducts() {
        progressBar.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        recyclerProducts.visibility = View.GONE

        lifecycleScope.launch {
            productRepository.getMyProducts("all").collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        allProducts = (resource.data ?: emptyList()).map { r ->
                            SellerProduct(
                                id = r.id,
                                name = r.name,
                                description = r.description ?: "",
                                price = r.salePrice ?: r.basePrice ?: r.mrp ?: r.price,
                                wholesalePrice = r.salePrice ?: r.price,
                                brand = "",
                                imageUrl = r.images?.firstOrNull() ?: r.imageUrl ?: "",
                                imageUrls = r.images ?: emptyList(),
                                category = r.category ?: "",
                                stock = r.stock,
                                status = r.status,
                                isActive = r.isActive
                            )
                        }

                        if (allProducts.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                            recyclerProducts.visibility = View.GONE
                        } else {
                            layoutEmpty.visibility = View.GONE
                            recyclerProducts.visibility = View.VISIBLE
                            adapter.submitList(allProducts)
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun filterProducts(query: String) {
        val filtered = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true) ||
                product.brand.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
        if (filtered.isEmpty() && query.isNotEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            recyclerProducts.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerProducts.visibility = View.VISIBLE
        }
    }
}
