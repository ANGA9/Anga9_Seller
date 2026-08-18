package com.anga9.seller.ui.reviews

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerReview
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class SellerReviewsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvAvgRating: TextView
    private lateinit var tvStarDisplay: TextView
    private lateinit var tvRatingLabel: TextView
    private lateinit var bar5: ProgressBar
    private lateinit var bar4: ProgressBar
    private lateinit var bar3: ProgressBar
    private lateinit var bar2: ProgressBar
    private lateinit var bar1: ProgressBar
    private lateinit var tvPct5: TextView
    private lateinit var tvPct4: TextView
    private lateinit var tvPct3: TextView
    private lateinit var tvPct2: TextView
    private lateinit var tvPct1: TextView
    private lateinit var chipAll: TextView
    private lateinit var chip5: TextView
    private lateinit var chip4: TextView
    private lateinit var chip3: TextView
    private lateinit var chipCritical: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var rvReviews: RecyclerView

    private lateinit var adapter: ReviewAdapter
    private var allReviews = listOf<SellerReview>()
    private var activeFilter = 0 // 0=all, 5,4,3,-1=critical

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_reviews)
        initViews()
        setupRecyclerView()
        setupFilterChips()
        loadReviews()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvAvgRating = findViewById(R.id.tvAvgRating)
        tvStarDisplay = findViewById(R.id.tvStarDisplay)
        tvRatingLabel = findViewById(R.id.tvRatingLabel)
        bar5 = findViewById(R.id.bar5); bar4 = findViewById(R.id.bar4)
        bar3 = findViewById(R.id.bar3); bar2 = findViewById(R.id.bar2); bar1 = findViewById(R.id.bar1)
        tvPct5 = findViewById(R.id.tvPct5); tvPct4 = findViewById(R.id.tvPct4)
        tvPct3 = findViewById(R.id.tvPct3); tvPct2 = findViewById(R.id.tvPct2); tvPct1 = findViewById(R.id.tvPct1)
        chipAll = findViewById(R.id.chipAll); chip5 = findViewById(R.id.chip5)
        chip4 = findViewById(R.id.chip4); chip3 = findViewById(R.id.chip3)
        chipCritical = findViewById(R.id.chipCritical)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        rvReviews = findViewById(R.id.rvReviews)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ReviewAdapter(emptyList())
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = adapter
    }

    private fun setupFilterChips() {
        val chips = listOf(chipAll to 0, chip5 to 5, chip4 to 4, chip3 to 3, chipCritical to -1)
        chips.forEach { (chip, filter) ->
            chip.setOnClickListener {
                activeFilter = filter
                updateChipUI(chip)
                applyFilter()
            }
        }
    }

    private fun updateChipUI(selected: TextView) {
        val all = listOf(chipAll, chip5, chip4, chip3, chipCritical)
        all.forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.btn_primary_rounded)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.input_background)
                chip.setTextColor(Color.parseColor("#475569"))
            }
        }
    }

    private fun loadReviews() {
        // TODO Phase 6A: Wire to GET /api/products/:id/reviews
        progressBar.visibility = View.GONE
        allReviews = emptyList()
        updateSummary()
        applyFilter()
    }

    private fun updateSummary() {
        val total = allReviews.size
        tvTotalCount.text = "$total reviews"
        tvRatingLabel.text = "$total ratings"

        if (total == 0) {
            tvAvgRating.text = "0.0"
            tvStarDisplay.text = "\u2606\u2606\u2606\u2606\u2606"
            return
        }

        val avg = allReviews.sumOf { it.rating.toDouble() } / total
        tvAvgRating.text = String.format("%.1f", avg)
        tvStarDisplay.text = buildStarString(avg.toFloat())

        val counts = IntArray(6)
        allReviews.forEach { counts[it.rating.roundToInt().coerceIn(1, 5)]++ }

        fun pct(count: Int) = if (total > 0) (count * 100 / total) else 0

        bar5.progress = pct(counts[5]); tvPct5.text = "${pct(counts[5])}%"
        bar4.progress = pct(counts[4]); tvPct4.text = "${pct(counts[4])}%"
        bar3.progress = pct(counts[3]); tvPct3.text = "${pct(counts[3])}%"
        bar2.progress = pct(counts[2]); tvPct2.text = "${pct(counts[2])}%"
        bar1.progress = pct(counts[1]); tvPct1.text = "${pct(counts[1])}%"
    }

    private fun applyFilter() {
        val filtered = when (activeFilter) {
            0 -> allReviews
            -1 -> allReviews.filter { it.rating <= 2f }
            else -> allReviews.filter { it.rating.roundToInt() == activeFilter }
        }
        adapter.updateList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvReviews.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun buildStarString(rating: Float): String {
        val full = rating.toInt()
        val half = if (rating - full >= 0.5f) 1 else 0
        val empty = 5 - full - half
        return "\u2605".repeat(full) + (if (half == 1) "\u00BD" else "") + "\u2606".repeat(empty)
    }
}

// ─── Adapter ──────────────────────────────────────────────────────────────────

class ReviewAdapter(private var reviews: List<SellerReview>) :
    RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvVerified: TextView = view.findViewById(R.id.tvVerified)
        val tvStars: TextView = view.findViewById(R.id.tvStars)
        val tvRatingNum: TextView = view.findViewById(R.id.tvRatingNum)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvReviewText: TextView = view.findViewById(R.id.tvReviewText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val initial = review.userName.firstOrNull()?.uppercase() ?: "?"
        holder.tvAvatar.text = initial

        val colors = listOf("#2563EB", "#7C3AED", "#059669", "#DC2626", "#D97706", "#0891B2")
        val colorIndex = (initial.firstOrNull()?.code ?: 0) % colors.size
        holder.tvAvatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor(colors[colorIndex])
        )

        holder.tvUserName.text = review.userName
        holder.tvVerified.visibility = if (review.isVerifiedPurchase) View.VISIBLE else View.GONE

        val fullStars = review.rating.toInt()
        holder.tvStars.text = "\u2605".repeat(fullStars) + "\u2606".repeat(5 - fullStars)
        holder.tvRatingNum.text = String.format("%.1f", review.rating)

        holder.tvDate.text = dateFormat.format(review.createdAt)

        if (review.productName.isNotEmpty()) {
            holder.tvProductName.visibility = View.VISIBLE
            holder.tvProductName.text = review.productName
        } else {
            holder.tvProductName.visibility = View.GONE
        }

        if (review.reviewText.isNotEmpty()) {
            holder.tvReviewText.visibility = View.VISIBLE
            holder.tvReviewText.text = review.reviewText
        } else {
            holder.tvReviewText.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size

    fun updateList(newList: List<SellerReview>) {
        reviews = newList
        notifyDataSetChanged()
    }
}
