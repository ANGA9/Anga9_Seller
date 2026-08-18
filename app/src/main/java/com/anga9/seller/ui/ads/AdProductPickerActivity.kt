package com.anga9.seller.ui.ads

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.R
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.data_models.SellerProduct
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.launch

class AdProductPickerActivity : AppCompatActivity() {

    private val productRepository by lazy { ProductRepository(this) }
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var adapter: ProductPickerAdapter
    private var allProducts: List<SellerProduct> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_product_picker)
        initViews()
        setupRecyclerView()
        setupSearch()
        loadProducts()
    }

    private fun initViews() {
        recyclerProducts = findViewById(R.id.recyclerProducts)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        etSearch = findViewById(R.id.etSearch)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ProductPickerAdapter { product ->
            val result = Intent().apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productImage", product.imageUrl)
                putExtra("productPrice", product.price)
                putExtra("productCategory", product.category)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        recyclerProducts.layoutManager = LinearLayoutManager(this)
        recyclerProducts.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            productRepository.getMyProducts("approved").collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        allProducts = (resource.data ?: emptyList()).map { r ->
                            com.anga9.seller.data_models.SellerProduct(
                                id = r.id,
                                name = r.name,
                                description = r.description ?: "",
                                price = r.salePrice ?: r.basePrice ?: r.mrp ?: r.price,
                                wholesalePrice = r.salePrice ?: r.price,
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
                        Toast.makeText(this@AdProductPickerActivity, resource.message, Toast.LENGTH_SHORT).show()
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

// ————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————

class ProductPickerAdapter(
    private val onProductSelected: (SellerProduct) -> Unit
) : ListAdapter<SellerProduct, ProductPickerAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ad_product_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        holder.tvProductName.text = product.name
        holder.tvPrice.text = "₹${String.format("%.0f", product.price)}"

        val imageUrl = product.imageUrl.ifEmpty { product.imageUrls.firstOrNull() ?: "" }
        if (imageUrl.isNotEmpty()) {
            holder.ivProductImage.load(imageUrl) {
                placeholder(R.drawable.ic_products)
            }
        }

        holder.itemView.setOnClickListener { onProductSelected(product) }
    }

    class DiffCallback : DiffUtil.ItemCallback<SellerProduct>() {
        override fun areItemsTheSame(old: SellerProduct, new: SellerProduct) = old.id == new.id
        override fun areContentsTheSame(old: SellerProduct, new: SellerProduct) = old == new
    }
}
