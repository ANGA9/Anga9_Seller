package com.anga9.seller.ui.orders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.StatusHistoryResponse
import java.text.SimpleDateFormat
import java.util.*

class OrderTimelineAdapter(
    private var events: List<StatusHistoryResponse> = emptyList()
) : RecyclerView.Adapter<OrderTimelineAdapter.TimelineViewHolder>() {

    fun submitList(newEvents: List<StatusHistoryResponse>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = events.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_timeline_event, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(events[position], isLatest = position == events.size - 1, isLast = position == events.size - 1)
    }

    inner class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val vTimelineDot: View = itemView.findViewById(R.id.vTimelineDot)
        private val vTimelineLine: View = itemView.findViewById(R.id.vTimelineLine)
        private val tvTimelineStatus: TextView = itemView.findViewById(R.id.tvTimelineStatus)
        private val tvTimelineNote: TextView = itemView.findViewById(R.id.tvTimelineNote)
        private val tvTimelineDate: TextView = itemView.findViewById(R.id.tvTimelineDate)

        fun bind(event: StatusHistoryResponse, isLatest: Boolean, isLast: Boolean) {
            val rawStatus = event.status
            val statusLabel = when (rawStatus.lowercase()) {
                "pending" -> "Order placed"
                "confirmed" -> "Order Confirmed"
                "processing" -> "Processing"
                "shipped" -> "Shipped"
                "delivered" -> "Delivered"
                "cancelled" -> "Cancelled"
                else -> rawStatus
            }
            tvTimelineStatus.text = statusLabel

            val note = event.getEffectiveNote()
            if (!note.isNullOrBlank() && !note.equals(rawStatus, ignoreCase = true) && !note.equals(statusLabel, ignoreCase = true)) {
                tvTimelineNote.visibility = View.VISIBLE
                tvTimelineNote.text = note
            } else {
                tvTimelineNote.visibility = View.GONE
            }

            tvTimelineDate.text = formatIsoDate(event.getEffectiveDate())

            // Dot style: Solid accent #2851C4 for latest event, light blue #93C5FD for past events
            val dotDrawable = GradientDrawable()
            dotDrawable.shape = GradientDrawable.OVAL
            if (isLatest) {
                dotDrawable.setColor(Color.parseColor("#2851C4"))
            } else {
                dotDrawable.setColor(Color.parseColor("#93C5FD"))
            }
            vTimelineDot.background = dotDrawable

            // Hide connecting line on the last event
            vTimelineLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        }

        private fun formatIsoDate(isoString: String?): String {
            if (isoString.isNullOrBlank()) return ""
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            var date: Date? = null
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    date = sdf.parse(isoString)
                    if (date != null) break
                } catch (e: Exception) {
                    // continue
                }
            }
            if (date == null) return isoString
            val outSdf = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }
            return outSdf.format(date).lowercase()
        }
    }
}
