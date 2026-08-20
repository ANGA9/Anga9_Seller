package com.anga9.seller.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.R
import com.anga9.seller.network.model.OrderItemResponse
import com.google.android.material.imageview.ShapeableImageView
import java.text.NumberFormat
import java.util.Locale

class OrderItemAdapter(
    private var items: List<OrderItemResponse> = emptyList()
) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {

    fun submitList(newItems: List<OrderItemResponse>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail_product, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], isLast = position == items.size - 1)
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductThumb: ShapeableImageView = itemView.findViewById(R.id.ivProductThumb)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQtyAndPrice: TextView = itemView.findViewById(R.id.tvQtyAndPrice)
        private val tvLineTotal: TextView = itemView.findViewById(R.id.tvLineTotal)
        private val itemDivider: View = itemView.findViewById(R.id.itemDivider)

        fun bind(item: OrderItemResponse, isLast: Boolean) {
            tvProductName.text = item.productName.ifEmpty { "Product" }

            val unitPrice = item.getEffectiveUnitPrice()
            val lineTotal = item.getEffectiveTotalPrice()

            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2

            tvQtyAndPrice.text = "Qty: ${item.quantity} × ${format.format(unitPrice)}"
            tvLineTotal.text = format.format(lineTotal)

            itemDivider.visibility = if (isLast) View.GONE else View.VISIBLE

            val imageUrl = item.productImage?.let {
                if (it.startsWith("http")) it else "https://api.anga9.com/$it"
            }
            ivProductThumb.load(imageUrl) {
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
        }
    }
}
