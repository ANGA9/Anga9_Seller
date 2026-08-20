package com.anga9.seller.ui.reviews

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anga9.seller.databinding.ActivityReviewsBinding
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.launch

class ReviewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewsBinding
    private val viewModel: ReviewsViewModel by viewModels()
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupRecyclerView()
        setupSearchAndSort()
        observeViewModel()

        viewModel.loadReviews()
    }

    private fun setupHeader() {
        binding.header.tvHeaderTitle.text = "Customer Reviews"
        binding.header.tvHeaderSubtitle.text = "See what customers are saying about your products"
        binding.header.btnBackStacked.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter { productId ->
            val intent = Intent(this, com.anga9.seller.MVVM.ui.products.EditProductActivity::class.java).apply {
                putExtra("product_id", productId)
            }
            startActivity(intent)
        }
        binding.rvReviews.adapter = reviewAdapter
    }

    private fun setupSearchAndSort() {
        binding.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.applyFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.sortFilter.btnSort.setOnClickListener {
            val bottomSheet = SortReviewsBottomSheet(viewModel.currentSort.value) { sortValue, sortLabel ->
                binding.sortFilter.tvSortLabel.text = sortLabel
                viewModel.loadReviews(sortValue)
            }
            bottomSheet.show(supportFragmentManager, "SortReviews")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.rvReviews.visibility = View.GONE
                                binding.emptyState.emptyStateContainer.visibility = View.GONE
                                binding.tvError.visibility = View.GONE
                            }
                            is UiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvError.visibility = View.GONE
                                // The actual list update happens in filteredReviews observer
                            }
                            is UiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.rvReviews.visibility = View.GONE
                                binding.emptyState.emptyStateContainer.visibility = View.GONE
                                binding.tvError.visibility = View.VISIBLE
                                binding.tvError.text = state.message
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.filteredReviews.collect { reviews ->
                        if (viewModel.uiState.value is UiState.Success) {
                            if (reviews.isEmpty()) {
                                binding.rvReviews.visibility = View.GONE
                                binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                                binding.emptyState.tvEmptyHeading.text = "No Reviews Found"
                                binding.emptyState.tvEmptyDesc.text = "No customer reviews match your current filters."
                                binding.emptyState.btnEmptyAction.visibility = View.GONE
                            } else {
                                binding.rvReviews.visibility = View.VISIBLE
                                binding.emptyState.emptyStateContainer.visibility = View.GONE
                                reviewAdapter.submitList(reviews)
                            }
                        }
                    }
                }
            }
        }
    }
}
