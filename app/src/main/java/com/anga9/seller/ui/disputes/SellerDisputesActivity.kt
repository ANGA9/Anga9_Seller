package com.anga9.seller.ui.disputes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerDispute

class SellerDisputesActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvPendingBadge: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var rvDisputes: RecyclerView
    private lateinit var tabAll: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabAccepted: TextView
    private lateinit var tabRejected: TextView
    private lateinit var tabResolved: TextView

    private lateinit var adapter: SellerDisputeAdapter
    private var allDisputes = listOf<SellerDispute>()
    private var activeTab = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_disputes)
        initViews()
        setupRecyclerView()
        setupTabs()
        loadDisputes()
    }

    private fun initViews() {
        btnBack        = findViewById(R.id.btnBack)
        tvPendingBadge = findViewById(R.id.tvPendingBadge)
        progressBar    = findViewById(R.id.progressBar)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        rvDisputes     = findViewById(R.id.rvDisputes)
        tabAll         = findViewById(R.id.tabAll)
        tabPending     = findViewById(R.id.tabPending)
        tabAccepted    = findViewById(R.id.tabAccepted)
        tabRejected    = findViewById(R.id.tabRejected)
        tabResolved    = findViewById(R.id.tabResolved)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SellerDisputeAdapter(emptyList()) { dispute ->
            val intent = Intent(this, SellerDisputeDetailActivity::class.java)
            intent.putExtra("disputeId", dispute.disputeId)
            startActivityForResult(intent, 100)
        }
        rvDisputes.layoutManager = LinearLayoutManager(this)
        rvDisputes.adapter = adapter
    }

    private fun setupTabs() {
        val tabs    = listOf(tabAll, tabPending, tabAccepted, tabRejected, tabResolved)
        val filters = listOf("ALL", "PENDING", "SELLER_ACCEPTED", "SELLER_REJECTED", "RESOLVED")
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                activeTab = filters[index]
                tabs.forEach { t ->
                    t.setTextColor(Color.parseColor("#666666"))
                    t.textSize = 14f
                }
                tab.setTextColor(Color.parseColor("#2C3E50"))
                tab.textSize = 15f
                applyFilter()
            }
        }
        // Default active tab
        tabAll.setTextColor(Color.parseColor("#2C3E50"))
    }

    private fun loadDisputes() {
        // Phase 6B — backend not yet implemented for seller disputes list
        // TODO: wire to GET /api/seller/orders/disputes/ when backend is ready
        progressBar.visibility = View.GONE
        allDisputes = emptyList()
        val pendingCount = allDisputes.count { it.status == "PENDING" }
        if (pendingCount > 0) {
            tvPendingBadge.visibility = View.VISIBLE
            tvPendingBadge.text = "$pendingCount pending"
        } else {
            tvPendingBadge.visibility = View.GONE
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (activeTab == "ALL") allDisputes
                       else allDisputes.filter { it.status == activeTab }
        adapter.updateList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvDisputes.visibility  = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Refresh on return from detail
        if (requestCode == 100) loadDisputes()
    }
}
