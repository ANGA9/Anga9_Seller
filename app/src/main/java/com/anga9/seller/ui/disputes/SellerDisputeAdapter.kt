package com.anga9.seller.ui.disputes

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.DisputeItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SellerDisputeAdapter(
    private val onItemClick: (DisputeItem) -> Unit,
    private val onResolveClick: (DisputeItem) -> Unit
) : ListAdapter<DisputeItem, SellerDisputeAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<DisputeItem>() {
        override fun areItemsTheSame(oldItem: DisputeItem, newItem: DisputeItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DisputeItem, newItem: DisputeItem): Boolean =
            oldItem == newItem
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardDispute: MaterialCardView = view.findViewById(R.id.cardDispute)
        val flTypeBadge: FrameLayout = view.findViewById(R.id.flTypeBadge)
        val ivTypeIcon: ImageView = view.findViewById(R.id.ivTypeIcon)
        val tvTypeAndQty: TextView = view.findViewById(R.id.tvTypeAndQty)
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvStatusPill: TextView = view.findViewById(R.id.tvStatusPill)
        val tvReasonText: TextView = view.findViewById(R.id.tvReasonText)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvRefundBadge: TextView = view.findViewById(R.id.tvRefundBadge)
        val btnResolve: MaterialButton = view.findViewById(R.id.btnResolve)

        fun bind(item: DisputeItem) {
            val typeConfig = DisputeConfig.getTypeConfig(item.type)
            val statusStyle = DisputeConfig.getStatusConfig(item.status)

            // Card Styling & Dimming
            cardDispute.strokeColor = Color.parseColor(statusStyle.borderHex)
            cardDispute.alpha = if (statusStyle.dimmed) 0.75f else 1.0f

            // Issue type badge & icon
            ivTypeIcon.setImageResource(typeConfig.iconRes)
            ivTypeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(statusStyle.textHex))
            flTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(statusStyle.bgHex))

            // Title & Order ID
            val qty = item.requestedQty ?: 1
            tvTypeAndQty.text = "${typeConfig.label} · Qty $qty"
            tvOrderId.text = "Order #${item.orderId.take(8)}..."

            // Status Pill
            tvStatusPill.text = statusStyle.label.uppercase()
            tvStatusPill.setTextColor(Color.parseColor(statusStyle.textHex))
            val pillDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * itemView.resources.displayMetrics.density
                setColor(Color.parseColor(statusStyle.bgHex))
            }
            tvStatusPill.background = pillDrawable

            // Reason
            tvReasonText.text = if (item.reason.isNotBlank()) item.reason else "Customer raised a dispute for this item."

            // Date
            tvDate.text = formatDate(item.createdAt)

            // Refund badge if applicable
            val refund = item.refundAmount
            if (refund != null && refund > 0) {
                tvRefundBadge.visibility = View.VISIBLE
                val format = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                    maximumFractionDigits = 0
                }
                tvRefundBadge.text = "Refund: ${format.format(refund)}"
            } else {
                tvRefundBadge.visibility = View.GONE
            }

            // Action Button (Resolve) — CRITICAL: only show if showAction is true
            if (statusStyle.showAction) {
                btnResolve.visibility = View.VISIBLE
                btnResolve.setOnClickListener { onResolveClick(item) }
            } else {
                btnResolve.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dispute_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
