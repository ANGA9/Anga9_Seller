package com.anga9.seller.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerStatusUpdate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusHistoryAdapter(private val history: List<SellerStatusUpdate>) :
    RecyclerView.Adapter<StatusHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvNotes: TextView = view.findViewById(R.id.tvNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_status_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = history[history.size - 1 - position] // newest first
        val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
        holder.tvTime.text = fmt.format(Date(item.timestamp))
        if (item.notes.isNotEmpty()) {
            holder.tvNotes.visibility = View.VISIBLE
            holder.tvNotes.text = item.notes
        } else {
            holder.tvNotes.visibility = View.GONE
        }
    }

    override fun getItemCount() = history.size
}
