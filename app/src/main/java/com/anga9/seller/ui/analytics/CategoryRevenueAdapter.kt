package com.anga9.seller.ui.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.CategoryRevenue

class CategoryRevenueAdapter : ListAdapter<CategoryRevenue, CategoryRevenueAdapter.VH>(DIFF) {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvCategoryName)
        val revenue: TextView = v.findViewById(R.id.tvCategoryRevenue)
        val percent: TextView = v.findViewById(R.id.tvCategoryPercent)
        val progress: ProgressBar = v.findViewById(R.id.progressCategory)
        val orders: TextView = v.findViewById(R.id.tvCategoryOrders)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_category_revenue, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.name.text = item.categoryName
        holder.revenue.text = "₹${String.format("%,.0f", item.revenue)}"
        holder.percent.text = "${String.format("%.1f", item.percentage)}%"
        holder.progress.progress = item.percentage.toInt()
        holder.orders.text = "${item.orderCount} orders"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CategoryRevenue>() {
            override fun areItemsTheSame(a: CategoryRevenue, b: CategoryRevenue) = a.categoryName == b.categoryName
            override fun areContentsTheSame(a: CategoryRevenue, b: CategoryRevenue) = a == b
        }
    }
}
