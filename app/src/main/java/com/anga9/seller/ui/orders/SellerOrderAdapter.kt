package com.anga9.seller.ui.orders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil.load
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerOrderResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SellerOrderAdapter(
    private val onOrderClick: (SellerOrderResponse) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.OrderViewHolder>() {

    private var orders: List<SellerOrderResponse> = emptyList()

    fun submitList(newList: List<SellerOrderResponse>) {
        this.orders = newList
        notifyDataSetChanged()
    }

    val currentList: List<SellerOrderResponse>
        get() = orders

    override fun getItemCount(): Int = orders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvMoreItems: TextView = itemView.findViewById(R.id.tvMoreItems)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(order: SellerOrderResponse) {
            val orderNum = order.orderNumber ?: order.id.take(8)
            tvOrderId.text = if (orderNum.startsWith("ANGA")) "#$orderNum" else "#ANGA-$orderNum"
            
            // Format date — web seller uses placed_at
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.US)
            val dateStr = order.getEffectiveDate()?.let {
                try {
                    val date = inputFormat.parse(it)
                    if (date != null) outputFormat.format(date) else it
                } catch (e: Exception) {
                    it
                }
            } ?: "N/A"
            tvOrderDate.text = dateStr

            // Status Pill — web seller uses items[0]?.status || order.status
            val statusKey = order.getEffectiveStatus().lowercase()
            val style = OrderStatusConfig.config[statusKey] ?: OrderStatusConfig.config["all"]!!
            tvStatus.text = style.label
            tvStatus.setTextColor(style.getTextColor())
            
            val bgDrawable = GradientDrawable()
            bgDrawable.cornerRadius = 24f
            bgDrawable.setColor(style.getBgColor())
            if (style.border != null) {
                bgDrawable.setStroke(2, style.getBorderColor())
            }
            tvStatus.background = bgDrawable

            // Product Bottom Row
            val firstItem = order.items.firstOrNull()
            tvProductName.text = firstItem?.productName ?: "Unknown Product"
            tvQty.text = "Qty: ${firstItem?.quantity ?: 1}"
            
            val extraCount = order.items.size - 1
            if (extraCount > 0) {
                tvMoreItems.visibility = View.VISIBLE
                tvMoreItems.text = "+$extraCount more ${if (extraCount == 1) "item" else "items"}"
            } else {
                tvMoreItems.visibility = View.GONE
            }

            // Image
            val imageUrl = firstItem?.productImage?.let {
                if (it.startsWith("http")) it else "https://api.anga9.com/$it"
            }
            ivProduct.load(imageUrl) {
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }

            // Total Price — web seller: items.reduce((sum, item) => sum + item.total_price, 0)
            val totalAmt = order.items.sumOf { it.totalPrice.takeIf { tp -> tp > 0 } ?: (it.unitPrice * it.quantity).takeIf { up -> up > 0 } ?: (it.price * it.quantity) }
            val finalTotal = if (totalAmt > 0) totalAmt else order.totalAmount
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 0
            tvTotal.text = format.format(finalTotal)

            // Click listener
            itemView.setOnClickListener {
                onOrderClick(order)
            }
        }
    }
}
