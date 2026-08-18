package com.anga9.seller.ui.wallet

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.anga9.seller.R
import com.anga9.seller.network.model.EarningItemResponse
import com.anga9.seller.network.model.SellerEarningsResponse
import com.anga9.seller.utils.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EarningsActivity : AppCompatActivity() {

    private lateinit var viewModel: EarningsViewModel
    private lateinit var historyAdapter: EarningsHistoryAdapter

    // Header
    private lateinit var btnBack: ImageView
    
    // Swipe & Scroll
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    // States
    private lateinit var llErrorState: LinearLayout
    private lateinit var tvErrorMsg: TextView
    private lateinit var btnRetry: View
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmptyState: View
    
    // Hero Card
    private lateinit var tvHeroAmount: TextView
    
    // 2x2 Grid (Pending, Available, Requested, Paid)
    // Pending
    private lateinit var gridPending: View
    private lateinit var tvPendingAmount: TextView
    // Available
    private lateinit var gridAvailable: View
    private lateinit var tvAvailableAmount: TextView
    // Requested
    private lateinit var gridRequested: View
    private lateinit var tvRequestedAmount: TextView
    // Paid
    private lateinit var gridPaid: View
    private lateinit var tvPaidAmount: TextView
    
    // CTA
    private lateinit var btnManagePayouts: View
    private lateinit var tvCtaHelp: TextView

    // Recycler
    private lateinit var rvEarningsHistory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earnings)
        
        viewModel = ViewModelProvider(this)[EarningsViewModel::class.java]
        
        initViews()
        setupGridCards()
        setupListeners()
        setupRecyclerView()
        observeViewModel()
        
        loadData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        llErrorState = findViewById(R.id.llErrorState)
        tvErrorMsg = findViewById(R.id.tvErrorMsg)
        btnRetry = findViewById(R.id.btnRetry)
        progressBar = findViewById(R.id.progressBar)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        
        val heroCard = findViewById<View>(R.id.heroCard)
        tvHeroAmount = heroCard.findViewById(R.id.tvHeroAmount)
        
        gridPending = findViewById(R.id.gridPending)
        gridAvailable = findViewById(R.id.gridAvailable)
        gridRequested = findViewById(R.id.gridRequested)
        gridPaid = findViewById(R.id.gridPaid)
        
        btnManagePayouts = findViewById(R.id.btnManagePayouts)
        tvCtaHelp = findViewById(R.id.tvCtaHelp)
        
        rvEarningsHistory = findViewById(R.id.rvEarningsHistory)
    }

    private fun setupGridCards() {
        // Pending: amber tint (#FFF7E8 bg, #D98E04 icon)
        setupGridCard(gridPending, "PENDING CLEARANCE", R.drawable.ic_time, "#FFF7E8", "#D98E04")
        tvPendingAmount = gridPending.findViewById(R.id.tvAmount)

        // Available: green tint (#F0FBF4 bg, #1E7A45 icon)
        setupGridCard(gridAvailable, "AVAILABLE TO WITHDRAW", R.drawable.ic_wallet, "#F0FBF4", "#1E7A45")
        tvAvailableAmount = gridAvailable.findViewById(R.id.tvAmount)

        // Requested: purple tint (#F5EEFF bg, #7C3AED icon)
        setupGridCard(gridRequested, "PAYOUT REQUESTED", R.drawable.ic_document, "#F5EEFF", "#7C3AED")
        tvRequestedAmount = gridRequested.findViewById(R.id.tvAmount)

        // Paid: green tint (#F0FBF4 bg, #1E7A45 icon)
        setupGridCard(gridPaid, "SUCCESSFULLY PAID", R.drawable.ic_check_circle, "#F0FBF4", "#1E7A45")
        tvPaidAmount = gridPaid.findViewById(R.id.tvAmount)
    }

    private fun setupGridCard(cardView: View, label: String, iconRes: Int, bgTint: String, iconTint: String) {
        val tvLabel = cardView.findViewById<TextView>(R.id.tvLabel)
        val ivIcon = cardView.findViewById<ImageView>(R.id.ivIcon)
        val flIconBg = cardView.findViewById<View>(R.id.flIconBg)
        
        tvLabel.text = label
        ivIcon.setImageResource(iconRes)
        ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(iconTint))
        flIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgTint))
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        btnRetry.setOnClickListener {
            llErrorState.visibility = View.GONE
            loadData()
        }
        
        btnManagePayouts.setOnClickListener {
            // Navigate to PayoutsActivity
            startActivity(Intent(this, PayoutsActivity::class.java))
        }
        
        tvCtaHelp.setOnClickListener {
            // Open web link or help center
        }
    }
    
    private fun setupRecyclerView() {
        historyAdapter = EarningsHistoryAdapter()
        rvEarningsHistory.adapter = historyAdapter
    }

    private fun loadData() {
        viewModel.loadEarnings()
        viewModel.loadEarningsHistory()
    }

    private fun observeViewModel() {
        viewModel.earningsState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    if (!swipeRefresh.isRefreshing) {
                        tvHeroAmount.text = "₹..."
                        tvPendingAmount.text = "₹..."
                        tvAvailableAmount.text = "₹..."
                        tvRequestedAmount.text = "₹..."
                        tvPaidAmount.text = "₹..."
                    }
                }
                is Resource.Success -> {
                    llErrorState.visibility = View.GONE
                    val data = result.data
                    if (data != null) {
                        updateEarningsUI(data)
                    }
                }
                is Resource.Error -> {
                    llErrorState.visibility = View.VISIBLE
                    tvErrorMsg.text = result.message ?: "Failed to load earnings"
                }
            }
        }
        
        viewModel.historyState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    if (!swipeRefresh.isRefreshing) progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    val list = result.data?.earnings ?: emptyList()
                    
                    if (list.isEmpty()) {
                        rvEarningsHistory.visibility = View.GONE
                        layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        rvEarningsHistory.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                        historyAdapter.submitList(list)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun updateEarningsUI(e: SellerEarningsResponse) {
        tvHeroAmount.text = fmt(e.total)
        tvPendingAmount.text = fmt(e.pending)
        tvAvailableAmount.text = fmt(e.available)
        tvRequestedAmount.text = fmt(e.requested)
        tvPaidAmount.text = fmt(e.paid)
    }

    private fun fmt(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    inner class EarningsHistoryAdapter : ListAdapter<EarningItemResponse, EarningsHistoryAdapter.VH>(
        object : DiffUtil.ItemCallback<EarningItemResponse>() {
            override fun areItemsTheSame(a: EarningItemResponse, b: EarningItemResponse) = a.id == b.id
            override fun areContentsTheSame(a: EarningItemResponse, b: EarningItemResponse) = a == b
        }
    ) {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvDesc = v.findViewById<TextView>(R.id.tvDescription)
            val tvDate = v.findViewById<TextView>(R.id.tvDate)
            val tvAmount = v.findViewById<TextView>(R.id.tvAmount)
            val tvStatus = v.findViewById<TextView>(R.id.tvStatus)

            fun bind(item: EarningItemResponse) {
                tvDesc.text = "Order #${item.orderId?.take(8) ?: "N/A"}"
                tvDate.text = formatDate(item.createdAt ?: "")
                tvAmount.text = "+ ${fmt(item.amount)}"
                tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
                
                when (item.status.lowercase()) {
                    "pending" -> tvStatus.setTextColor(Color.parseColor("#D98E04"))
                    "available", "paid" -> tvStatus.setTextColor(Color.parseColor("#1E7A45"))
                    "requested" -> tvStatus.setTextColor(Color.parseColor("#7C3AED"))
                    else -> tvStatus.setTextColor(Color.parseColor("#5B6472"))
                }
            }
            
            private fun formatDate(dateStr: String): String {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val d = sdf.parse(dateStr) ?: return dateStr
                    val out = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    return out.format(d)
                } catch(e: Exception) {
                    return dateStr
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_earning_history, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    }
}
