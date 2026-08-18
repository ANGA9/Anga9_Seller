package com.anga9.seller.MVVM.ui.products.wizard

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.AddProductWizardViewModel
import com.anga9.seller.MVVM.ui.products.WizardStep
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class Step5PoliciesFragment : Fragment(R.layout.fragment_wizard_step5_policies), WizardStep {

    private lateinit var etSearchTagsInput: TextInputEditText
    private lateinit var cgTags: ChipGroup
    private val currentTags = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etSearchTagsInput = view.findViewById(R.id.etSearchTagsInput)
        cgTags = view.findViewById(R.id.cgTags)

        etSearchTagsInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
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
        viewModel.returnPolicy = "7-day replacement only"
        viewModel.warranty = "No warranty"
    }
}

