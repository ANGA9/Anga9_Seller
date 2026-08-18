package com.anga9.seller.MVVM.ui.products

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerProductResponse
import kotlin.math.roundToInt

class BulkEditPriceAdapter(
    private val viewModel: BulkEditViewModel
) : ListAdapter<SellerProductResponse, BulkEditPriceAdapter.PriceVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bulk_edit_price, parent, false)
        return PriceVH(view)
    }

    override fun onBindViewHolder(holder: PriceVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PriceVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        private val tvEditedBadge: TextView = view.findViewById(R.id.tvEditedBadge)
        private val etBasePrice: EditText = view.findViewById(R.id.etBasePrice)
        private val etSalePrice: EditText = view.findViewById(R.id.etSalePrice)
        private val tvError: TextView = view.findViewById(R.id.tvError)
        private val tvDiscountBadge: TextView = view.findViewById(R.id.tvDiscountBadge)
        private val clCard: View = view.findViewById(R.id.clCard)
        
        private var currentProductId: String = ""

        private val baseWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateViewModel()
            }
        }

        private val saleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateViewModel()
            }
        }

        fun bind(product: SellerProductResponse) {
            currentProductId = product.id
            tvProductName.text = product.name

            val editState = viewModel.priceEdits[product.id] ?: return
            
            etBasePrice.removeTextChangedListener(baseWatcher)
            etSalePrice.removeTextChangedListener(saleWatcher)

            etBasePrice.setText(editState.currentBase?.let { if (it == 0.0) "" else it.toString() } ?: "")
            etSalePrice.setText(editState.currentSale?.toString() ?: "")

            etBasePrice.addTextChangedListener(baseWatcher)
            etSalePrice.addTextChangedListener(saleWatcher)

            updateUIState()
        }

        private fun updateViewModel() {
            val baseStr = etBasePrice.text.toString()
            val saleStr = etSalePrice.text.toString()
            val base = baseStr.toDoubleOrNull()
            val sale = saleStr.toDoubleOrNull()
            viewModel.updatePrice(currentProductId, base, sale)
            updateUIState()
        }

        private fun updateUIState() {
            val editState = viewModel.priceEdits[currentProductId] ?: return
            val isDirty = editState.isDirty
            val isValid = editState.isValid

            // Card Border
            val bg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_ticket_card)?.mutate()
            if (bg is android.graphics.drawable.GradientDrawable) {
                if (isDirty) {
                    bg.setStroke(1, Color.parseColor("#1D4ED8"))
                } else {
                    bg.setStroke(1, Color.parseColor("#E5E7EB"))
                }
            }
            clCard.background = bg

            // Edited Badge
            tvEditedBadge.visibility = if (isDirty) View.VISIBLE else View.GONE

            // Validation Error
            tvError.visibility = if (!isValid) View.VISIBLE else View.GONE

            // Sale Price input styling
            val saleBg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_ticket_card)?.mutate()
            if (saleBg is android.graphics.drawable.GradientDrawable) {
                when {
                    !isValid -> {
                        saleBg.setStroke(1, Color.parseColor("#DC2626"))
                        saleBg.setColor(Color.parseColor("#FEF2F2"))
                    }
                    editState.originalSale != editState.currentSale -> {
                        saleBg.setStroke(1, Color.parseColor("#1D4ED8"))
                        saleBg.setColor(Color.parseColor("#EFF6FF"))
                    }
                    else -> {
                        saleBg.setStroke(1, Color.parseColor("#E5E7EB"))
                        saleBg.setColor(Color.parseColor("#FFFFFF"))
                    }
                }
            }
            etSalePrice.background = saleBg

            // Discount % logic
            val base = editState.currentBase ?: 0.0
            val sale = editState.currentSale
            if (sale != null && base > 0 && sale < base) {
                val discount = ((1 - (sale / base)) * 100).roundToInt()
                tvDiscountBadge.text = "$discount% off"
                tvDiscountBadge.visibility = View.VISIBLE
                
                // If Sale field was edited this session, make badge green tinted, otherwise grey.
                val badgeBg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_pill)?.mutate()
                if (editState.originalSale != editState.currentSale) {
                    badgeBg?.setTint(Color.parseColor("#ECFDF5")) // Green tint
                    tvDiscountBadge.background = badgeBg
                    tvDiscountBadge.setTextColor(Color.parseColor("#059669"))
                } else {
                    badgeBg?.setTint(Color.parseColor("#F3F4F6")) // Neutral grey
                    tvDiscountBadge.background = badgeBg
                    tvDiscountBadge.setTextColor(Color.parseColor("#4B5563"))
                }
            } else {
                tvDiscountBadge.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SellerProductResponse>() {
            override fun areItemsTheSame(oldItem: SellerProductResponse, newItem: SellerProductResponse) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SellerProductResponse, newItem: SellerProductResponse) = oldItem == newItem
        }
    }
}

class BulkEditStockAdapter(
    private val viewModel: BulkEditViewModel
) : ListAdapter<SellerProductResponse, BulkEditStockAdapter.StockVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bulk_edit_stock, parent, false)
        return StockVH(view)
    }

    override fun onBindViewHolder(holder: StockVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StockVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        private val ivWarning: ImageView = view.findViewById(R.id.ivWarning)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        private val etStock: EditText = view.findViewById(R.id.etStock)
        private val clCard: View = view.findViewById(R.id.clCard)
        
        private var currentProductId: String = ""

        private val stockWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateViewModel()
            }
        }

        fun bind(product: SellerProductResponse) {
            currentProductId = product.id
            tvProductName.text = product.name

            val editState = viewModel.stockEdits[product.id] ?: return
            
            etStock.removeTextChangedListener(stockWatcher)
            etStock.setText(editState.currentStock.toString())
            etStock.addTextChangedListener(stockWatcher)

            updateUIState()
        }

        private fun updateViewModel() {
            val stockStr = etStock.text.toString()
            val stock = stockStr.toIntOrNull() ?: 0
            viewModel.updateStock(currentProductId, stock)
            updateUIState()
        }

        private fun updateUIState() {
            val editState = viewModel.stockEdits[currentProductId] ?: return
            val stock = editState.currentStock
            val isDirty = editState.isDirty

            // Stock severity
            val badgeLabel: String
            val badgeBgColor: String
            val badgeTextColor: String
            val borderColor: String?
            
            if (stock == 0) {
                badgeLabel = "Out of Stock"
                badgeBgColor = "#FEF2F2"
                badgeTextColor = "#DC2626"
                borderColor = "#DC2626"
            } else if (stock <= 10) {
                badgeLabel = "Low Stock"
                badgeBgColor = "#FBE4E1" // Or #FEF9C3 from web
                badgeTextColor = "#B42318" // Or #CA8A04
                borderColor = "#E4562E" // Or #EAB308
            } else if (stock <= 49) {
                badgeLabel = "Limited stock"
                badgeBgColor = "#FAEEDA"
                badgeTextColor = "#854F0B"
                borderColor = "#F0A400"
            } else {
                badgeLabel = "In stock"
                badgeBgColor = "#EAF3DE"
                badgeTextColor = "#27500A"
                borderColor = null
            }

            // Warning Icon
            if (stock <= 49) {
                ivWarning.visibility = View.VISIBLE
                ivWarning.imageTintList = ColorStateList.valueOf(Color.parseColor(borderColor))
            } else {
                ivWarning.visibility = View.GONE
            }

            // Status Badge
            tvStatusBadge.text = badgeLabel
            tvStatusBadge.setTextColor(Color.parseColor(badgeTextColor))
            val badgeBg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_pill)?.mutate()
            badgeBg?.setTint(Color.parseColor(badgeBgColor))
            tvStatusBadge.background = badgeBg

            // Card & Input Border
            val cardBg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_ticket_card)?.mutate()
            if (cardBg is android.graphics.drawable.GradientDrawable) {
                if (isDirty) {
                    cardBg.setStroke(1, Color.parseColor("#1D4ED8")) // Overall dirty border takes precedence? Or severity? Let's use severity if low, else dirty.
                } else {
                    cardBg.setStroke(1, Color.parseColor("#E5E7EB"))
                }
            }
            clCard.background = cardBg
            
            val inputBg = androidx.core.content.ContextCompat.getDrawable(itemView.context, R.drawable.bg_ticket_card)?.mutate()
            if (inputBg is android.graphics.drawable.GradientDrawable) {
                if (borderColor != null) {
                    inputBg.setStroke(1, Color.parseColor(borderColor))
                } else if (isDirty) {
                    inputBg.setStroke(1, Color.parseColor("#1D4ED8"))
                    inputBg.setColor(Color.parseColor("#EFF6FF"))
                } else {
                    inputBg.setStroke(1, Color.parseColor("#E5E7EB"))
                }
            }
            etStock.background = inputBg
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SellerProductResponse>() {
            override fun areItemsTheSame(oldItem: SellerProductResponse, newItem: SellerProductResponse) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SellerProductResponse, newItem: SellerProductResponse) = oldItem == newItem
        }
    }
}
