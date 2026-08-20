package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Step5PoliciesFragment : Fragment(R.layout.fragment_wizard_step5_policies), WizardStep {

    private lateinit var spinnerReturnPolicy: Spinner
    private lateinit var spinnerWarranty: Spinner
    private lateinit var tilCustomWarranty: TextInputLayout
    private lateinit var etCustomWarranty: TextInputEditText
    private lateinit var etSearchTagsInput: TextInputEditText
    private lateinit var cgTags: ChipGroup

    private val currentTags = mutableListOf<String>()

    private val returnPolicyOptions = listOf(
        "7 Days Replacement",
        "7 Days Return & Refund",
        "15 Days Return",
        "No Returns (Transit Damage Only)",
        "30 Days Return"
    )

    private val warrantyOptions = listOf(
        "No Warranty",
        "6 Months Brand Warranty",
        "1 Year Manufacturer Warranty",
        "2 Years Manufacturer Warranty",
        "Specify Custom Warranty"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerReturnPolicy = view.findViewById(R.id.spinnerReturnPolicy)
        spinnerWarranty = view.findViewById(R.id.spinnerWarranty)
        tilCustomWarranty = view.findViewById(R.id.tilCustomWarranty)
        etCustomWarranty = view.findViewById(R.id.etCustomWarranty)
        etSearchTagsInput = view.findViewById(R.id.etSearchTagsInput)
        cgTags = view.findViewById(R.id.cgTags)

        setupSpinners()
        setupTagsInput()
    }

    private fun setupSpinners() {
        val returnAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            returnPolicyOptions
        )
        spinnerReturnPolicy.adapter = returnAdapter
        spinnerReturnPolicy.setSelection(0)

        val warrantyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            warrantyOptions
        )
        spinnerWarranty.adapter = warrantyAdapter
        spinnerWarranty.setSelection(0)

        spinnerWarranty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == warrantyOptions.size - 1) {
                    tilCustomWarranty.visibility = View.VISIBLE
                } else {
                    tilCustomWarranty.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTagsInput() {
        etSearchTagsInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                val tagText = v.text.toString().trim()
                if (tagText.isNotEmpty() && !currentTags.contains(tagText)) {
                    addTagChip(tagText)
                    v.text = ""
                }
                true
            } else {
                false
            }
        }
    }

    private fun addTagChip(tag: String) {
        currentTags.add(tag)
        val chip = layoutInflater.inflate(R.layout.item_tag_chip, cgTags, false) as Chip
        chip.text = tag
        chip.setOnCloseIconClickListener {
            cgTags.removeView(chip)
            currentTags.remove(tag)
        }
        cgTags.addView(chip)
    }

    override fun validate(): Boolean {
        return true
    }

    override fun saveDataToViewModel(viewModel: AddProductWizardViewModel) {
        viewModel.searchTags = currentTags.toList()
        viewModel.returnPolicy = spinnerReturnPolicy.selectedItem?.toString() ?: "7 Days Replacement"
        
        val selectedWarranty = spinnerWarranty.selectedItem?.toString() ?: "No Warranty"
        if (selectedWarranty == "Specify Custom Warranty") {
            val custom = etCustomWarranty.text.toString().trim()
            viewModel.warranty = if (custom.isNotEmpty()) custom else "No Warranty"
        } else {
            viewModel.warranty = selectedWarranty
        }
    }
}
