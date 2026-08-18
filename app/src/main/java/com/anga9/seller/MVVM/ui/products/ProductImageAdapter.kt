package com.anga9.seller.MVVM.ui.products

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R

class ProductImageAdapter(
    private val images: MutableList<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ProductImageAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_product_image, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.ivImage.setImageURI(images[position])
        holder.btnRemove.setOnClickListener { onRemove(holder.bindingAdapterPosition) }
    }

    override fun getItemCount() = images.size
}
