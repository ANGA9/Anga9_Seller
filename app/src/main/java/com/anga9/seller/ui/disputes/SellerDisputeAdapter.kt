package com.anga9.seller.ui.disputes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerDispute
import java.text.SimpleDateFormat
import java.util.Locale

class SellerDisputeAdapter(
    private var disputes: List<SellerDispute>,
    private val onClick: (SellerDispute) -> Unit
) : RecyclerView.Adapter<SellerDisputeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dispute_seller, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dispute = disputes[position]
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        holder.tvOrderId.text = "Order: ${dispute.orderId.takeLast(8)}"
        holder.tvCustomerName.text = "Customer: ${dispute.customerName}"
        holder.tvDescription.text = dispute.description
        holder.tvAmount.text = "₹${String.format("%,.0f", dispute.orderAmount)}"
        holder.tvDate.text = dateFormat.format(dispute.createdAt)

        // Reason label
        holder.tvReason.text = when (dispute.reason) {
            "DEFECTIVE" -> "⚠️ Defective/Damaged"
            "WRONG_ITEM" -> "📦 Wrong Item"
            "QUALITY" -> "🔍 Quality Issue"
            else -> dispute.reason
        }

        // Status badge
        val (statusText, statusColor) = when (dispute.status) {
            "PENDING" -> "PENDING" to "#FF9800"
            "SELLER_ACCEPTED" -> "ACCEPTED" to "#4CAF50"
            "SELLER_REJECTED" -> "REJECTED" to "#F44336"
            "ADMIN_REVIEW" -> "ADMIN REVIEW" to "#9C27B0"
            "RESOLVED" -> "RESOLVED" to "#607D8B"
            else -> dispute.status to "#999999"
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        holder.itemView.setOnClickListener { onClick(dispute) }
    }

    override fun getItemCount() = disputes.size

    fun updateList(newList: List<SellerDispute>) {
        disputes = newList
        notifyDataSetChanged()
    }
}
