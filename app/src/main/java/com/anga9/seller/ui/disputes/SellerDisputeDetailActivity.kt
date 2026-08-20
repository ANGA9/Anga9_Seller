package com.anga9.seller.ui.disputes

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import coil.load
import com.anga9.seller.R
import com.anga9.seller.network.ApiClient
import com.anga9.seller.network.model.DisputeItem
import com.anga9.seller.network.model.DisputeRespondRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * SellerDisputeDetailActivity
 * Loads dispute via GET /api/orders/:orderId/dispute
 * Responds via PATCH /api/orders/:orderId/dispute/:disputeId
 *
 * Intent extras:
 *   "orderId"   â€” required
 *   "disputeId" â€” required
 */
class SellerDisputeDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvOrderId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvOrderAmount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvReason: TextView
    private lateinit var tvDescription: TextView
    private lateinit var cardPhotos: CardView
    private lateinit var layoutPhotos: LinearLayout
    private lateinit var cardSellerAction: CardView
    private lateinit var etSellerResponse: EditText
    private lateinit var btnAcceptReplacement: Button
    private lateinit var btnAcceptRefund: Button
    private lateinit var btnReject: Button
    private lateinit var cardSellerResponseDisplay: CardView
    private lateinit var tvSellerResolution: TextView
    private lateinit var tvSellerResponseText: TextView
    private lateinit var cardAdminDecision: CardView
    private lateinit var tvAdminDecision: TextView
    private lateinit var tvRefundAmount: TextView
    private lateinit var progressBar: ProgressBar

    private var orderId = ""
    private var disputeId = ""
    private var currentDispute: DisputeItem? = null

    private lateinit var api: com.anga9.seller.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = com.anga9.seller.network.ApiClient.getApiService(this)
        setContentView(R.layout.activity_dispute_detail_seller)

        orderId   = intent.getStringExtra("orderId")   ?: ""
        disputeId = intent.getStringExtra("disputeId") ?: ""

        initViews()

        if (orderId.isEmpty()) {
            Toast.makeText(this, "Invalid dispute", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadDispute()
    }

    private fun initViews() {
        btnBack                  = findViewById(R.id.btnBack)
        tvOrderId                = findViewById(R.id.tvOrderId)
        tvStatus                 = findViewById(R.id.tvStatus)
        tvCustomerName           = findViewById(R.id.tvCustomerName)
        tvOrderAmount            = findViewById(R.id.tvOrderAmount)
        tvDate                   = findViewById(R.id.tvDate)
        tvReason                 = findViewById(R.id.tvReason)
        tvDescription            = findViewById(R.id.tvDescription)
        cardPhotos               = findViewById(R.id.cardPhotos)
        layoutPhotos             = findViewById(R.id.layoutPhotos)
        cardSellerAction         = findViewById(R.id.cardSellerAction)
        etSellerResponse         = findViewById(R.id.etSellerResponse)
        btnAcceptReplacement     = findViewById(R.id.btnAcceptReplacement)
        btnAcceptRefund          = findViewById(R.id.btnAcceptRefund)
        btnReject                = findViewById(R.id.btnReject)
        cardSellerResponseDisplay = findViewById(R.id.cardSellerResponseDisplay)
        tvSellerResolution       = findViewById(R.id.tvSellerResolution)
        tvSellerResponseText     = findViewById(R.id.tvSellerResponseText)
        cardAdminDecision        = findViewById(R.id.cardAdminDecision)
        tvAdminDecision          = findViewById(R.id.tvAdminDecision)
        tvRefundAmount           = findViewById(R.id.tvRefundAmount)
        progressBar              = findViewById(R.id.progressBar)

        btnBack.setOnClickListener { finish() }
    }

    private fun loadDispute() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = api.getOrderDispute(orderId)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val dispute = response.body()?.dispute
                    if (dispute != null) {
                        currentDispute = dispute
                        // Update disputeId if not passed via intent
                        if (disputeId.isEmpty()) disputeId = dispute.id
                        displayDispute(dispute)
                    } else {
                        Toast.makeText(this@SellerDisputeDetailActivity,
                            "Dispute not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@SellerDisputeDetailActivity,
                        "Failed to load dispute (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SellerDisputeDetailActivity,
                    "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayDispute(dispute: DisputeItem) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        tvOrderId.text      = "Order: $orderId"
        tvCustomerName.text = "Customer: ${dispute.customer?.fullName ?: "Customer"}"
        tvOrderAmount.text  = "Refund Amount: \u20B9${String.format("%,.0f", dispute.refundAmount)}"
        tvDate.text         = "Raised on: ${dispute.createdAt}"

        tvReason.text = when (dispute.type) {
            "damaged"      -> "\u26A0\uFE0F Damaged Goods"
            "wrong_item"   -> "\uD83D\uDCE6 Wrong Item Shipped"
            "return"       -> "\uD83D\uDD04 Return Request"
            "refund"       -> "\uD83D\uDCB0 Refund Request"
            "not_received" -> "\uD83D\uDCED Not Received"
            else           -> dispute.type
        }
        tvDescription.text = dispute.reason

        // Status badge
        val (statusText, statusColor) = when (dispute.status) {
            "open"          -> "OPEN - Response Required" to "#FF9800"
            "under_review"  -> "UNDER REVIEW" to "#9C27B0"
            "resolved"      -> "RESOLVED" to "#607D8B"
            "closed"        -> "CLOSED" to "#4CAF50"
            else            -> dispute.status.uppercase() to "#999999"
        }
        tvStatus.text = statusText
        tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        // Photos
        if (dispute.evidenceImages.isNotEmpty()) {
            cardPhotos.visibility = View.VISIBLE
            layoutPhotos.removeAllViews()
            dispute.evidenceImages.forEach { url ->
                val iv = ImageView(this)
                val params = LinearLayout.LayoutParams(200, 200)
                params.marginEnd = 8
                iv.layoutParams = params
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setBackgroundColor(Color.parseColor("#F0F0F0"))
                iv.load(url)
                layoutPhotos.addView(iv)
            }
        }

        // Show action card only if open
        if (dispute.status == "open") {
            cardSellerAction.visibility = View.VISIBLE
            setupActionButtons()
        } else {
            cardSellerAction.visibility = View.GONE
        }

        // Show seller response if already responded
        if (!dispute.sellerResponse.isNullOrEmpty()) {
            cardSellerResponseDisplay.visibility = View.VISIBLE
            tvSellerResolution.text = "Your Response"
            tvSellerResponseText.text = dispute.sellerResponse
        }

        // Admin decision
        if (!dispute.adminDecision.isNullOrEmpty()) {
            cardAdminDecision.visibility = View.VISIBLE
            val refund = dispute.refundAmount ?: 0.0
            if (refund > 0.0) {
                tvRefundAmount.visibility = View.VISIBLE
                tvRefundAmount.text = "Refund Amount: \u20B9${String.format("%,.0f", refund)}"
            }
        }
    }

    private fun setupActionButtons() {
        btnAcceptReplacement.setOnClickListener {
            showConfirmDialog(
                "Replacement Confirm",
                "Kya aap replacement bhejne ke liye agree karte ho?",
                "We will send a replacement for this item."
            )
        }
        btnAcceptRefund.setOnClickListener {
            showConfirmDialog(
                "Refund Confirm",
                "Kya aap refund karne ke liye agree karte ho?",
                "We agree to process a refund for this item."
            )
        }
        btnReject.setOnClickListener {
            showConfirmDialog(
                "Dispute Reject",
                "Kya aap sure ho? Reject karne par admin review mein jayega.",
                "We have reviewed this dispute and cannot accept the claim."
            )
        }
    }

    private fun showConfirmDialog(title: String, message: String, defaultResponse: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ ->
                val response = etSellerResponse.text.toString().trim()
                    .ifEmpty { defaultResponse }
                submitResponse(response)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitResponse(response: String) {
        if (disputeId.isEmpty()) {
            Toast.makeText(this, "Dispute ID missing", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = DisputeRespondRequest(
                    sellerResponse = response,
                    requestAdmin   = false
                )
                val apiResponse = api.respondToDispute(orderId, disputeId, request)
                progressBar.visibility = View.GONE
                if (apiResponse.isSuccessful) {
                    Toast.makeText(
                        this@SellerDisputeDetailActivity,
                        "Response submitted successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    loadDispute() // Refresh
                } else {
                    Toast.makeText(
                        this@SellerDisputeDetailActivity,
                        "Failed to submit (${apiResponse.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@SellerDisputeDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
