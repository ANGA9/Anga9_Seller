package com.anga9.seller.MVVM.ui.products

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.anga9.seller.R
import com.anga9.seller.data_models.SellerProduct
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SellerProductAdapter(
    private var products: List<SellerProduct>,
    private val onEditClick: (SellerProduct) -> Unit,
    private val onDeleteClick: (SellerProduct) -> Unit,
    private val onStockClick: (SellerProduct) -> Unit,
    private val onItemClick: (SellerProduct) -> Unit
) : RecyclerView.Adapter<SellerProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvMetaLine: TextView = itemView.findViewById(R.id.tvMetaLine)
        val tvWholesalePrice: TextView = itemView.findViewById(R.id.tvWholesalePrice)
        val tvMrpPrice: TextView = itemView.findViewById(R.id.tvMrpPrice)
        val tvStatusPill: TextView = itemView.findViewById(R.id.tvStatusPill)
        val ivRejectionInfo: ImageView = itemView.findViewById(R.id.ivRejectionInfo)
        val ivEditProduct: ImageView = itemView.findViewById(R.id.ivEditProduct)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(products[adapterPosition])
                }
            }
            ivEditProduct.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onEditClick(products[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card_web, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        val context = holder.itemView.context

        holder.tvProductName.text = product.name
        
        // Price logic
        holder.tvWholesalePrice.text = "₹${String.format(Locale.getDefault(), "%.2f", product.wholesalePrice)}"
        if (product.price > product.wholesalePrice && product.wholesalePrice > 0.0) {
            holder.tvMrpPrice.visibility = View.VISIBLE
            holder.tvMrpPrice.text = "₹${String.format(Locale.getDefault(), "%.2f", product.price)}"
            holder.tvMrpPrice.paintFlags = holder.tvMrpPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvMrpPrice.visibility = View.GONE
        }

        // Meta line
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val dateAdded = if (product.createdAt > 0) dateFormat.format(Date(product.createdAt)) else "Recently"
        holder.tvMetaLine.text = "Min qty ${product.moq} ${product.unit} · $dateAdded"

        // Image loading
        if (product.imageUrl.isNotEmpty()) {
            holder.ivProductImage.load(product.imageUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(8f))
            }
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_package) // Fallback
        }

        // Status logic
        holder.ivRejectionInfo.visibility = View.GONE
        
        val bgDrawable = GradientDrawable()
        bgDrawable.cornerRadius = 6f * context.resources.displayMetrics.density

        when (product.status.lowercase(Locale.getDefault())) {
            "published", "approved", "active" -> {
                holder.tvStatusPill.text = "PUBLISHED"
                bgDrawable.setColor(Color.parseColor("#F0FBF4"))
                holder.tvStatusPill.setTextColor(Color.parseColor("#1E7A45"))
            }
            "pending" -> {
                holder.tvStatusPill.text = "PENDING REVIEW"
                bgDrawable.setColor(Color.parseColor("#FFF7E8"))
                holder.tvStatusPill.setTextColor(Color.parseColor("#D98E04"))
            }
            "rejected" -> {
                holder.tvStatusPill.text = "REJECTED"
                bgDrawable.setColor(Color.parseColor("#FDECEA"))
                holder.tvStatusPill.setTextColor(Color.parseColor("#D8342A"))
                if (product.rejectionReason.isNotEmpty()) {
                    holder.ivRejectionInfo.visibility = View.VISIBLE
                    holder.ivRejectionInfo.setOnClickListener {
                        android.widget.Toast.makeText(context, "Reason: ${product.rejectionReason}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            "draft" -> {
                holder.tvStatusPill.text = "DRAFT"
                bgDrawable.setColor(Color.parseColor("#F0F1F3"))
                holder.tvStatusPill.setTextColor(Color.parseColor("#5B6472"))
            }
            else -> {
                holder.tvStatusPill.text = product.status.uppercase()
                bgDrawable.setColor(Color.parseColor("#F0F1F3"))
                holder.tvStatusPill.setTextColor(Color.parseColor("#5B6472"))
            }
        }
        holder.tvStatusPill.background = bgDrawable
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<SellerProduct>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = products.size
            override fun getNewListSize() = newProducts.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                products[oldItemPosition].id == newProducts[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                products[oldItemPosition] == newProducts[newItemPosition]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        products = newProducts
        diffResult.dispatchUpdatesTo(this)
    }
}
