package com.anga9.seller.ui.wallet

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.databinding.ItemPayoutHistoryBinding
import com.anga9.seller.network.model.SellerPayoutResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PayoutHistoryAdapter :
    ListAdapter<SellerPayoutResponse, PayoutHistoryAdapter.PayoutViewHolder>(PayoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutViewHolder {
        val binding = ItemPayoutHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PayoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PayoutViewHolder(private val binding: ItemPayoutHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val formatINR = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
        
        private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        private val apiDateFormatFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        private val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        fun bind(item: SellerPayoutResponse) {
            binding.tvAmount.text = formatINR.format(item.amount)
            
            val dateStr = item.requestedAt ?: item.processedAt ?: ""
            binding.tvDate.text = try {
                val date = apiDateFormat.parse(dateStr) ?: apiDateFormatFallback.parse(dateStr)
                if (date != null) displayFormat.format(date) else dateStr
            } catch (e: Exception) {
                dateStr
            }

            val last4 = if (!item.bankAccount.isNullOrEmpty() && item.bankAccount.length > 4) {
                item.bankAccount.takeLast(4)
            } else {
                "****"
            }
            binding.tvBankAccount.text = "Bank ending in $last4"

            binding.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }

            val bg = binding.tvStatus.background.mutate() as? GradientDrawable
            
            when (item.status.lowercase()) {
                "processing", "pending" -> {
                    bg?.setColor(Color.parseColor("#FFF7E8"))
                    binding.tvStatus.setTextColor(Color.parseColor("#D98E04"))
                }
                "completed", "paid", "successful" -> {
                    bg?.setColor(Color.parseColor("#F0FBF4"))
                    binding.tvStatus.setTextColor(Color.parseColor("#1E7A45"))
                }
                "failed", "cancelled" -> {
                    bg?.setColor(Color.parseColor("#FEE2E2"))
                    binding.tvStatus.setTextColor(Color.parseColor("#DC2626"))
                }
                else -> {
                    bg?.setColor(Color.parseColor("#F3F4F6"))
                    binding.tvStatus.setTextColor(Color.parseColor("#4B5563"))
                }
            }
        }
    }

    class PayoutDiffCallback : DiffUtil.ItemCallback<SellerPayoutResponse>() {
        override fun areItemsTheSame(
            oldItem: SellerPayoutResponse,
            newItem: SellerPayoutResponse
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SellerPayoutResponse,
            newItem: SellerPayoutResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}
