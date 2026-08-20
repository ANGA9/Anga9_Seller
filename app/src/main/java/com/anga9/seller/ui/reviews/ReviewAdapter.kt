package com.anga9.seller.ui.reviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.databinding.ItemReviewCardBinding
import com.anga9.seller.network.model.SellerReviewItem
import coil.load
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReviewAdapter(
    private val onProductClick: (String) -> Unit
) : ListAdapter<SellerReviewItem, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemReviewCardBinding,
        private val onProductClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: SellerReviewItem) {
            binding.tvReviewTitle.text = review.title ?: ""
            binding.tvReviewBody.text = "\"${review.body ?: ""}\""

            
            // Format Date
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val parsedDate = java.time.ZonedDateTime.parse(review.createdAt)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.US)
                    binding.tvDate.text = parsedDate.format(formatter).uppercase(java.util.Locale.US)
                } else {
                    binding.tvDate.text = review.createdAt.substring(0, 10)
                }
            } catch (e: Exception) {
                binding.tvDate.text = review.createdAt.take(10)
            }

            // Star Rating
            fun updateStar(iv: android.widget.ImageView, isFilled: Boolean) {
                iv.setImageResource(if (isFilled) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                iv.setColorFilter(android.graphics.Color.parseColor(if (isFilled) "#FBBF24" else "#D1D5DB"))
            }

            val rating = review.rating.toInt()
            updateStar(binding.ivStar1, rating >= 1)
            updateStar(binding.ivStar2, rating >= 2)
            updateStar(binding.ivStar3, rating >= 3)
            updateStar(binding.ivStar4, rating >= 4)
            updateStar(binding.ivStar5, rating >= 5)

            // Product Reference
            if (review.products != null) {
                binding.llProductRef.visibility = View.VISIBLE
                binding.tvProductName.text = review.products.name
                
                val category = review.products.category
                val subcategory = review.products.subcategory
                if (category != null && subcategory != null) {
                    binding.tvProductCategory.text = "$category > $subcategory"
                    binding.tvProductCategory.visibility = View.VISIBLE
                } else if (category != null) {
                    binding.tvProductCategory.text = category
                    binding.tvProductCategory.visibility = View.VISIBLE
                } else {
                    binding.tvProductCategory.visibility = View.GONE
                }
                
                val imageUrl = review.products.images?.firstOrNull()
                if (imageUrl != null) {
                    binding.ivProductThumb.load(imageUrl) {
                        placeholder(R.drawable.bg_image_placeholder)
                        error(R.drawable.bg_image_placeholder)
                    }
                } else {
                    binding.ivProductThumb.setImageResource(R.drawable.bg_image_placeholder)
                }
                
                binding.llProductRef.setOnClickListener {
                    onProductClick(review.productId)
                }
            } else {
                binding.llProductRef.visibility = View.GONE
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<SellerReviewItem>() {
        override fun areItemsTheSame(oldItem: SellerReviewItem, newItem: SellerReviewItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SellerReviewItem, newItem: SellerReviewItem): Boolean {
            return oldItem == newItem
        }
    }
}
