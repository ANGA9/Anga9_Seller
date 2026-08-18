package com.anga9.seller.ui.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.TopProduct

class TopProductAdapter : ListAdapter<TopProduct, TopProductAdapter.VH>(DIFF) {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val rank: TextView = v.findViewById(R.id.tvRank)
        val name: TextView = v.findViewById(R.id.tvProductName)
        val sold: TextView = v.findViewById(R.id.tvUnitsSold)
        val revenue: TextView = v.findViewById(R.id.tvProductRevenue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_top_product, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.rank.text = "#${position + 1}"
        holder.name.text = item.productName
        holder.sold.text = "${item.totalSold} units sold"
        holder.revenue.text = "₹${String.format("%,.0f", item.totalRevenue)}"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<TopProduct>() {
            override fun areItemsTheSame(a: TopProduct, b: TopProduct) = a.productId == b.productId
            override fun areContentsTheSame(a: TopProduct, b: TopProduct) = a == b
        }
    }
}
