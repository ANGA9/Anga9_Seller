package com.anga9.seller.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.anga9.seller.R
import com.anga9.seller.databinding.BottomSheetSortReviewsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortReviewsBottomSheet(
    private val currentSort: String,
    private val onSortSelected: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortReviewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Select the current sort
        when (currentSort) {
            "newest" -> binding.rbNewest.isChecked = true
            "rating_desc" -> binding.rbRatingDesc.isChecked = true
            "rating_asc" -> binding.rbRatingAsc.isChecked = true
            "helpful" -> binding.rbHelpful.isChecked = true
        }

        binding.rgSortOptions.setOnCheckedChangeListener { _, checkedId ->
            val sortValue = when (checkedId) {
                R.id.rbNewest -> "newest"
                R.id.rbRatingDesc -> "rating_desc"
                R.id.rbRatingAsc -> "rating_asc"
                R.id.rbHelpful -> "helpful"
                else -> "newest"
            }

            val sortLabel = when (checkedId) {
                R.id.rbNewest -> "Newest First"
                R.id.rbRatingDesc -> "Highest Rated"
                R.id.rbRatingAsc -> "Lowest Rated"
                R.id.rbHelpful -> "Most Helpful"
                else -> "Newest First"
            }

            onSortSelected(sortValue, sortLabel)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
