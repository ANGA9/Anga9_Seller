package com.anga9.seller.ui.wallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.WalletTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : ListAdapter<WalletTransaction, TransactionAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WalletTransaction>() {
            override fun areItemsTheSame(a: WalletTransaction, b: WalletTransaction) =
                a.transactionId == b.transactionId
            override fun areContentsTheSame(a: WalletTransaction, b: WalletTransaction) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivTransactionIcon)
        val tvDescription: TextView = view.findViewById(R.id.tvTransactionDescription)
        val tvDate: TextView = view.findViewById(R.id.tvTransactionDate)
        val tvAmount: TextView = view.findViewById(R.id.tvTransactionAmount)
        val tvStatus: TextView = view.findViewById(R.id.tvTransactionStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_wallet_transaction, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tx = getItem(position)
        holder.tvDescription.text = tx.description
        holder.tvDate.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(tx.timestamp))

        if (tx.isCredit()) {
            holder.tvAmount.text = "+${tx.formatCurrency()}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
            holder.ivIcon.setImageResource(R.drawable.ic_credit_arrow)
        } else {
            holder.tvAmount.text = "-${tx.formatCurrency()}"
            holder.tvAmount.setTextColor(Color.parseColor("#F44336"))
            holder.ivIcon.setImageResource(R.drawable.ic_debit_arrow)
        }

        holder.tvStatus.text = tx.status
        holder.tvStatus.setTextColor(when (tx.status) {
            "COMPLETED", "DUE" -> Color.parseColor("#4CAF50")
            "PENDING" -> Color.parseColor("#FFA500")
            "PROCESSING" -> Color.parseColor("#2196F3")
            "FAILED", "CANCELLED" -> Color.parseColor("#F44336")
            else -> Color.parseColor("#757575")
        })
    }
}
