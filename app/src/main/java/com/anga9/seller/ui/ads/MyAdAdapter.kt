package com.anga9.seller.ui.ads

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.R
import com.anga9.seller.network.model.AdCampaignResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MyAdAdapter(
    private val onItemClick: (AdCampaignResponse) -> Unit
) : ListAdapter<AdCampaignResponse, MyAdAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner: ImageView = view.findViewById(R.id.ivBanner)
        val tvCampaignName: TextView = view.findViewById(R.id.tvCampaignName)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvPlacement: TextView = view.findViewById(R.id.tvPlacement)
        val tvDates: TextView = view.findViewById(R.id.tvDates)
        val tvBudget: TextView = view.findViewById(R.id.tvBudget)
        val tvImpressions: TextView = view.findViewById(R.id.tvImpressions)
        val tvClicks: TextView = view.findViewById(R.id.tvClicks)
        val tvRejectReason: TextView = view.findViewById(R.id.tvRejectReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ad_campaign, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val campaign = getItem(position)

        holder.tvCampaignName.text = campaign.headline.ifEmpty { "Untitled Campaign" }
        holder.tvProductName.text = "Product: ${campaign.products?.name ?: campaign.productId.split("-").firstOrNull() ?: "Unknown"}"
        
        holder.tvPlacement.text = campaign.placement.replace("_", " ")
        
        // Date formatting
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val start = inputFormat.parse(campaign.startsAt)
            val end = inputFormat.parse(campaign.endsAt)
            if (start != null && end != null) {
                holder.tvDates.text = "${outputFormat.format(start)} – ${outputFormat.format(end)}"
            } else {
                holder.tvDates.text = "${campaign.startsAt} – ${campaign.endsAt}"
            }
        } catch (e: Exception) {
            holder.tvDates.text = ""
        }

        holder.tvBudget.text = "₹${String.format("%,d", campaign.budgetInr.toLong())}"
        
        holder.tvImpressions.text = "${String.format("%,d", campaign.impressions)} VWS"
        holder.tvClicks.text = "${String.format("%,d", campaign.clicks)} CLKS"

        // Banner Image
        val bannerUrl = campaign.bannerUrl
        if (!bannerUrl.isNullOrEmpty()) {
            val fullUrl = if (bannerUrl.startsWith("http")) {
                bannerUrl
            } else {
                val supabaseUrl = "https://plfaugkadavxenpqawzw.supabase.co"
                if (bannerUrl.startsWith("/storage/")) {
                    "$supabaseUrl$bannerUrl"
                } else if (bannerUrl.startsWith("/")) {
                    "$supabaseUrl/storage/v1/object/public/public-assets$bannerUrl"
                } else {
                    "$supabaseUrl/storage/v1/object/public/public-assets/$bannerUrl"
                }
            }
            android.util.Log.e("MyAdAdapter", "Loading bannerUrl: $bannerUrl -> fullUrl: $fullUrl")
            holder.ivBanner.load(fullUrl) {
                placeholder(R.drawable.ic_products)
                error(R.drawable.ic_products)
            }
        } else {
            android.util.Log.e("MyAdAdapter", "Banner URL is empty!")
            holder.ivBanner.setImageResource(R.drawable.ic_products)
        }

        // Status Pill
        setupStatusPill(holder.tvStatus, campaign.status)

        // Reject Reason
        if (campaign.status == "rejected" && !campaign.rejectReason.isNullOrEmpty()) {
            holder.tvRejectReason.visibility = View.VISIBLE
            holder.tvRejectReason.text = "Rejected: ${campaign.rejectReason}"
        } else {
            holder.tvRejectReason.visibility = View.GONE
        }

        // Removed item click listener since preview screen is no longer needed
    }

    private fun setupStatusPill(tvStatus: TextView, status: String) {
        val (bgColorHex, textColorHex, label) = when (status.lowercase()) {
            "pending" -> Triple("#FFFBEB", "#B45309", "Pending Review") // amber
            "approved" -> Triple("#EFF6FF", "#1D4ED8", "Approved") // blue
            "active" -> Triple("#ECFDF5", "#047857", "Active") // green
            "completed" -> Triple("#F3F4F6", "#374151", "Completed") // gray
            "rejected" -> Triple("#FEF2F2", "#B91C1C", "Rejected") // red
            else -> Triple("#F3F4F6", "#374151", status)
        }

        tvStatus.text = label.uppercase()
        tvStatus.setTextColor(Color.parseColor(textColorHex))
        
        // Update background shape color dynamically
        val bg = tvStatus.background.mutate()
        if (bg is GradientDrawable) {
            bg.setColor(Color.parseColor(bgColorHex))
            bg.setStroke(1, Color.parseColor(textColorHex))
            bg.alpha = 50 // roughly 20% opacity for the stroke? Actually let's just color it.
        } else {
            // Fallback if background is not GradientDrawable
            tvStatus.setBackgroundColor(Color.parseColor(bgColorHex))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AdCampaignResponse>() {
        override fun areItemsTheSame(old: AdCampaignResponse, new: AdCampaignResponse) = old.id == new.id
        override fun areContentsTheSame(old: AdCampaignResponse, new: AdCampaignResponse) = old == new
    }
}
