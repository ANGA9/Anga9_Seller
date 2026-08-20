package com.anga9.seller.ui.inventory

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import java.text.NumberFormat
import java.util.Locale

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
            
            val qty = stock?.effectiveQuantity ?: stock?.quantity ?: stock?.stock ?: 0
            val threshold = stock?.lowStockThreshold ?: 10
            val reserved = stock?.reserved ?: 0

            tvProductName.text = product.name

            val priceVal = (product.basePrice ?: product.price ?: 0.0)
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                maximumFractionDigits = 0
            }
            tvPrice.text = format.format(priceVal)
            tvStock.text = qty.toString()
            tvAlertAt.text = threshold.toString()
            tvReserved.text = "$reserved units"

            // Load Image
            val imageUrl = product.imageUrl ?: product.images?.firstOrNull()
            if (!imageUrl.isNullOrEmpty()) {
                val fullUrl = if (imageUrl.startsWith("http")) imageUrl else "https://api.anga9.com/$imageUrl"
                ivProduct.load(fullUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            } else {
                ivProduct.setImageResource(R.drawable.bg_image_placeholder)
            }

            // Status Logic (Colors matching Web: IN STOCK, LOW STOCK, OUT OF STOCK)
            val context = itemView.context
            when {
                qty <= 0 -> {
                    // Out of stock (Red)
                    tvStatusPill.text = "OUT OF STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#DC2626"))
                    
                    val bg = GradientDrawable()
                    bg.cornerRadius = 16f
                    bg.setColor(Color.parseColor("#FEF2F2"))
                    tvStatusPill.background = bg
                    
                    cardContainer.strokeColor = Color.parseColor("#FCA5A5")
                    tvStock.setTextColor(Color.parseColor("#DC2626"))
                }
                qty <= threshold -> {
                    // Low stock (Amber)
                    tvStatusPill.text = "LOW STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#D97706"))
                    
                    val bg = GradientDrawable()
                    bg.cornerRadius = 16f
                    bg.setColor(Color.parseColor("#FFFBEB"))
                    tvStatusPill.background = bg
                    
                    cardContainer.strokeColor = Color.parseColor("#FCD34D")
                    tvStock.setTextColor(Color.parseColor("#D97706"))
                }
                else -> {
                    // In stock (Green)
                    tvStatusPill.text = "IN STOCK"
                    tvStatusPill.setTextColor(Color.parseColor("#16A34A"))
                    
                    val bg = GradientDrawable()
                    bg.cornerRadius = 16f
                    bg.setColor(Color.parseColor("#F0FDF4"))
                    tvStatusPill.background = bg
                    
                    cardContainer.strokeColor = Color.parseColor("#E5E7EB")
                    tvStock.setTextColor(Color.parseColor("#111827"))
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
