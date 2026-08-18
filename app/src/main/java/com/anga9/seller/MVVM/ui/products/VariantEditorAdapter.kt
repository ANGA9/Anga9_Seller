package com.anga9.seller.MVVM.ui.products

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data_models.ProductVariant
import com.anga9.seller.data_models.VariantSize

/**
 * Adapter for editing Size/Color variants in AddEditProductActivity.
 * Each row = one color with S/M/L/XL/XXL stock + price inputs.
 */
class VariantEditorAdapter(
    private val variants: MutableList<VariantEditorItem>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<VariantEditorAdapter.VH>() {

    // Standard garment sizes - order matters
    val STANDARD_SIZES = listOf("XS", "S", "M", "L", "XL", "XXL", "XXXL")

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val viewColorSwatch: View = view.findViewById(R.id.viewColorSwatch)
        val etColorName: EditText = view.findViewById(R.id.etColorName)
        val etColorHex: EditText = view.findViewById(R.id.etColorHex)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemoveVariant)
        val llSizeRows: LinearLayout = view.findViewById(R.id.llSizeRows)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_variant_editor, parent, false)
        return VH(view)
    }

    override fun getItemCount() = variants.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = variants[position]

        // Color name
        holder.etColorName.setText(item.colorName)
        holder.etColorName.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                variants[holder.adapterPosition].colorName = s.toString()
            }
        })

        // Color hex + swatch
        holder.etColorHex.setText(item.colorHex)
        updateSwatch(holder.viewColorSwatch, item.colorHex)
        holder.etColorHex.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val hex = s.toString().trim()
                variants[holder.adapterPosition].colorHex = hex
                updateSwatch(holder.viewColorSwatch, hex)
            }
        })

        // Remove button
        holder.btnRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) onRemove(pos)
        }

        // Build size rows
        holder.llSizeRows.removeAllViews()
        STANDARD_SIZES.forEach { size ->
            val sizeData = item.sizes[size] ?: SizeEntry(0, 0.0)
            val rowView = buildSizeRow(holder.llSizeRows, size, sizeData, position)
            holder.llSizeRows.addView(rowView)
        }
    }

    private fun buildSizeRow(
        parent: LinearLayout,
        size: String,
        sizeData: SizeEntry,
        variantPos: Int
    ): View {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * ctx.resources.displayMetrics.density).toInt() }
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Size label
        val tvSize = TextView(ctx).apply {
            text = size
            textSize = 13f
            setTextColor(Color.parseColor("#263238"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Stock input
        val etStock = EditText(ctx).apply {
            hint = "0"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(if (sizeData.stock > 0) sizeData.stock.toString() else "")
            gravity = android.view.Gravity.CENTER
            setPadding(8, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(0,
                (44 * ctx.resources.displayMetrics.density).toInt(), 1.5f).apply {
                marginStart = (6 * ctx.resources.displayMetrics.density).toInt()
                marginEnd = (6 * ctx.resources.displayMetrics.density).toInt()
            }
            background = ctx.getDrawable(R.drawable.input_background)
        }

        // Price input
        val etPrice = EditText(ctx).apply {
            hint = "₹0"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (sizeData.price > 0) sizeData.price.toString() else "")
            gravity = android.view.Gravity.CENTER
            setPadding(8, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(0,
                (44 * ctx.resources.displayMetrics.density).toInt(), 1.5f)
            background = ctx.getDrawable(R.drawable.input_background)
        }

        // Watchers to update data model
        etStock.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val stock = s.toString().toIntOrNull() ?: 0
                variants[variantPos].sizes.getOrPut(size) { SizeEntry(0, 0.0) }.stock = stock
            }
        })
        etPrice.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val price = s.toString().toDoubleOrNull() ?: 0.0
                variants[variantPos].sizes.getOrPut(size) { SizeEntry(0, 0.0) }.price = price
            }
        })

        row.addView(tvSize)
        row.addView(etStock)
        row.addView(etPrice)
        return row
    }

    private fun updateSwatch(view: View, hex: String) {
        try {
            val color = if (hex.startsWith("#") && (hex.length == 7 || hex.length == 4))
                Color.parseColor(hex) else Color.LTGRAY
            view.background?.mutate()?.setTint(color)
                ?: view.setBackgroundColor(color)
        } catch (_: Exception) {
            view.setBackgroundColor(Color.LTGRAY)
        }
    }

    /** Convert adapter items to SellerProduct variant list */
    fun toProductVariants(): List<ProductVariant> {
        return variants.filter { it.colorName.isNotBlank() }.map { item ->
            ProductVariant(
                color = item.colorName.trim(),
                colorHex = item.colorHex.trim().ifEmpty { "#000000" },
                sizes = item.sizes
                    .filter { (_, v) -> v.stock > 0 || v.price > 0 }
                    .mapValues { (_, v) -> VariantSize(stock = v.stock, price = v.price) }
            )
        }
    }

    /** Load existing variants into editor items */
    fun loadVariants(productVariants: List<ProductVariant>) {
        variants.clear()
        productVariants.forEach { pv ->
            val item = VariantEditorItem(
                colorName = pv.color,
                colorHex = pv.colorHex
            )
            pv.sizes.forEach { (size, vs) ->
                item.sizes[size] = SizeEntry(stock = vs.stock, price = vs.price)
            }
            variants.add(item)
        }
        notifyDataSetChanged()
    }

    abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}

/** Mutable editor model for one color variant */
data class VariantEditorItem(
    var colorName: String = "",
    var colorHex: String = "#000000",
    val sizes: MutableMap<String, SizeEntry> = mutableMapOf()
)

/** Mutable stock+price for one size */
data class SizeEntry(
    var stock: Int = 0,
    var price: Double = 0.0
)
