package com.anga9.seller.ui.b2b

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data_models.RepeatBuyer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepeatBuyerAdapter(
    private val onBuyerClick: (RepeatBuyer) -> Unit,
    private val onMarkPaidClick: (RepeatBuyer) -> Unit
) : ListAdapter<RepeatBuyer, RepeatBuyerAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvBuyerName)
        val tvPhone: TextView = view.findViewById(R.id.tvBuyerPhone)
        val tvOrders: TextView = view.findViewById(R.id.tvTotalOrders)
        val tvValue: TextView = view.findViewById(R.id.tvTotalValue)
        val tvLastOrder: TextView = view.findViewById(R.id.tvLastOrder)
        val tvOutstanding: TextView = view.findViewById(R.id.tvOutstanding)
        val tvBadge: TextView = view.findViewById(R.id.tvBuyerBadge)
        val tvAvgOrder: TextView = view.findViewById(R.id.tvAvgOrder)
        val btnMarkPaid: Button = view.findViewById(R.id.btnMarkPaid)
        val tvPendingOrders: TextView = view.findViewById(R.id.tvPendingOrders)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repeat_buyer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buyer = getItem(position)
        val ctx = holder.itemView.context
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        holder.tvName.text = buyer.buyerName.ifEmpty { "Unknown Buyer" }
        holder.tvPhone.text = buyer.buyerPhone
        holder.tvOrders.text = "${buyer.totalOrders} Orders"
        holder.tvValue.text = "₹${String.format("%,.0f", buyer.totalOrderValue)}"
        holder.tvLastOrder.text = "Last: ${sdf.format(Date(buyer.lastOrderDate))}"
        holder.tvAvgOrder.text = "Avg: ₹${String.format("%,.0f", buyer.avgOrderValue)}"

        // Outstanding amount
        if (buyer.outstandingAmount > 0) {
            holder.tvOutstanding.visibility = View.VISIBLE
            holder.tvOutstanding.text = "⚠ Outstanding: ₹${String.format("%,.0f", buyer.outstandingAmount)}"
            holder.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.error_red))
            holder.btnMarkPaid.visibility = View.VISIBLE
        } else {
            holder.tvOutstanding.visibility = View.GONE
            holder.btnMarkPaid.visibility = View.GONE
        }

        // Pending orders
        if (buyer.pendingOrdersCount > 0) {
            holder.tvPendingOrders.visibility = View.VISIBLE
            holder.tvPendingOrders.text = "${buyer.pendingOrdersCount} order(s) in progress"
        } else {
            holder.tvPendingOrders.visibility = View.GONE
        }

        // Regular buyer badge
        if (buyer.isRegular) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = if (buyer.totalOrders >= 10) "⭐ VIP" else "✓ Regular"
            holder.tvBadge.setBackgroundColor(
                ContextCompat.getColor(ctx,
                    if (buyer.totalOrders >= 10) R.color.badge_premium else R.color.badge_trusted)
            )
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onBuyerClick(buyer) }
        holder.btnMarkPaid.setOnClickListener { onMarkPaidClick(buyer) }
    }

    class DiffCallback : DiffUtil.ItemCallback<RepeatBuyer>() {
        override fun areItemsTheSame(a: RepeatBuyer, b: RepeatBuyer) = a.buyerId == b.buyerId
        override fun areContentsTheSame(a: RepeatBuyer, b: RepeatBuyer) = a == b
    }
}
