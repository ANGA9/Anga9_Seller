package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class Step2BasicDetailsFragment : Fragment(R.layout.fragment_wizard_step2_basic), WizardStep {

    private lateinit var etName: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSubcategory: Spinner
    
    // Fallback map in case of network error, but we will overwrite it with live data
    private var categoriesMap: MutableMap<String, MutableList<String>> = mutableMapOf(
        "Select Category" to mutableListOf("Select Subcategory")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etName = view.findViewById(R.id.etProductName)
        etDesc = view.findViewById(R.id.etProductDesc)
        tilName = view.findViewById(R.id.tilProductName)
        tilDesc = view.findViewById(R.id.tilProductDesc)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerSubcategory = view.findViewById(R.id.spinnerSubcategory)
        
        val tipText = view.findViewById<TextView>(R.id.tvTipText)
        tipText.text = "Clear names with material and size (e.g. '2mm' not 'medium') get 3x more buyer searches."
        
        setupSpinners()
        fetchLiveCategories()
    }

    private fun fetchLiveCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://plfaugkadavxenpqawzw.supabase.co/rest/v1/categories?select=id,name,parent_id"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc")
                    .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsZmF1Z2thZGF2eGVucHFhd3p3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyMzY2OTgsImV4cCI6MjA5MTgxMjY5OH0.iR7aGloeXXNZPf1Vur_WPjEsqnD--MY_k53LTvmodnc")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    val parentMap = mutableMapOf<String, String>() // id -> name
                    val newCategoriesMap = mutableMapOf<String, MutableList<String>>(
                        "Select Category" to mutableListOf("Select Subcategory")
                    )

                    // First pass: Find all parents
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        if (obj.isNull("parent_id")) {
                            val name = obj.getString("name")
                            parentMap[obj.getString("id")] = name
                            newCategoriesMap[name] = mutableListOf("Select Subcategory")
                        }
                    }

                    // Second pass: Find all children and assign to parents
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        if (!obj.isNull("parent_id")) {
                            val parentId = obj.getString("parent_id")
                            val parentName = parentMap[parentId]
                            if (parentName != null) {
                                newCategoriesMap[parentName]?.add(obj.getString("name"))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        categoriesMap = newCategoriesMap
                        setupSpinners()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSpinners() {
        val categories = categoriesMap.keys.toList()
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCat = categories[position]
                val subcategories = categoriesMap[selectedCat] ?: listOf("Select Subcategory")
                
                val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, subcategories)
                spinnerSubcategory.adapter = subAdapter
                spinnerSubcategory.isEnabled = selectedCat != "Select Category"
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
        
        if (spinnerCategory.selectedItemPosition == 0) {
            isValid = false // Optionally show a toast
        }
        
        return isValid
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.productName = etName.text.toString().trim()
        viewModel.productDescription = etDesc.text.toString().trim()
        
        if (spinnerCategory.selectedItemPosition > 0) {
            viewModel.categoryId = spinnerCategory.selectedItem.toString().lowercase()
        } else {
            viewModel.categoryId = "general"
        }
        
        if (spinnerSubcategory.selectedItemPosition > 0) {
            viewModel.subcategoryId = spinnerSubcategory.selectedItem.toString().lowercase()
        }
    }
}
