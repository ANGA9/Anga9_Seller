package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.R
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.anga9.seller.utils.Resource
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Step2BasicDetailsFragment : Fragment(R.layout.fragment_wizard_step2_basic), WizardStep {

    private lateinit var etName: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSubcategory: Spinner

    data class CategoryItem(val id: String, val name: String, val parentId: String?)

    private val allCategories = mutableListOf<CategoryItem>()
    private val parentCategories = mutableListOf<CategoryItem>()
    private val subcategoryMap = mutableMapOf<String, MutableList<CategoryItem>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etName = view.findViewById(R.id.etProductName)
        etDesc = view.findViewById(R.id.etProductDesc)
        tilName = view.findViewById(R.id.tilProductName)
        tilDesc = view.findViewById(R.id.tilProductDesc)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerSubcategory = view.findViewById(R.id.spinnerSubcategory)

        val tipText = view.findViewById<TextView>(R.id.tvTipText)
        tipText?.text = "Clear names with material and size (e.g. '2mm' not 'medium') get 3x more buyer searches."

        fetchLiveCategories()
    }

    private fun fetchLiveCategories() {
        val repo = ProductRepository(requireContext())
        lifecycleScope.launch {
            repo.getCategories().collectLatest { res ->
                if (res is Resource.Success) {
                    val cats = res.data ?: emptyList()
                    allCategories.clear()
                    parentCategories.clear()
                    subcategoryMap.clear()

                    parentCategories.add(CategoryItem("", "Select Category", null))

                    for (c in cats) {
                        val item = CategoryItem(c.id, c.name, c.parentId)
                        allCategories.add(item)
                        if (c.parentId == null) {
                            parentCategories.add(item)
                            subcategoryMap[c.id] = mutableListOf(CategoryItem("", "Select Subcategory", c.id))
                        }
                    }

                    for (c in cats) {
                        if (c.parentId != null) {
                            val item = CategoryItem(c.id, c.name, c.parentId)
                            subcategoryMap[c.parentId]?.add(item)
                        }
                    }

                    setupSpinners()
                }
            }
        }
    }

    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            parentCategories.map { it.name }
        )
        spinnerCategory.adapter = categoryAdapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedParent = parentCategories[position]
                    val subcats = subcategoryMap[selectedParent.id] ?: listOf(CategoryItem("", "Select Subcategory", selectedParent.id))
                    val subAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        subcats.map { it.name }
                    )
                    spinnerSubcategory.adapter = subAdapter
                    spinnerSubcategory.isEnabled = true
                } else {
                    val subAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Select Subcategory")
                    )
                    spinnerSubcategory.adapter = subAdapter
                    spinnerSubcategory.isEnabled = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun validate(): Boolean {
        var isValid = true
        if (etName.text.toString().trim().isEmpty()) {
            tilName.error = "Name is required"
            isValid = false
        } else {
            tilName.error = null
        }

        if (etDesc.text.toString().trim().isEmpty()) {
            tilDesc.error = "Description is required"
            isValid = false
        } else {
            tilDesc.error = null
        }

        if (spinnerCategory.selectedItemPosition <= 0) {
            isValid = false
        }

        return isValid
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.productName = etName.text.toString().trim()
        viewModel.productDescription = etDesc.text.toString().trim()

        val catPos = spinnerCategory.selectedItemPosition
        if (catPos > 0 && catPos < parentCategories.size) {
            val parentCat = parentCategories[catPos]
            viewModel.categoryId = parentCat.id

            val subcats = subcategoryMap[parentCat.id]
            val subPos = spinnerSubcategory.selectedItemPosition
            if (subcats != null && subPos > 0 && subPos < subcats.size) {
                viewModel.subcategoryId = subcats[subPos].id
            } else {
                viewModel.subcategoryId = ""
            }
        } else {
            viewModel.categoryId = ""
            viewModel.subcategoryId = ""
        }
    }
}
