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
            binding.tvReviewBody.text = review.body ?: ""
            binding.tvCustomerName.text = "By ${review.userName.ifEmpty { "Anonymous" }}"
            
            // Format Date
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val parsedDate = java.time.ZonedDateTime.parse(review.createdAt)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US)
                    binding.tvDate.text = parsedDate.format(formatter)
                } else {
                    binding.tvDate.text = review.createdAt.substring(0, 10)
                }
            } catch (e: Exception) {
                binding.tvDate.text = review.createdAt.take(10)
            }

            // Star Rating
            val rating = review.rating.toInt()
            binding.ivStar1.setImageResource(if (rating >= 1) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            binding.ivStar2.setImageResource(if (rating >= 2) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            binding.ivStar3.setImageResource(if (rating >= 3) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            binding.ivStar4.setImageResource(if (rating >= 4) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            binding.ivStar5.setImageResource(if (rating >= 5) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            // Product Reference
            if (review.products != null) {
                binding.llProductRef.visibility = View.VISIBLE
                binding.tvProductName.text = review.products.name
                
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
