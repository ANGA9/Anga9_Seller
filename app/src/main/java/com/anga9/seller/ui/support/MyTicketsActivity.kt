package com.anga9.seller.ui.support

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.support.SupportTicket
import com.anga9.seller.data.repository.SupportRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyTicketsActivity : BaseActivity() {

    private lateinit var repository: SupportRepository
    private lateinit var ticketAdapter: TicketAdapter
    private lateinit var chipAdapter: ChipAdapter

    private lateinit var btnBack: ImageView
    private lateinit var btnNewTicket: TextView
    private lateinit var btnEmptyNewTicket: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var btnRetry: Button
    private lateinit var rvTickets: RecyclerView
    private lateinit var rvFilterChips: RecyclerView

    private var currentPage = 1
    private var isLoading = false
    private var hasMore = true
    
    private val allTickets = mutableListOf<SupportTicket>()
    private var currentFilter = "all"

    private val filters = listOf("all", "open", "pending_user", "in_progress", "resolved", "closed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)

        repository = SupportRepository(this)
        initViews()
        setupAdapters()
        loadTickets(reset = true)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnNewTicket = findViewById(R.id.btnNewTicket)
        btnEmptyNewTicket = findViewById(R.id.btnEmptyNewTicket)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnRetry = findViewById(R.id.btnRetry)
        rvTickets = findViewById(R.id.rvTickets)
        rvFilterChips = findViewById(R.id.rvFilterChips)

        btnBack.setOnClickListener { finish() }
        
        val newTicketAction = View.OnClickListener {
            startActivity(Intent(this, RaiseTicketActivity::class.java))
        }
        btnNewTicket.setOnClickListener(newTicketAction)
        btnEmptyNewTicket.setOnClickListener(newTicketAction)

        btnRetry.setOnClickListener {
            loadTickets(reset = true)
        }

        // Pagination listener
        rvTickets.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && !isLoading && hasMore) {
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    if (lm.findLastVisibleItemPosition() >= ticketAdapter.itemCount - 3) {
                        loadTickets(reset = false)
                    }
                }
            }
        })
    }

    private fun setupAdapters() {
        ticketAdapter = TicketAdapter { ticket ->
            // Assume we navigate to detail
            // val intent = Intent(this, TicketDetailActivity::class.java)
            // intent.putExtra("ticket_id", ticket.id)
            // startActivity(intent)
        }
        rvTickets.adapter = ticketAdapter
        
        chipAdapter = ChipAdapter(filters, currentFilter) { selected ->
            currentFilter = selected
            applyFilter()
        }
        rvFilterChips.adapter = chipAdapter
    }

    private fun loadTickets(reset: Boolean) {
        if (isLoading) return
        if (reset) {
            currentPage = 1
            hasMore = true
            allTickets.clear()
            applyFilter()
        }
        isLoading = true
        progressBar.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.getTickets(page = currentPage, limit = 20)
            isLoading = false
            progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { response ->
                    allTickets.addAll(response.tickets)
                    hasMore = response.tickets.size >= 20
                    currentPage++
                    applyFilter()
                },
                onFailure = {
                    if (allTickets.isEmpty()) {
                        btnRetry.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.VISIBLE
                    }
                }
            )
        }
    }
    
    private fun applyFilter() {
        val filtered = if (currentFilter == "all") {
            allTickets
        } else {
            allTickets.filter { it.status.lowercase() == currentFilter }
        }
        ticketAdapter.submitList(filtered.toList())
        
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvTickets.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}

// --- Adapters ----------------------------------------------------------------

class ChipAdapter(
    private val items: List<String>,
    private var selectedItem: String,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_filter_chip, parent, false)
        return ChipVH(view)
    }

    override fun onBindViewHolder(holder: ChipVH, position: Int) {
        val item = items[position]
        holder.bind(item, item == selectedItem)
    }

    override fun getItemCount() = items.size

    inner class ChipVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvLabel: TextView = view.findViewById(R.id.tvChipLabel)

        fun bind(statusKey: String, isSelected: Boolean) {
            if (statusKey == "all") {
                tvLabel.text = "All"
            } else {
                tvLabel.text = TicketStatusConfig.getStyle(statusKey).label
            }

            if (isSelected) {
                tvLabel.setBackgroundResource(R.drawable.bg_filter_chip_selected)
                tvLabel.setTextColor(Color.WHITE)
            } else {
                if (statusKey == "all") {
                    tvLabel.setBackgroundResource(R.drawable.bg_filter_chip_unselected)
                    tvLabel.setTextColor(Color.parseColor("#444441"))
                } else {
                    val style = TicketStatusConfig.getStyle(statusKey)
                    if (style.bgColor == Color.parseColor("#FFFFFF")) {
                        tvLabel.setBackgroundResource(R.drawable.bg_filter_chip_unselected)
                    } else {
                        val bg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_pill)?.mutate()
                        bg?.setTint(style.bgColor)
                        tvLabel.background = bg
                    }
                    tvLabel.setTextColor(style.textColor)
                }
            }
            
            itemView.setOnClickListener {
                if (!isSelected) {
                    val oldSelected = selectedItem
                    selectedItem = statusKey
                    // Since it's a small list, notifyDataSetChanged is fine, but we can do it properly
                    notifyDataSetChanged()
                    onSelect(statusKey)
                }
            }
        }
    }
}

class TicketAdapter(
    private val onClick: (SupportTicket) -> Unit
) : ListAdapter<SupportTicket, TicketAdapter.TicketVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_row, parent, false)
        return TicketVH(view)
    }

    override fun onBindViewHolder(holder: TicketVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TicketVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvStatusPill: TextView = view.findViewById(R.id.tvStatusPill)
        private val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        private val flIconBadge: View = view.findViewById(R.id.flIconBadge)
        private val ivBadgeIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)

        fun bind(ticket: SupportTicket) {
            val style = TicketStatusConfig.getStyle(ticket.status)
            
            tvTitle.text = ticket.subject
            tvStatusPill.text = style.label
            
            // Apply Status Pill styling
            val bgPill = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_pill)?.mutate()
            bgPill?.setTint(style.bgColor)
            tvStatusPill.background = bgPill
            tvStatusPill.setTextColor(style.textColor)
            
            // Apply Icon Badge styling
            val bgBadge = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_pill)?.mutate()
            bgBadge?.setTint(style.bgColor)
            flIconBadge.background = bgBadge
            ivBadgeIcon.setImageResource(style.iconRes)
            ivBadgeIcon.imageTintList = ColorStateList.valueOf(style.textColor)

            // Meta Line
            val timeStr = getRelativeTime(ticket.updatedAt)
            val categoryStr = if(ticket.category.isNullOrEmpty()) "General" else ticket.category
            tvMeta.text = "$categoryStr · #${ticket.ticketNumber} · $timeStr"

            // Dimmed
            itemView.alpha = if (style.dimmed) 0.75f else 1.0f

            itemView.setOnClickListener { onClick(ticket) }
        }

        private fun getRelativeTime(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                val now = Date()
                val diffInMillis = now.time - date.time
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInHours / 24
                when {
                    diffInDays > 0 -> "${diffInDays}d ago"
                    diffInHours > 0 -> "${diffInHours}h ago"
                    else -> "Just now"
                }
            } catch (_: Exception) { dateStr }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SupportTicket>() {
            override fun areItemsTheSame(old: SupportTicket, new: SupportTicket) = old.id == new.id
            override fun areContentsTheSame(old: SupportTicket, new: SupportTicket) = old == new
        }
    }
}
