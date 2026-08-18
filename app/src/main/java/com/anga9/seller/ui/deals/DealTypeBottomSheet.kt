package com.anga9.seller.ui.deals

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DealTypeBottomSheet(
    private val onTypeSelected: (DealTypeItem) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_deal_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerDealTypes)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = DealTypeAdapter(DealTypeConfig.DEAL_TYPES) { selected ->
            onTypeSelected(selected)
            dismiss()
        }
    }

    private inner class DealTypeAdapter(
        private val items: List<DealTypeItem>,
        private val onItemClick: (DealTypeItem) -> Unit
    ) : RecyclerView.Adapter<DealTypeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
            val tvName: TextView = view.findViewById(R.id.tvName)

            init {
                view.setOnClickListener {
                    onItemClick(items[adapterPosition])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deal_type_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.title
            
            try {
                holder.ivIcon.setImageResource(item.iconRes)
                holder.ivIcon.imageTintList = ColorStateList.valueOf(item.iconTint)
            } catch (e: Exception) {
                // Fallback if resource is missing
            }
        }

        override fun getItemCount() = items.size
    }
}
