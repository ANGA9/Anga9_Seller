package com.anga9.seller.ui.notifications

import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerNotification
import java.text.SimpleDateFormat
import java.util.*

sealed class NotificationListItem {
    data class Header(val title: String) : NotificationListItem()
    data class Row(val notification: SellerNotification) : NotificationListItem()
}

class NotificationsAdapter(
    private val onNotificationClick: (SellerNotification) -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Row -> VIEW_TYPE_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_notification_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_notification_row, parent, false)
            RowViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is NotificationListItem.Header) {
            holder.bind(item.title)
        } else if (holder is RowViewHolder && item is NotificationListItem.Row) {
            holder.bind(item.notification)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateGroup: TextView = itemView.findViewById(R.id.tvDateGroup)
        fun bind(title: String) {
            tvDateGroup.text = title
        }
    }

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val vUnreadDot: View = itemView.findViewById(R.id.vUnreadDot)
        private val ivBadge: ImageView = itemView.findViewById(R.id.ivBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        
        fun bind(notification: SellerNotification) {
            tvTitle.text = notification.title
            tvDescription.text = notification.body
            
            // Format timestamp
            tvTimestamp.text = getRelativeTime(notification.sentAt)

            // Visuals
            val visual = NotificationVisuals.getVisualForType(notification.type)
            ivBadge.setImageResource(visual.iconResId)
            ivBadge.imageTintList = ColorStateList.valueOf(visual.iconTintColor)
            ivBadge.backgroundTintList = ColorStateList.valueOf(visual.badgeBgColor)

            // Read state
            if (notification.read) {
                vUnreadDot.visibility = View.GONE
                itemView.alpha = 0.6f
            } else {
                vUnreadDot.visibility = View.VISIBLE
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener {
                // Optimistic UI update
                vUnreadDot.visibility = View.GONE
                itemView.alpha = 0.6f
                onNotificationClick(notification)
            }
        }

        private fun getRelativeTime(isoString: String): String {
            if (isoString.isEmpty()) return ""
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(isoString) ?: return ""
                val now = System.currentTimeMillis()
                val diff = now - date.time
                
                return when {
                    diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
                    diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
                    diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h ago"
                    else -> "${diff / DateUtils.DAY_IN_MILLIS}d ago"
                }
            } catch (e: Exception) {
                return ""
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationListItem>() {
    override fun areItemsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
        if (oldItem is NotificationListItem.Header && newItem is NotificationListItem.Header) {
            return oldItem.title == newItem.title
        }
        if (oldItem is NotificationListItem.Row && newItem is NotificationListItem.Row) {
            return oldItem.notification.id == newItem.notification.id
        }
        return false
    }

    override fun areContentsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
        return oldItem == newItem
    }
}
