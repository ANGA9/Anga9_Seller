package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.google.android.material.textfield.TextInputEditText

class Step4LogisticsFragment : Fragment(R.layout.fragment_wizard_step4_logistics), WizardStep {

    private lateinit var etWeight: TextInputEditText
    private lateinit var etBrand: TextInputEditText
    private lateinit var etHsnCode: TextInputEditText
    private lateinit var etSkuCode: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etWeight = view.findViewById(R.id.etWeight)
        etBrand = view.findViewById(R.id.etBrand)
        etHsnCode = view.findViewById(R.id.etHsnCode)
        etSkuCode = view.findViewById(R.id.etSkuCode)
        
        view.findViewById<View>(R.id.btnGenerateSku).setOnClickListener {
            etSkuCode.setText("SKU-${System.currentTimeMillis().toString().takeLast(6)}")
        }
        
        val tipText = view.findViewById<TextView>(R.id.tvTipText)
        tipText.text = "HSN code decides GST slab automatically. Not sure? Tap the info icon or leave blank — we'll suggest one based on category."
    }

    override fun validate(): Boolean {
        // Logistics fields are mostly optional
        return true
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.weightKg = etWeight.text.toString().toDoubleOrNull()
        viewModel.brand = etBrand.text.toString().trim()
        viewModel.hsnCode = etHsnCode.text.toString().trim()
        viewModel.skuCode = etSkuCode.text.toString().trim()
        // Spinners omitted for MVP simplicity
        viewModel.countryOfOrigin = "India"
    }
}
