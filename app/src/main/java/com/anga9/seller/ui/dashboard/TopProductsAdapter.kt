package com.anga9.seller.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.TopProductItem
import com.anga9.seller.utils.AppFormatters

class TopProductsAdapter(
    private var items: List<TopProductItem> = emptyList(),
    private val onItemClick: ((TopProductItem) -> Unit)? = null
) : RecyclerView.Adapter<TopProductsAdapter.ViewHolder>() {

    fun submitList(newItems: List<TopProductItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_top_product_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRankNumber: TextView = itemView.findViewById(R.id.tvRankNumber)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvUnitsSold: TextView = itemView.findViewById(R.id.tvUnitsSold)
        private val tvProductRevenue: TextView = itemView.findViewById(R.id.tvProductRevenue)

        fun bind(item: TopProductItem, rank: Int) {
            tvRankNumber.text = rank.toString()
            tvProductName.text = item.name.ifEmpty { "Product" }
            tvUnitsSold.text = "${item.unitsSold} units sold"
            tvProductRevenue.text = AppFormatters.formatINRShort(item.revenue)

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
