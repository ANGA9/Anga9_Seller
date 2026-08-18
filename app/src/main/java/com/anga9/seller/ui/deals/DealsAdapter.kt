package com.anga9.seller.ui.deals

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.DealItem
import com.anga9.seller.network.model.DealStatus
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DealsAdapter(
    private val onEditClick: (DealItem) -> Unit,
    private val onPauseResumeClick: (DealItem) -> Unit,
    private val onDeleteClick: (DealItem) -> Unit
) : ListAdapter<DealItem, DealsAdapter.DealViewHolder>(DealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deal_card, parent, false)
        return DealViewHolder(view)
    }

    override fun onBindViewHolder(holder: DealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvDealTypePill: TextView = itemView.findViewById(R.id.tvDealTypePill)
        private val tvStatusPill: TextView = itemView.findViewById(R.id.tvStatusPill)
        private val tvDealPrice: TextView = itemView.findViewById(R.id.tvDealPrice)
        private val tvPriceMeta: TextView = itemView.findViewById(R.id.tvPriceMeta)
        private val tvStartsValue: TextView = itemView.findViewById(R.id.tvStartsValue)
        private val tvEndsValue: TextView = itemView.findViewById(R.id.tvEndsValue)
        
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnPauseResume: MaterialButton = itemView.findViewById(R.id.btnPauseResume)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(deal: DealItem) {
            tvProductName.text = deal.displayProduct?.displayTitle ?: "Unknown Product"
            
            // Type Pill
            tvDealTypePill.text = when(deal.type) {
                "flash" -> "Flash Sale"
                "deal_of_day" -> "Deal of the Day"
                "quantity" -> "Quantity Discount"
                else -> deal.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

            // Status Pill
            val status = deal.currentStatus
            when (status) {
                DealStatus.ACTIVE -> {
                    tvStatusPill.text = "Active"
                    tvStatusPill.setTextColor(Color.parseColor("#1E7A45"))
                    tvStatusPill.setBackgroundResource(R.drawable.shape_status_active)
                    btnPauseResume.visibility = View.VISIBLE
                    btnPauseResume.text = "Pause"
                    btnPauseResume.setIconResource(R.drawable.ic_pause)
                }
                DealStatus.PAUSED -> {
                    tvStatusPill.text = "Paused"
                    tvStatusPill.setTextColor(Color.parseColor("#92400E"))
                    tvStatusPill.setBackgroundResource(R.drawable.shape_status_scheduled) // reusing amber pill for paused
                    btnPauseResume.visibility = View.VISIBLE
                    btnPauseResume.text = "Resume"
                    btnPauseResume.setIconResource(R.drawable.ic_play_arrow)
                }
                DealStatus.SCHEDULED -> {
                    tvStatusPill.text = "Scheduled"
                    tvStatusPill.setTextColor(Color.parseColor("#92400E"))
                    tvStatusPill.setBackgroundResource(R.drawable.shape_status_scheduled)
                    btnPauseResume.visibility = View.VISIBLE
                    btnPauseResume.text = "Pause"
                    btnPauseResume.setIconResource(R.drawable.ic_pause)
                }
                DealStatus.EXPIRED -> {
                    tvStatusPill.text = "Expired"
                    tvStatusPill.setTextColor(Color.parseColor("#5B6472"))
                    tvStatusPill.setBackgroundResource(R.drawable.shape_pill_unselected)
                    btnPauseResume.visibility = View.GONE
                }
            }

            // Price Row
            val formatPrice = if (deal.dealPrice % 1.0 == 0.0) {
                deal.dealPrice.toInt().toString()
            } else {
                deal.dealPrice.toString()
            }
            tvDealPrice.text = "₹$formatPrice"
            
            if (deal.quantityThreshold >= 1) {
                tvPriceMeta.visibility = View.VISIBLE
                tvPriceMeta.text = "(Min ${deal.quantityThreshold})"
            } else {
                tvPriceMeta.visibility = View.GONE
            }

            // Timeline format
            tvStartsValue.text = formatDateTime(deal.startsAt)
            tvEndsValue.text = formatDateTime(deal.endsAt)

            // Click Listeners
            btnEdit.setOnClickListener { onEditClick(deal) }
            btnPauseResume.setOnClickListener { onPauseResumeClick(deal) }
            btnDelete.setOnClickListener { onDeleteClick(deal) }
        }

        private fun formatDateTime(isoString: String): String {
            return try {
                // Try format with milliseconds and Z
                var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                var date = parser.parse(isoString)
                
                if (date == null) {
                    // Try format with +00:00 and no milliseconds
                    parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    date = parser.parse(isoString)
                }
                
                if (date == null) {
                    // Try just date time
                    parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    date = parser.parse(isoString)
                }
                
                if (date != null) {
                    val formatter = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
                    formatter.format(date)
                } else {
                    isoString
                }
            } catch (e: Exception) {
                // Fallback to simpler parse
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    val date = parser.parse(isoString)
                    val formatter = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
                    date?.let { formatter.format(it) } ?: isoString
                } catch (e2: Exception) {
                    isoString
                }
            }
        }
    }
}

class DealDiffCallback : DiffUtil.ItemCallback<DealItem>() {
    override fun areItemsTheSame(oldItem: DealItem, newItem: DealItem): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: DealItem, newItem: DealItem): Boolean {
        return oldItem == newItem
    }
}
