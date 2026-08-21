package com.anga9.seller.ui.orders

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.network.model.SellerOrderResponse
import com.anga9.seller.network.model.StatusHistoryResponse
import com.anga9.seller.utils.Resource
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OrderDetailActivity : BaseActivity() {

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var orderId: String

    // Adapters
    private lateinit var itemAdapter: OrderItemAdapter
    private lateinit var timelineAdapter: OrderTimelineAdapter

    // Views
    private lateinit var btnBackContainer: View
    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvPlacedDate: TextView
    private lateinit var progressBar: ProgressBar

    // Items Card
    private lateinit var rvOrderItems: RecyclerView
    private lateinit var tvItemsSubtotal: TextView

    // Price Breakdown Card
    private lateinit var tvBreakdownItemsTotal: TextView
    private lateinit var tvBreakdownDiscount: TextView
    private lateinit var tvBreakdownGst: TextView
    private lateinit var tvBreakdownDelivery: TextView
    private lateinit var tvBreakdownTotal: TextView
    private lateinit var tvEarningsLabel: TextView
    private lateinit var tvEarningsAmount: TextView

    // Timeline Card
    private lateinit var rvStatusHistory: RecyclerView

    // Sticky Action Bar
    private lateinit var btnFulfillmentAction: MaterialButton
    private lateinit var progressAction: ProgressBar

    private var currentOrder: SellerOrderResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        orderId = intent.getStringExtra("orderId")
            ?: intent.getStringExtra("ORDER_ID")
            ?: run {
                Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        initViews()
        setupRecyclerViews()
        observeViewModel()

        viewModel.loadOrderDetail(orderId)
    }

    private fun initViews() {
        btnBackContainer = findViewById(R.id.btnBackContainer)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvPlacedDate = findViewById(R.id.tvPlacedDate)
        progressBar = findViewById(R.id.progressBar)

        rvOrderItems = findViewById(R.id.rvOrderItems)
        tvItemsSubtotal = findViewById(R.id.tvItemsSubtotal)

        tvBreakdownItemsTotal = findViewById(R.id.tvBreakdownItemsTotal)
        tvBreakdownDiscount = findViewById(R.id.tvBreakdownDiscount)
        tvBreakdownGst = findViewById(R.id.tvBreakdownGst)
        tvBreakdownDelivery = findViewById(R.id.tvBreakdownDelivery)
        tvBreakdownTotal = findViewById(R.id.tvBreakdownTotal)
        tvEarningsLabel = findViewById(R.id.tvEarningsLabel)
        tvEarningsAmount = findViewById(R.id.tvEarningsAmount)

        rvStatusHistory = findViewById(R.id.rvStatusHistory)

        btnFulfillmentAction = findViewById(R.id.btnFulfillmentAction)
        progressAction = findViewById(R.id.progressAction)

        btnBackContainer.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        itemAdapter = OrderItemAdapter()
        rvOrderItems.layoutManager = LinearLayoutManager(this)
        rvOrderItems.isNestedScrollingEnabled = false
        rvOrderItems.adapter = itemAdapter

        timelineAdapter = OrderTimelineAdapter()
        rvStatusHistory.layoutManager = LinearLayoutManager(this)
        rvStatusHistory.isNestedScrollingEnabled = false
        rvStatusHistory.adapter = timelineAdapter
    }

    private fun observeViewModel() {
        viewModel.orderDetailState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    result.data?.let { bindOrder(it) }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message ?: "Failed to load order", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.updateStatusState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    progressAction.visibility = View.VISIBLE
                    btnFulfillmentAction.text = ""
                    btnFulfillmentAction.isEnabled = false
                }
                is Resource.Success -> {
                    progressAction.visibility = View.GONE
                    Toast.makeText(this, "Order status updated successfully", Toast.LENGTH_SHORT).show()
                    viewModel.loadOrderDetail(orderId)
                }
                is Resource.Error -> {
                    progressAction.visibility = View.GONE
                    btnFulfillmentAction.isEnabled = true
                    currentOrder?.let { bindFulfillmentButton(it) }
                    Toast.makeText(this, result.message ?: "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindOrder(order: SellerOrderResponse) {
        currentOrder = order

        // 1. Order ID
        val orderNum = order.orderNumber ?: order.id.take(8).uppercase()
        tvOrderId.text = if (orderNum.startsWith("ANGA")) "#$orderNum" else "#ANGA-$orderNum"

        // 2. Status Pill
        val statusKey = order.getEffectiveStatus().lowercase()
        val style = OrderStatusConfig.config[statusKey] ?: OrderStatusConfig.config["pending"]!!
        tvOrderStatus.text = style.label.uppercase()
        tvOrderStatus.setTextColor(style.getTextColor())

        val pillDrawable = GradientDrawable()
        pillDrawable.cornerRadius = 32f
        pillDrawable.setColor(style.getBgColor())
        if (style.border != null) {
            pillDrawable.setStroke(2, style.getBorderColor())
        }
        tvOrderStatus.background = pillDrawable

        // 3. Placed Timestamp (Real Date in IST, matching Web Seller)
        val formattedDate = formatIsoDate(order.getEffectiveDate())
        tvPlacedDate.text = "Placed $formattedDate"

        // 4. Order Items (Seller Items & Subtotal)
        itemAdapter.submitList(order.items)
        tvItemsSubtotal.text = formatINR(order.getSellerTotal())

        // 5. Price Breakdown (Seller Items Total)
        tvBreakdownItemsTotal.text = formatINR(order.getSellerTotal())
        tvBreakdownDiscount.text = "-${formatINR(order.getEffectiveDiscount())}"
        tvBreakdownGst.text = formatINR(0.0)
        tvBreakdownDelivery.text = formatINR(0.0)
        tvBreakdownTotal.text = formatINR(order.getSellerTotal())

        // Your Earnings (95%)
        tvEarningsLabel.text = "Your Earnings (95%)"
        tvEarningsAmount.text = formatINR(order.getSellerEarnings(0.95))

        // 6. Timeline Card
        bindTimeline(order)

        // 7. Sticky Fulfillment Button
        bindFulfillmentButton(order)
    }

    private fun bindTimeline(order: SellerOrderResponse) {
        val history = order.statusHistory
        if (!history.isNullOrEmpty()) {
            timelineAdapter.submitList(history)
        } else {
            // Fallback timeline events based on order data
            val syntheticHistory = mutableListOf<StatusHistoryResponse>()
            val effectiveDate = order.getEffectiveDate()
            val effectiveStatus = order.getEffectiveStatus().lowercase()

            syntheticHistory.add(
                StatusHistoryResponse(
                    status = "Order placed",
                    createdAt = effectiveDate
                )
            )

            syntheticHistory.add(
                StatusHistoryResponse(
                    status = if (order.paymentMethod?.lowercase() == "cod") "COD order auto-confirmed at placement" else "Order confirmed",
                    createdAt = effectiveDate
                )
            )

            if (effectiveStatus == "processing") {
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Processing",
                        createdAt = order.updatedAt ?: effectiveDate,
                        reason = "Order is being packed and processed"
                    )
                )
            } else if (effectiveStatus == "shipped") {
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Processing",
                        createdAt = effectiveDate
                    )
                )
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Shipped",
                        createdAt = order.updatedAt ?: effectiveDate,
                        reason = if (!order.trackingNumber.isNullOrEmpty()) "Tracking: ${order.trackingNumber}" else "Handed over to courier"
                    )
                )
            } else if (effectiveStatus == "delivered") {
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Processing",
                        createdAt = effectiveDate
                    )
                )
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Shipped",
                        createdAt = effectiveDate
                    )
                )
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Delivered",
                        createdAt = order.updatedAt ?: effectiveDate,
                        reason = "Delivered to customer"
                    )
                )
            } else if (effectiveStatus == "cancelled") {
                syntheticHistory.add(
                    StatusHistoryResponse(
                        status = "Cancelled",
                        createdAt = order.updatedAt ?: effectiveDate,
                        reason = "Order cancelled"
                    )
                )
            }

            timelineAdapter.submitList(syntheticHistory)
        }
    }

    private fun bindFulfillmentButton(order: SellerOrderResponse) {
        val status = order.getEffectiveStatus().lowercase()
        btnFulfillmentAction.isEnabled = true
        progressAction.visibility = View.GONE

        when (status) {
            "pending", "confirmed" -> {
                btnFulfillmentAction.text = "Mark as Processing"
                btnFulfillmentAction.setIconResource(R.drawable.ic_package)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8720C"))
                btnFulfillmentAction.setOnClickListener {
                    viewModel.updateOrderStatus(orderId = order.id, status = "processing")
                }
            }
            "processing" -> {
                btnFulfillmentAction.text = "Mark as Shipped"
                btnFulfillmentAction.setIconResource(R.drawable.ic_truck)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8720C"))
                btnFulfillmentAction.setOnClickListener {
                    viewModel.updateOrderStatus(orderId = order.id, status = "shipped")
                }
            }
            "shipped" -> {
                btnFulfillmentAction.text = "Mark as Delivered"
                btnFulfillmentAction.setIconResource(R.drawable.ic_check_circle)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8720C"))
                btnFulfillmentAction.setOnClickListener {
                    viewModel.updateOrderStatus(orderId = order.id, status = "delivered")
                }
            }
            "delivered" -> {
                btnFulfillmentAction.text = "Fulfillment Completed"
                btnFulfillmentAction.setIconResource(R.drawable.ic_check_circle)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))
                btnFulfillmentAction.isEnabled = false
                btnFulfillmentAction.setOnClickListener(null)
            }
            "cancelled" -> {
                btnFulfillmentAction.text = "Order Cancelled"
                btnFulfillmentAction.setIconResource(R.drawable.ic_cancel)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                btnFulfillmentAction.isEnabled = false
                btnFulfillmentAction.setOnClickListener(null)
            }
            else -> {
                btnFulfillmentAction.text = "Mark as Processing"
                btnFulfillmentAction.setIconResource(R.drawable.ic_package)
                btnFulfillmentAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8720C"))
                btnFulfillmentAction.setOnClickListener {
                    viewModel.updateOrderStatus(orderId = order.id, status = "processing")
                }
            }
        }
    }

    private fun formatINR(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
        return format.format(amount)
    }

    private fun formatIsoDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Date N/A"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        var date: Date? = null
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                date = sdf.parse(dateString)
                if (date != null) break
            } catch (e: Exception) {
                // try next pattern
            }
        }
        if (date == null) return dateString
        val outSdf = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return outSdf.format(date).lowercase()
    }
}
