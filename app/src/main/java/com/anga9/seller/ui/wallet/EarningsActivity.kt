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
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.EarningItemResponse
import com.anga9.seller.network.model.SellerEarningsResponse
import com.anga9.seller.utils.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EarningsActivity : BaseActivity() {

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
            startActivity(Intent(this, PayoutsActivity::class.java))
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

    data class EarningStatusStyle(
        val label: String,
        val bgHex: String,
        val textHex: String
    )

    companion object {
        val earningStatusConfig: Map<String, EarningStatusStyle> = mapOf(
            "available" to EarningStatusStyle("Available", "#EAF3DE", "#27500A"),
            "processing" to EarningStatusStyle("Processing", "#FAEEDA", "#854F0B"),
            "pending" to EarningStatusStyle("Pending", "#FAEEDA", "#854F0B"),
            "on_hold" to EarningStatusStyle("On hold", "#F3E8FD", "#6B21A8"),
            "requested" to EarningStatusStyle("Requested", "#F3E8FD", "#6B21A8"),
            "paid" to EarningStatusStyle("Paid", "#EAF3DE", "#27500A"),
            "failed" to EarningStatusStyle("Failed", "#FBE4E1", "#B42318"),
            "reversed" to EarningStatusStyle("Reversed", "#FBE4E1", "#B42318")
        )
    }

    private fun updateEarningsUI(e: SellerEarningsResponse) {
        tvHeroAmount.text = fmt(e.total)
        tvPendingAmount.text = fmt(e.pending)
        tvAvailableAmount.text = fmt(e.available)
        tvRequestedAmount.text = fmt(e.requested)
        tvPaidAmount.text = fmt(e.paid)
    }

    private fun fmt(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 0
            format.format(amount)
        } else {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.minimumFractionDigits = 1
            format.maximumFractionDigits = 2
            format.format(amount)
        }
    }

    inner class EarningsHistoryAdapter : ListAdapter<EarningItemResponse, EarningsHistoryAdapter.VH>(
        object : DiffUtil.ItemCallback<EarningItemResponse>() {
            override fun areItemsTheSame(a: EarningItemResponse, b: EarningItemResponse) = a.id == b.id
            override fun areContentsTheSame(a: EarningItemResponse, b: EarningItemResponse) = a == b
        }
    ) {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvProductName: TextView = v.findViewById(R.id.tvProductName)
            private val tvMeta: TextView = v.findViewById(R.id.tvMeta)
            private val tvAmount: TextView = v.findViewById(R.id.tvAmount)
            private val tvStatus: TextView = v.findViewById(R.id.tvStatus)

            fun bind(item: EarningItemResponse) {
                val productName = item.orderItems?.productName?.takeIf { it.isNotBlank() }
                    ?: item.description?.takeIf { it.isNotBlank() }
                    ?: ("Order #" + (item.orderItems?.orderId?.take(8) ?: item.orderId?.take(8) ?: "N/A"))

                tvProductName.text = productName

                val qty = item.orderItems?.quantity ?: 1
                val dateFormatted = formatDate(item.createdAt ?: "")
                tvMeta.text = if (dateFormatted.isNotBlank()) "×$qty · $dateFormatted" else "×$qty"

                tvAmount.text = fmt(item.amount)

                val statusKey = item.status.lowercase().trim()
                val config = earningStatusConfig[statusKey] ?: EarningStatusStyle(
                    label = statusKey.replaceFirstChar { it.uppercase() },
                    bgHex = "#F3F4F6",
                    textHex = "#374151"
                )

                tvStatus.text = config.label
                tvStatus.setTextColor(Color.parseColor(config.textHex))

                val pillDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 12f * itemView.resources.displayMetrics.density
                    setColor(Color.parseColor(config.bgHex))
                }
                tvStatus.background = pillDrawable
            }

            private fun formatDate(dateStr: String): String {
                if (dateStr.isBlank()) return ""
                return try {
                    val cleaned = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr.substringBefore("Z")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val d = sdf.parse(cleaned) ?: return dateStr
                    val out = SimpleDateFormat("d MMM yyyy", Locale.US)
                    out.format(d)
                } catch (e: Exception) {
                    try {
                        val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val d2 = sdf2.parse(dateStr.take(10)) ?: return dateStr
                        val out2 = SimpleDateFormat("d MMM yyyy", Locale.US)
                        out2.format(d2)
                    } catch (e2: Exception) {
                        dateStr.take(10)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_earning_history, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    }
}
