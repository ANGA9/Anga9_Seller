package com.anga9.seller.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.RecentOrderItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentOrderAdapter(
    private val onOrderClick: (RecentOrderItem) -> Unit
) : RecyclerView.Adapter<RecentOrderAdapter.ViewHolder>() {

    private val orders = mutableListOf<RecentOrderItem>()

    fun updateOrders(newOrders: List<RecentOrderItem>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvItemCount: TextView = view.findViewById(R.id.tvItemCount)
        val cardOrder: CardView = view.findViewById(R.id.cardOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        holder.tvOrderId.text = "#${order.orderId.takeLast(6).uppercase()}"
        holder.tvCustomerName.text = order.customerName
        holder.tvAmount.text = "₹${String.format("%,.0f", order.amount)}"
        holder.tvItemCount.text = "${order.itemCount} item${if (order.itemCount > 1) "s" else ""}"

        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(order.createdAt))

        // Status chip color
        val (statusText, statusColor) = when (order.status.lowercase()) {
            "new", "pending" -> Pair("New", "#2563EB")
            "accepted" -> Pair("Accepted", "#7C3AED")
            "packed" -> Pair("Packed", "#D97706")
            "shipped" -> Pair("Shipped", "#0891B2")
            "delivered" -> Pair("Delivered", "#059669")
            "cancelled" -> Pair("Cancelled", "#DC2626")
            else -> Pair(order.status.replaceFirstChar { it.uppercase() }, "#6B7280")
        }

        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))
        holder.cardOrder.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount() = orders.size
}
