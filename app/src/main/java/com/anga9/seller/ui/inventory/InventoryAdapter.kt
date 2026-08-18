package com.anga9.seller.ui.inventory

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import coil.load
import com.google.android.material.card.MaterialCardView

class InventoryAdapter(
    private val onEditClicked: (InventoryRow) -> Unit
) : ListAdapter<InventoryRow, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory_product, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: MaterialCardView = itemView.findViewById(R.id.cardContainer)
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvStatusPill: TextView = itemView.findViewById(R.id.tvStatusPill)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val tvAlertAt: TextView = itemView.findViewById(R.id.tvAlertAt)
        private val tvReserved: TextView = itemView.findViewById(R.id.tvReserved)

        fun bind(row: InventoryRow) {
            val product = row.product
            val stock = row.stock
            
            val qty = stock?.stock ?: 0
            val threshold = stock?.lowStockThreshold ?: 10
            val reserved = stock?.reserved ?: 0

            tvProductName.text = product.name
            val priceToShow = product.basePrice ?: product.price
            tvPrice.text = "₹${priceToShow}" // Matching web's formatINR(base_price)
            tvStock.text = qty.toString()
            tvAlertAt.text = threshold.toString()
            tvReserved.text = reserved.toString()

            // Load Image
            val imageUrl = product.imageUrl ?: product.images?.firstOrNull()
            if (!imageUrl.isNullOrEmpty()) {
                ivProduct.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_package)
                    error(R.drawable.ic_package)
                }
            } else {
                ivProduct.setImageResource(R.drawable.ic_package)
            }

            // Status Logic (Colors matching Web: Neutral, Amber, Red)
            val context = itemView.context
            when {
                qty <= 0 -> {
                    // Out of stock (Red)
                    tvStatusPill.text = "OUT OF STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#D8342A"))
                    tvStatusPill.setBackgroundResource(R.drawable.bg_rounded_danger_tint)
                    cardContainer.strokeColor = Color.parseColor("#F3B4AE")
                    tvStock.setTextColor(Color.parseColor("#D8342A"))
                }
                qty <= threshold -> {
                    // Low stock (Amber)
                    tvStatusPill.text = "LOW STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#D98E04"))
                    tvStatusPill.setBackgroundResource(R.drawable.bg_rounded_warning_tint)
                    cardContainer.strokeColor = Color.parseColor("#F5D98A")
                    tvStock.setTextColor(Color.parseColor("#D98E04"))
                }
                else -> {
                    // In stock (Green/Neutral)
                    tvStatusPill.text = "IN STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#1E7A45"))
                    tvStatusPill.setBackgroundResource(R.drawable.bg_rounded_success_tint)
                    cardContainer.strokeColor = Color.parseColor("#E5E7EB") // Neutral border
                    tvStock.setTextColor(ContextCompat.getColor(context, R.color.seller_text_primary))
                }
            }

            btnEdit.setOnClickListener {
                onEditClicked(row)
            }
        }
    }

    class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryRow>() {
        override fun areItemsTheSame(oldItem: InventoryRow, newItem: InventoryRow): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: InventoryRow, newItem: InventoryRow): Boolean {
            return oldItem == newItem
        }
    }
}
