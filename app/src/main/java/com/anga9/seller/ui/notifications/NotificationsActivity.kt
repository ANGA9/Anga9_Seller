package com.anga9.seller.ui.notifications

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerNotification
import com.anga9.seller.ui.disputes.SellerDisputeDetailActivity
import com.anga9.seller.ui.orders.OrderDetailActivity
import com.anga9.seller.ui.wallet.PayoutsActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsActivity : BaseActivity() {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    // Views
    private lateinit var rvNotifications: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var llEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMarkAllRead: MaterialButton
    private lateinit var btnBack: ImageView

    // Chips
    private lateinit var chipAll: TextView
    private lateinit var chipUnread: TextView
    private lateinit var chipOrders: TextView
    private lateinit var chipDisputes: TextView
    private lateinit var chipPayouts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        
        // Force light mode background for consistency
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        rvNotifications = findViewById(R.id.rvNotifications)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        llEmptyState = findViewById(R.id.llEmptyState)
        progressBar = findViewById(R.id.progressBar)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        btnBack = findViewById(R.id.btnBack)

        chipAll = findViewById(R.id.chipAll)
        chipUnread = findViewById(R.id.chipUnread)
        chipOrders = findViewById(R.id.chipOrders)
        chipDisputes = findViewById(R.id.chipDisputes)
        chipPayouts = findViewById(R.id.chipPayouts)
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter { notification ->
            handleNotificationClick(notification)
        }
        val layoutManager = LinearLayoutManager(this)
        rvNotifications.layoutManager = layoutManager
        rvNotifications.adapter = adapter

        // Infinite scroll
        rvNotifications.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisiblesItems = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                        viewModel.loadNotifications()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications(isRefresh = true)
        }

        // Chip click listeners
        chipAll.setOnClickListener {
            updateChipStyles(chipAll)
            viewModel.setFilter(NotificationsViewModel.FilterType.ALL)
        }
        chipUnread.setOnClickListener {
            updateChipStyles(chipUnread)
            viewModel.setFilter(NotificationsViewModel.FilterType.UNREAD)
        }
        chipOrders.setOnClickListener {
            updateChipStyles(chipOrders)
            viewModel.setFilter(NotificationsViewModel.FilterType.ORDERS)
        }
        chipDisputes.setOnClickListener {
            updateChipStyles(chipDisputes)
            viewModel.setFilter(NotificationsViewModel.FilterType.DISPUTES)
        }
        chipPayouts.setOnClickListener {
            updateChipStyles(chipPayouts)
            viewModel.setFilter(NotificationsViewModel.FilterType.PAYOUTS)
        }
    }

    private fun updateChipStyles(selectedChip: TextView) {
        val allChips = listOf(chipAll, chipUnread, chipOrders, chipDisputes, chipPayouts)
        for (chip in allChips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.parseColor("#111827"))
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is NotificationsViewModel.UiState.Loading -> {
                        if (!swipeRefresh.isRefreshing && adapter.itemCount == 0) {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                    is NotificationsViewModel.UiState.Success -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                    }
                    is NotificationsViewModel.UiState.Error -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@NotificationsActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.groupedItems.collectLatest { items ->
                adapter.submitList(items)
                if (items.isEmpty() && viewModel.uiState.value !is NotificationsViewModel.UiState.Loading) {
                    llEmptyState.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                } else {
                    llEmptyState.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun handleNotificationClick(notification: SellerNotification) {
        viewModel.markAsRead(notification.id)

        val entityType = notification.data?.get("relatedEntityType")
        val entityId = notification.data?.get("relatedEntityId")

        when (entityType) {
            "order" -> {
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("ORDER_ID", entityId) // Using assumed standard extra keys
                startActivity(intent)
            }
            "dispute" -> {
                val intent = Intent(this, SellerDisputeDetailActivity::class.java)
                intent.putExtra("DISPUTE_ID", entityId)
                startActivity(intent)
            }
            "payout" -> {
                val intent = Intent(this, PayoutsActivity::class.java)
                startActivity(intent)
            }
            else -> {
                // If the backend doesn't send relatedEntityType but just order_id, handle legacy mappings
                if (notification.data?.containsKey("order_id") == true) {
                    val intent = Intent(this, OrderDetailActivity::class.java)
                    intent.putExtra("ORDER_ID", notification.data["order_id"])
                    startActivity(intent)
                } else if (notification.type.startsWith("order_")) {
                    Toast.makeText(this, "Order Details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
