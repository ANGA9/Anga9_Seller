package com.anga9.seller.ui.brand

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.BrandUser

/**
 * RecyclerView adapter for the brand switcher list.
 * Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md — Phase 2 (New Files)
 *
 * Displays each brand row with:
 *  - Brand logo initial (first letter of store name)
 *  - Store name + slug
 *  - Checkmark on the currently active brand
 */
class BrandAdapter(
    private val activeBrandId: String?,
    private val onBrandClick: (BrandUser) -> Unit
) : ListAdapter<BrandUser, BrandAdapter.BrandViewHolder>(BrandDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brand_switcher, parent, false)
        return BrandViewHolder(view)
    }

    override fun onBindViewHolder(holder: BrandViewHolder, position: Int) {
        holder.bind(getItem(position), activeBrandId, onBrandClick)
    }

    class BrandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBrandName: TextView = itemView.findViewById(R.id.tvBrandName)
        private val tvBrandSlug: TextView = itemView.findViewById(R.id.tvBrandSlug)
        private val tvBrandInitial: TextView = itemView.findViewById(R.id.tvBrandInitial)
        private val ivActiveCheck: ImageView = itemView.findViewById(R.id.ivActiveCheck)

        fun bind(brand: BrandUser, activeBrandId: String?, onClick: (BrandUser) -> Unit) {
            val name = brand.storeName ?: brand.displayName ?: "Brand"
            tvBrandName.text = name
            tvBrandSlug.text = brand.storeSlug?.let { "@$it" } ?: ""
            tvBrandInitial.text = name.firstOrNull()?.uppercase() ?: "B"

            // Show checkmark if this brand is currently active
            // If activeBrandId is null, parent brand (first item) is considered active
            val isActive = if (activeBrandId == null) {
                brand.parentUserId == null  // parent brand = no parentUserId
            } else {
                brand.id == activeBrandId
            }
            ivActiveCheck.visibility = if (isActive) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(brand) }
        }
    }

    private class BrandDiffCallback : DiffUtil.ItemCallback<BrandUser>() {
        override fun areItemsTheSame(oldItem: BrandUser, newItem: BrandUser) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BrandUser, newItem: BrandUser) =
            oldItem == newItem
    }
}
