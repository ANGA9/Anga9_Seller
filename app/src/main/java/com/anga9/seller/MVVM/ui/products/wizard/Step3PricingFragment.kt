package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import android.widget.ArrayAdapter
import android.widget.AdapterView

class Step3PricingFragment : Fragment(R.layout.fragment_wizard_step3_pricing), WizardStep {

    private lateinit var etMRP: TextInputEditText
    private lateinit var etWholesalePrice: TextInputEditText
    private lateinit var etInitialStock: TextInputEditText
    
    private lateinit var tilMRP: TextInputLayout
    private lateinit var tilWholesalePrice: TextInputLayout
    private lateinit var tilInitialStock: TextInputLayout
    
    private lateinit var tvMoqValue: TextView
    private lateinit var btnMoqMinus: ImageButton
    private lateinit var btnMoqPlus: ImageButton
    
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerGstRate: Spinner
    
    private var moq = 1
    
    private val unitsList = listOf("piece", "set", "pair", "box", "pack", "kg", "gram", "meter", "liter")
    private val gstRatesList = listOf("0%", "5%", "12%", "18%", "28%")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etMRP = view.findViewById(R.id.etMRP)
        etWholesalePrice = view.findViewById(R.id.etWholesalePrice)
        etInitialStock = view.findViewById(R.id.etInitialStock)
        
        tilMRP = view.findViewById(R.id.tilMRP)
        tilWholesalePrice = view.findViewById(R.id.tilWholesalePrice)
        tilInitialStock = view.findViewById(R.id.tilInitialStock)
        
        tvMoqValue = view.findViewById(R.id.tvMoqValue)
        btnMoqMinus = view.findViewById(R.id.btnMoqMinus)
        btnMoqPlus = view.findViewById(R.id.btnMoqPlus)
        
        spinnerUnit = view.findViewById(R.id.spinnerUnit)
        spinnerGstRate = view.findViewById(R.id.spinnerGstRate)
        
        setupSpinners()
        
        btnMoqMinus.setOnClickListener {
            if (moq > 1) {
                moq--
                tvMoqValue.text = moq.toString()
            }
        }
        
        btnMoqPlus.setOnClickListener {
            moq++
            tvMoqValue.text = moq.toString()
        }
        
        val tipText = view.findViewById<TextView>(R.id.tvTipText)
        tipText.text = "Wholesale price and min order qty are shown to all B2B buyers on your listing."
    }
    
    private fun setupSpinners() {
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, unitsList)
        spinnerUnit.adapter = unitAdapter
        
        val gstAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, gstRatesList)
        spinnerGstRate.adapter = gstAdapter
        // Set default to 18% (index 3)
        spinnerGstRate.setSelection(3)
    }

    override fun validate(): Boolean {
        var isValid = true
        val mrpStr = etMRP.text.toString().trim()
        val wholesaleStr = etWholesalePrice.text.toString().trim()
        
        if (mrpStr.isEmpty()) {
            tilMRP.error = "Required"
            isValid = false
        } else {
            tilMRP.error = null
        }
        
        if (wholesaleStr.isEmpty()) {
            tilWholesalePrice.error = "Required"
            isValid = false
        } else {
            val wholesale = wholesaleStr.toDoubleOrNull() ?: 0.0
            val mrp = mrpStr.toDoubleOrNull() ?: 0.0
            if (wholesale > mrp) {
                tilWholesalePrice.error = "Cannot exceed MRP"
                isValid = false
            } else {
                tilWholesalePrice.error = null
            }
        }
        
        if (etInitialStock.text.toString().trim().isEmpty()) {
            tilInitialStock.error = "Required"
            isValid = false
        } else {
            tilInitialStock.error = null
        }
        
        return isValid
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.mrp = etMRP.text.toString().toDoubleOrNull() ?: 0.0
        viewModel.wholesalePrice = etWholesalePrice.text.toString().toDoubleOrNull() ?: 0.0
        viewModel.initialStock = etInitialStock.text.toString().toIntOrNull() ?: 0
        viewModel.minOrderQty = moq
        
        viewModel.unit = spinnerUnit.selectedItem.toString()
        val gstString = spinnerGstRate.selectedItem.toString().replace("%", "")
        viewModel.gstRate = gstString.toDoubleOrNull() ?: 18.0
    }
}
