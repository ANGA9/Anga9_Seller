package com.anga9.seller.ui.dashboard

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.RecentOrderItem
import com.anga9.seller.utils.AppFormatters
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

        val displayId = if (order.orderNumber.isNotEmpty()) {
            if (order.orderNumber.startsWith("#")) order.orderNumber else "#${order.orderNumber}"
        } else if (order.orderId.length > 8) {
            "#${order.orderId.takeLast(6).uppercase()}"
        } else {
            "#${order.orderId}"
        }

        holder.tvOrderId.text = displayId
        holder.tvCustomerName.text = order.customerName.ifEmpty { "Customer" }
        holder.tvAmount.text = AppFormatters.formatINR(order.amount)
        holder.tvItemCount.text = "${order.itemCount} item${if (order.itemCount > 1) "s" else ""}"

        if (order.createdAt > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(order.createdAt))
            holder.tvDate.visibility = View.VISIBLE
        } else {
            holder.tvDate.visibility = View.GONE
        }

        // Shared status chip config
        val config = AppFormatters.getStatusConfig(order.status)
        holder.tvStatus.text = config.label
        holder.tvStatus.setTextColor(config.textColor)

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(config.bgColor)
        }
        holder.tvStatus.background = bgDrawable

        holder.cardOrder.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount() = orders.size
}
