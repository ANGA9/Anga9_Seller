package com.anga9.seller.ui.deals

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.DealItem
import com.anga9.seller.utils.UiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PromotionalDealsActivity : BaseActivity() {

    private val viewModel: DealsViewModel by viewModels()
    private lateinit var adapter: DealsAdapter

    private var currentTab = "All Deals"
    private var currentQuery = ""

    private lateinit var tabAll: TextView
    private lateinit var tabActive: TextView
    private lateinit var tabScheduled: TextView
    private lateinit var tabExpired: TextView
    private lateinit var tvCountBadge: TextView
    
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var btnCreateDealEmpty: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotional_deals)

        setupViews()
        setupRecyclerView()
        setupObservers()

        viewModel.loadDeals()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDeals()
    }

    private fun setupViews() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        tvCountBadge = findViewById(R.id.tvCountBadge)
        tabAll = findViewById(R.id.tabAll)
        tabActive = findViewById(R.id.tabActive)
        tabScheduled = findViewById(R.id.tabScheduled)
        tabExpired = findViewById(R.id.tabExpired)
        
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc)
        btnCreateDealEmpty = findViewById(R.id.btnCreateDealEmpty)
        progressBar = findViewById(R.id.progressBar)

        val etSearch: EditText = findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString() ?: ""
                viewModel.applyFilters(currentTab, currentQuery)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tabAll.setOnClickListener { switchTab("All Deals") }
        tabActive.setOnClickListener { switchTab("Active") }
        tabScheduled.setOnClickListener { switchTab("Scheduled") }
        tabExpired.setOnClickListener { switchTab("Expired") }

        findViewById<FloatingActionButton>(R.id.fabCreateDeal).setOnClickListener {
            startActivity(Intent(this, CreateDealActivity::class.java))
        }
        btnCreateDealEmpty.setOnClickListener {
            startActivity(Intent(this, CreateDealActivity::class.java))
        }
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        updateTabStyles()
        viewModel.applyFilters(currentTab, currentQuery)
    }

    private fun updateTabStyles() {
        val tabs = listOf(tabAll to "All Deals", tabActive to "Active", tabScheduled to "Scheduled", tabExpired to "Expired")
        for ((textView, name) in tabs) {
            if (name == currentTab) {
                textView.setBackgroundResource(R.drawable.shape_tab_selected)
                textView.setTextColor(Color.WHITE)
            } else {
                textView.setBackgroundResource(R.drawable.shape_tab_unselected)
                textView.setTextColor(Color.parseColor("#5B6472"))
            }
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DealsAdapter(
            onEditClick = { deal ->
                Toast.makeText(this, "Edit: ${deal.id}", Toast.LENGTH_SHORT).show()
            },
            onPauseResumeClick = { deal ->
                viewModel.togglePauseResume(deal)
            },
            onDeleteClick = { deal ->
                showDeleteConfirmation(deal)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmation(deal: DealItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete this deal?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDeal(deal.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    val deals = state.data
                    tvCountBadge.text = deals.size.toString()
                    adapter.submitList(deals)
                    
                    if (deals.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
                        
                        if (currentQuery.isNotEmpty()) {
                            tvEmptyTitle.text = "No deals found"
                            tvEmptyDesc.text = "No deals matched your search criteria."
                            btnCreateDealEmpty.visibility = View.GONE
                        } else {
                            if (currentTab == "All Deals") {
                                tvEmptyTitle.text = "No deals yet"
                                tvEmptyDesc.text = "Create a discount, flash sale, or special offer to boost sales."
                                btnCreateDealEmpty.visibility = View.VISIBLE
                            } else {
                                tvEmptyTitle.text = "No deals found"
                                tvEmptyDesc.text = "No $currentTab deals right now."
                                btnCreateDealEmpty.visibility = View.GONE
                            }
                        }
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
                    }
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.toggleState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    // It's optimistically updated, but we can show a small toast if we want
                }
                is UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
        
        viewModel.deleteState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    Toast.makeText(this, "Deal deleted", Toast.LENGTH_SHORT).show()
                }
                is UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
