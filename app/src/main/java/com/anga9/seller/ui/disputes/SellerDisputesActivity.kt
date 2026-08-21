package com.anga9.seller.ui.disputes

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.DisputeItem
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SellerDisputesActivity : BaseActivity() {

    private val viewModel: DisputesViewModel by viewModels()
    private lateinit var adapter: SellerDisputeAdapter

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: MaterialCardView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var rvDisputes: RecyclerView

    // Banner
    private lateinit var cardActiveBanner: MaterialCardView
    private lateinit var flBannerIconBg: FrameLayout
    private lateinit var ivBannerIcon: ImageView
    private lateinit var tvBannerTitle: TextView
    private lateinit var tvBannerSubtitle: TextView

    // Chips
    private lateinit var chipAll: TextView
    private lateinit var chipActionRequired: TextView
    private lateinit var chipInReview: TextView
    private lateinit var chipResolved: TextView

    private var activeBottomSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_disputes)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        rvDisputes = findViewById(R.id.rvDisputes)

        cardActiveBanner = findViewById(R.id.cardActiveBanner)
        flBannerIconBg = findViewById(R.id.flBannerIconBg)
        ivBannerIcon = findViewById(R.id.ivBannerIcon)
        tvBannerTitle = findViewById(R.id.tvBannerTitle)
        tvBannerSubtitle = findViewById(R.id.tvBannerSubtitle)

        chipAll = findViewById(R.id.chipAll)
        chipActionRequired = findViewById(R.id.chipActionRequired)
        chipInReview = findViewById(R.id.chipInReview)
        chipResolved = findViewById(R.id.chipResolved)
    }

    private fun setupRecyclerView() {
        adapter = SellerDisputeAdapter(
            onItemClick = { dispute -> showDisputeDetailsBottomSheet(dispute) },
            onResolveClick = { dispute -> showDisputeDetailsBottomSheet(dispute) }
        )
        rvDisputes.layoutManager = LinearLayoutManager(this)
        rvDisputes.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadDisputes(viewModel.currentFilterKey)
        }

        chipAll.setOnClickListener {
            updateChipSelection(chipAll, "all")
        }
        chipActionRequired.setOnClickListener {
            updateChipSelection(chipActionRequired, "open")
        }
        chipInReview.setOnClickListener {
            updateChipSelection(chipInReview, "in_review")
        }
        chipResolved.setOnClickListener {
            updateChipSelection(chipResolved, "resolved")
        }
    }

    private fun updateChipSelection(selectedChip: TextView, filterKey: String) {
        val chips = listOf(chipAll, chipActionRequired, chipInReview, chipResolved)
        for (chip in chips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.parseColor("#4B5563"))
            }
        }
        viewModel.setFilter(filterKey)
    }

    private fun observeViewModel() {
        // Observe disputes list
        lifecycleScope.launch {
            viewModel.disputesState.collectLatest { state ->
                when (state) {
                    is UiState.Idle -> {
                        progressBar.visibility = View.GONE
                    }
                    is UiState.Loading -> {
                        if (!swipeRefresh.isRefreshing) {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        val list = state.data
                        adapter.submitList(list)

                        if (list.isEmpty()) {
                            rvDisputes.visibility = View.GONE
                            layoutEmpty.visibility = View.VISIBLE
                            val filter = viewModel.currentFilterKey
                            tvEmptyTitle.text = if (filter == "all") "No disputes found" else "No $filter disputes"
                            tvEmptySubtitle.text = "Customer-raised return and refund issues will appear here."
                        } else {
                            rvDisputes.visibility = View.VISIBLE
                            layoutEmpty.visibility = View.GONE
                        }
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@SellerDisputesActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Observe active issues count for banner styling
        lifecycleScope.launch {
            viewModel.activeIssuesCount.collectLatest { activeCount ->
                updateBannerUI(activeCount)
            }
        }

        // Observe response submission state
        lifecycleScope.launch {
            viewModel.respondState.collectLatest { state ->
                when (state) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> {
                        // Handled inside active bottom sheet
                    }
                    is UiState.Success -> {
                        if (state.data != null) {
                            Toast.makeText(this@SellerDisputesActivity, "Response sent successfully", Toast.LENGTH_SHORT).show()
                            activeBottomSheet?.dismiss()
                            viewModel.resetRespondState()
                        }
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@SellerDisputesActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateBannerUI(activeCount: Int) {
        if (activeCount == 0) {
            // Neutral Blue Tone
            cardActiveBanner.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
            cardActiveBanner.strokeColor = Color.parseColor("#BFDBFE")
            flBannerIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
            ivBannerIcon.setImageResource(R.drawable.ic_privacy_shield)
            ivBannerIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#1A6FD4"))

            tvBannerTitle.text = "0 active issues"
            tvBannerTitle.setTextColor(Color.parseColor("#1E40AF"))
            tvBannerSubtitle.text = "You're all caught up! Great job maintaining customer satisfaction."
            tvBannerSubtitle.setTextColor(Color.parseColor("#3B82F6"))
        } else {
            // Urgent Red Tone
            cardActiveBanner.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            cardActiveBanner.strokeColor = Color.parseColor("#FCA5A5")
            flBannerIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            ivBannerIcon.setImageResource(R.drawable.ic_warning_triangle)
            ivBannerIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))

            tvBannerTitle.text = "$activeCount active ${if (activeCount == 1) "issue needs" else "issues need"} attention"
            tvBannerTitle.setTextColor(Color.parseColor("#991B1B"))
            tvBannerSubtitle.text = "Resolve open disputes promptly to avoid account penalties."
            tvBannerSubtitle.setTextColor(Color.parseColor("#B91C1C"))
        }
    }

    private fun showDisputeDetailsBottomSheet(dispute: DisputeItem) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_resolve_dispute, null)
        bottomSheet.setContentView(view)
        activeBottomSheet = bottomSheet

        // Header
        val tvSheetOrderId = view.findViewById<TextView>(R.id.tvSheetOrderId)
        val btnSheetClose = view.findViewById<ImageView>(R.id.btnSheetClose)
        tvSheetOrderId.text = "Order #${dispute.orderId.take(8)}"
        btnSheetClose.setOnClickListener { bottomSheet.dismiss() }

        // Badges & Reason
        val tvSheetQtyBadge = view.findViewById<TextView>(R.id.tvSheetQtyBadge)
        val tvSheetRefundBadge = view.findViewById<TextView>(R.id.tvSheetRefundBadge)
        val tvSheetReason = view.findViewById<TextView>(R.id.tvSheetReason)

        tvSheetQtyBadge.text = "Qty: ${dispute.requestedQty ?: 1}"
        if (dispute.refundAmount != null && dispute.refundAmount > 0) {
            tvSheetRefundBadge.visibility = View.VISIBLE
            tvSheetRefundBadge.text = "Refund: ₹${String.format("%.0f", dispute.refundAmount)}"
        } else {
            tvSheetRefundBadge.visibility = View.GONE
        }
        tvSheetReason.text = if (dispute.reason.isNotBlank()) dispute.reason else "Customer raised a dispute for this item."

        // Evidence photos
        val layoutSheetEvidence = view.findViewById<LinearLayout>(R.id.layoutSheetEvidence)
        val containerEvidence = view.findViewById<LinearLayout>(R.id.containerSheetEvidenceImages)
        if (dispute.evidenceImages.isNotEmpty()) {
            layoutSheetEvidence.visibility = View.VISIBLE
            containerEvidence.removeAllViews()
            for (imgUrl in dispute.evidenceImages) {
                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                        marginEnd = 16
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    load(imgUrl) {
                        placeholder(R.drawable.bg_rounded_8dp)
                        error(R.drawable.bg_rounded_8dp)
                    }
                }
                containerEvidence.addView(iv)
            }
        } else {
            layoutSheetEvidence.visibility = View.GONE
        }

        // Previous Seller Response
        val layoutPrevResponse = view.findViewById<LinearLayout>(R.id.layoutSheetPreviousResponse)
        val tvSheetSellerResponse = view.findViewById<TextView>(R.id.tvSheetSellerResponse)
        if (!dispute.sellerResponse.isNullOrBlank()) {
            layoutPrevResponse.visibility = View.VISIBLE
            tvSheetSellerResponse.text = dispute.sellerResponse
        } else {
            layoutPrevResponse.visibility = View.GONE
        }

        // Admin Resolution
        val layoutAdminResolution = view.findViewById<LinearLayout>(R.id.layoutSheetAdminResolution)
        val tvSheetAdminResolution = view.findViewById<TextView>(R.id.tvSheetAdminResolution)
        val adminRes = dispute.adminResolution ?: dispute.adminDecision
        if (!adminRes.isNullOrBlank()) {
            layoutAdminResolution.visibility = View.VISIBLE
            tvSheetAdminResolution.text = adminRes
        } else {
            layoutAdminResolution.visibility = View.GONE
        }

        // Response Form (Only active when status is action_required / open)
        val layoutResponseForm = view.findViewById<LinearLayout>(R.id.layoutSheetResponseForm)
        val isActionable = DisputeConfig.isActionRequired(dispute.status)

        if (isActionable) {
            layoutResponseForm.visibility = View.VISIBLE

            var selectedQc = "pending"
            val btnQcPending = view.findViewById<TextView>(R.id.btnQcPending)
            val btnQcPassed = view.findViewById<TextView>(R.id.btnQcPassed)
            val btnQcFailed = view.findViewById<TextView>(R.id.btnQcFailed)
            val qcButtons = listOf(btnQcPending, btnQcPassed, btnQcFailed)

            fun selectQc(qc: String) {
                selectedQc = qc
                btnQcPending.apply {
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (qc == "pending") "#EFF6FF" else "#F3F4F6"))
                    setTextColor(Color.parseColor(if (qc == "pending") "#1A6FD4" else "#4B5563"))
                }
                btnQcPassed.apply {
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (qc == "passed") "#ECFDF5" else "#F3F4F6"))
                    setTextColor(Color.parseColor(if (qc == "passed") "#059669" else "#4B5563"))
                }
                btnQcFailed.apply {
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (qc == "failed") "#FEF2F2" else "#F3F4F6"))
                    setTextColor(Color.parseColor(if (qc == "failed") "#DC2626" else "#4B5563"))
                }
            }

            btnQcPending.setOnClickListener { selectQc("pending") }
            btnQcPassed.setOnClickListener { selectQc("passed") }
            btnQcFailed.setOnClickListener { selectQc("failed") }
            selectQc(dispute.qcStatus ?: "pending")

            val etResponse = view.findViewById<EditText>(R.id.etSheetResponse)
            val tvError = view.findViewById<TextView>(R.id.tvSheetError)
            val btnCancel = view.findViewById<MaterialButton>(R.id.btnSheetCancel)
            val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSheetSubmit)
            val progressSubmit = view.findViewById<ProgressBar>(R.id.progressSheetSubmit)

            btnCancel.setOnClickListener { bottomSheet.dismiss() }

            btnSubmit.setOnClickListener {
                val respText = etResponse.text.toString().trim()
                if (respText.length < 5) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Response must be at least 5 characters"
                    return@setOnClickListener
                }
                tvError.visibility = View.GONE

                btnSubmit.isEnabled = false
                btnSubmit.text = ""
                progressSubmit.visibility = View.VISIBLE

                viewModel.submitResponse(
                    orderId = dispute.orderId,
                    disputeId = dispute.id,
                    responseText = respText,
                    qcStatus = selectedQc
                )
            }
        } else {
            layoutResponseForm.visibility = View.GONE
        }

        bottomSheet.show()
    }
}
