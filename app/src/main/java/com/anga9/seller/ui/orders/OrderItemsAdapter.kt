package com.anga9.seller.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerOrderItem

class OrderItemsAdapter(private val items: List<SellerOrderItem>) :
    RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.ivProduct)
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvOrderTypeBadge: TextView = view.findViewById(R.id.tvOrderTypeBadge)
        val tvQty: TextView = view.findViewById(R.id.tvQuantity)
        val tvPrice: TextView = view.findViewById(R.id.tvUnitPrice)
        val tvSubtotal: TextView = view.findViewById(R.id.tvSubtotal)
        val tvDiscount: TextView = view.findViewById(R.id.tvDiscount)
        val tvVariantBreakdown: TextView = view.findViewById(R.id.tvVariantBreakdown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.productName

        // --- Order Type Badge ---
        val isPerSet = item.orderType == "per_set" && item.setSize > 1
        if (isPerSet) {
            holder.tvOrderTypeBadge.text = "Per Set (${item.setSize} pcs/set)"
            holder.tvOrderTypeBadge.setBackgroundColor(0xFF7C3AED.toInt())
        } else {
            holder.tvOrderTypeBadge.text = "Per Piece"
            holder.tvOrderTypeBadge.setBackgroundColor(0xFF2563EB.toInt())
        }

        // --- Quantity Display ---
        val qtyText = when {
            // Per Set with variants
            item.hasVariants && isPerSet -> {
                val totalSets = item.variantSelections.values.sum()
                val totalPieces = item.quantity
                "$totalSets Sets ($totalPieces pcs total)"
            }
            // Per Set without variants
            isPerSet -> {
                val sets = if (item.setSize > 0) item.quantity / item.setSize else 1
                "$sets Sets (${item.quantity} pcs total)"
            }
            // Per Piece with variants
            item.hasVariants -> "${item.quantity} pcs"
            // Per Piece simple
            else -> "${item.quantity} ${item.unit}"
        }
        holder.tvQty.text = "Qty: $qtyText"

        // --- Unit Price ---
        holder.tvPrice.text = if (isPerSet)
            "₹${String.format("%.2f", item.unitPrice)}/set"
        else
            "₹${String.format("%.2f", item.unitPrice)}/piece"

        // --- Subtotal ---
        holder.tvSubtotal.text = "₹${String.format("%.2f", item.subtotal)}"

        // --- Variant Breakdown ---
        if (item.hasVariants && item.variantSummary.isNotEmpty()) {
            holder.tvVariantBreakdown.visibility = View.VISIBLE
            holder.tvVariantBreakdown.text = item.variantSummary
        } else if (item.setLabel.isNotEmpty()) {
            holder.tvVariantBreakdown.visibility = View.VISIBLE
            holder.tvVariantBreakdown.text = item.setLabel
        } else {
            holder.tvVariantBreakdown.visibility = View.GONE
        }

        // --- Bulk Discount ---
        if (item.bulkDiscountPercent > 0) {
            holder.tvDiscount.visibility = View.VISIBLE
            holder.tvDiscount.text = "${item.bulkDiscountPercent}% bulk discount applied"
        } else {
            holder.tvDiscount.visibility = View.GONE
        }

        // --- Product Image ---
        if (item.productImage.isNotEmpty()) {
            holder.ivProduct.load(item.productImage) {
                placeholder(R.drawable.ic_products)
                error(R.drawable.ic_products)
            }
        }
    }

    override fun getItemCount() = items.size
}