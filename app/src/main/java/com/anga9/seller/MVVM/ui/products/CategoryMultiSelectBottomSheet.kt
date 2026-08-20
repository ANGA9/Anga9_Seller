package com.anga9.seller.MVVM.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import com.anga9.seller.R
import com.anga9.seller.network.model.CategoryResponse
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryMultiSelectBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tvSelectionCount: TextView
    private lateinit var tvMaxWarning: TextView
    private lateinit var btnDone: Button
    private lateinit var adapter: CategoryAdapter

    private var selectedCategoryIds = mutableSetOf<String>()
    private var onSelectionChanged: ((List<String>) -> Unit)? = null
    private var allCategories = listOf<CategoryResponse>()
    private var displayList = listOf<CategoryItem>()

    data class CategoryItem(val category: CategoryResponse, val isChild: Boolean)

    fun setAllCategories(categories: List<CategoryResponse>) {
        allCategories = categories
        buildDisplayList()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun buildDisplayList() {
        val roots = allCategories.filter { it.parentId == null || it.parentId.isEmpty() }
        val result = mutableListOf<CategoryItem>()
        for (root in roots) {
            result.add(CategoryItem(root, false))
            val children = allCategories.filter { it.parentId == root.id }
            for (child in children) {
                result.add(CategoryItem(child, true))
            }
        }
        displayList = result
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_category_multi_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCategories = view.findViewById(R.id.rvCategories)
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount)
        tvMaxWarning = view.findViewById(R.id.tvMaxWarning)
        btnDone = view.findViewById(R.id.btnDone)

        adapter = CategoryAdapter()
        rvCategories.layoutManager = LinearLayoutManager(context)
        rvCategories.adapter = adapter

        updateCount()

        btnDone.setOnClickListener {
            onSelectionChanged?.invoke(selectedCategoryIds.toList())
            dismiss()
        }
    }

    fun setSelectedCategories(categories: List<String>) {
        selectedCategoryIds.clear()
        selectedCategoryIds.addAll(categories)
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
            updateCount()
        }
    }

    fun setOnSelectionChangedListener(listener: (List<String>) -> Unit) {
        onSelectionChanged = listener
    }

    private fun updateCount() {
        val count = selectedCategoryIds.size
        tvSelectionCount.text = "$count/5"
        tvMaxWarning.visibility = if (count >= 5) View.VISIBLE else View.GONE
    }

    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val cbCategory: CheckBox = view.findViewById(R.id.cbCategory)

            init {
                view.setOnClickListener {
                    val categoryId = displayList[adapterPosition].category.id
                    if (selectedCategoryIds.contains(categoryId)) {
                        selectedCategoryIds.remove(categoryId)
                        notifyItemChanged(adapterPosition)
                        updateCount()
                    } else {
                        if (selectedCategoryIds.size < 5) {
                            selectedCategoryIds.add(categoryId)
                            notifyItemChanged(adapterPosition)
                            updateCount()
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_picker_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = displayList[position]
            val category = item.category
            holder.tvName.text = category.name.replaceFirstChar { it.uppercase() }
            
            val density = holder.itemView.context.resources.displayMetrics.density
            val verticalPadding = (12 * density).toInt()
            if (item.isChild) {
                holder.itemView.setPadding((40 * density).toInt(), verticalPadding, (20 * density).toInt(), verticalPadding)
                holder.tvName.typeface = android.graphics.Typeface.DEFAULT
            } else {
                holder.itemView.setPadding((20 * density).toInt(), verticalPadding, (20 * density).toInt(), verticalPadding)
                holder.tvName.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            
            holder.cbCategory.isChecked = selectedCategoryIds.contains(category.id)
            
            // Disable unchecked items if max limit is reached
            if (!holder.cbCategory.isChecked && selectedCategoryIds.size >= 5) {
                holder.itemView.alpha = 0.5f
                holder.cbCategory.isEnabled = false
            } else {
                holder.itemView.alpha = 1.0f
                holder.cbCategory.isEnabled = true
            }
        }

        override fun getItemCount() = displayList.size
    }
}
