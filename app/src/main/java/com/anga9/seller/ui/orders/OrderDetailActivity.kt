package com.anga9.seller.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.SellerOrder
import com.anga9.seller.utils.GstInvoiceGenerator
import com.anga9.seller.utils.Resource
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var orderId: String

    // Views
    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var tvPoNumber: TextView
    private lateinit var tvOrderTypeRow: TextView
    private lateinit var layoutPoNumber: View
    private lateinit var tvItemsTotal: TextView
    private lateinit var tvBulkDiscount: TextView
    private lateinit var tvGst: TextView
    private lateinit var tvDeliveryCharges: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvSellerEarnings: TextView
    private lateinit var tvTrackingNumber: TextView
    private lateinit var layoutTracking: View
    private lateinit var rvItems: RecyclerView
    private lateinit var rvStatusHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutActions: View
    private lateinit var btnPrimaryAction: Button
    private lateinit var btnSecondaryAction: Button
    private lateinit var btnGenerateInvoice: Button
    private lateinit var btnRejectOrder: Button

    private var currentOrder: SellerOrder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
        orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
        initViews()
        observeViewModel()
        viewModel.loadOrderById(orderId)
    }

    private fun initViews() {
        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        tvPoNumber = findViewById(R.id.tvPoNumber)
        tvOrderTypeRow = findViewById(R.id.tvOrderTypeRow)
        layoutPoNumber = findViewById(R.id.layoutPoNumber)
        tvItemsTotal = findViewById(R.id.tvItemsTotal)
        tvBulkDiscount = findViewById(R.id.tvBulkDiscount)
        tvGst = findViewById(R.id.tvGst)
        tvDeliveryCharges = findViewById(R.id.tvDeliveryCharges)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvSellerEarnings = findViewById(R.id.tvSellerEarnings)
        tvTrackingNumber = findViewById(R.id.tvTrackingNumber)
        layoutTracking = findViewById(R.id.layoutTracking)
        rvItems = findViewById(R.id.rvItems)
        rvStatusHistory = findViewById(R.id.rvStatusHistory)
        progressBar = findViewById(R.id.progressBar)
        layoutActions = findViewById(R.id.layoutActions)
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction)
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction)
        btnGenerateInvoice = findViewById(R.id.btnGenerateInvoice)
        btnRejectOrder = findViewById(R.id.btnRejectOrder)
        rvItems.layoutManager = LinearLayoutManager(this)
        rvStatusHistory.layoutManager = LinearLayoutManager(this)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.selectedOrder.observe(this) { result ->
            when (result) {
                is Resource.Loading -> progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    result.data?.let { bindOrder(it) }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(rvItems, result.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.updateStatus.observe(this) { result ->
            when (result) {
                is Resource.Success -> Snackbar.make(rvItems, "Status updated", Snackbar.LENGTH_SHORT).show()
                is Resource.Error -> Snackbar.make(rvItems, result.message, Snackbar.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    private fun bindOrder(order: SellerOrder) {
        currentOrder = order
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        tvOrderId.text = "#${order.orderId.takeLast(8).uppercase()}"

        // Order Type Badge in title
        val orderTypes = order.items.map { it.orderType }.distinct()
        val hasPerSet = orderTypes.contains("per_set")
        val hasPerPiece = orderTypes.contains("per_piece") || orderTypes.isEmpty()
        val orderTypeLabel = when {
            hasPerSet && hasPerPiece -> "Mixed Order"
            hasPerSet -> "Per Set Order"
            else -> "Per Piece Order"
        }
        // Show order type as subtitle in status chip area
        tvOrderStatus.text = "${getStatusLabel(order.orderStatus)} · $orderTypeLabel"
        tvOrderTypeRow.text = orderTypeLabel
        tvOrderDate.text = fmt.format(Date(order.createdAt))
        tvOrderStatus.text = getStatusLabel(order.orderStatus)
        tvCustomerName.text = order.customerName
        tvCustomerPhone.text = "📞 ${order.customerPhone}"
        tvDeliveryAddress.text = "📍 ${order.deliveryAddress.ifEmpty { "Address not available" }}"
        tvPaymentMethod.text = order.paymentMethod
        tvPaymentStatus.text = order.paymentStatus

        // B2B Delivery Instructions
        val layoutDeliveryInstructions = findViewById<View>(R.id.layoutDeliveryInstructions)
        val tvDeliveryAddressType = findViewById<TextView>(R.id.tvDeliveryAddressType)
        val tvWeekendDelivery = findViewById<TextView>(R.id.tvWeekendDelivery)
        val tvDeliveryNote = findViewById<TextView>(R.id.tvDeliveryNote)

        val hasInstructions = order.deliveryAddressType.isNotEmpty() ||
                order.saturdayDelivery || order.sundayDelivery ||
                order.deliveryInstructions.isNotEmpty()

        if (hasInstructions) {
            layoutDeliveryInstructions.visibility = View.VISIBLE
            if (order.deliveryAddressType.isNotEmpty()) {
                tvDeliveryAddressType.visibility = View.VISIBLE
                tvDeliveryAddressType.text = "🏭 Address Type: ${order.deliveryAddressType}"
            }
            val weekendParts = mutableListOf<String>()
            if (order.saturdayDelivery) weekendParts.add("Saturday")
            if (order.sundayDelivery) weekendParts.add("Sunday")
            if (weekendParts.isNotEmpty()) {
                tvWeekendDelivery.visibility = View.VISIBLE
                tvWeekendDelivery.text = "📅 Weekend delivery: ${weekendParts.joinToString(", ")}"
            }
            if (order.deliveryInstructions.isNotEmpty()) {
                tvDeliveryNote.visibility = View.VISIBLE
                tvDeliveryNote.text = "📝 ${order.deliveryInstructions}"
            }
        } else {
            layoutDeliveryInstructions.visibility = View.GONE
        }

        if (order.poNumber.isNotEmpty()) {
            layoutPoNumber.visibility = View.VISIBLE
            tvPoNumber.text = order.poNumber
        } else {
            layoutPoNumber.visibility = View.GONE
        }

        tvItemsTotal.text = "₹${String.format("%.2f", order.itemsTotal)}"
        tvBulkDiscount.text = "-₹${String.format("%.2f", order.bulkDiscount)}"
        tvGst.text = "₹${String.format("%.2f", order.gstAmount)}"
        tvDeliveryCharges.text = "₹${String.format("%.2f", order.deliveryCharges)}"
        tvTotalAmount.text = "₹${String.format("%.2f", order.totalAmount)}"

        val earnings = if (order.sellerEarnings > 0) order.sellerEarnings
        else (order.itemsTotal - order.bulkDiscount) * 0.95
        tvSellerEarnings.text = "₹${String.format("%.2f", earnings)}"

        if (order.trackingNumber.isNotEmpty()) {
            layoutTracking.visibility = View.VISIBLE
            tvTrackingNumber.text = "${order.courierName} - ${order.trackingNumber}"
        } else {
            layoutTracking.visibility = View.GONE
        }

        // Order items
        rvItems.adapter = OrderItemsAdapter(order.items)

        // Status history
        rvStatusHistory.adapter = StatusHistoryAdapter(order.statusHistory)

        // Action buttons based on status
        setupActionButtons(order)

        // Invoice button
        btnGenerateInvoice.visibility = if (order.orderStatus == "delivered") View.VISIBLE else View.GONE
        btnGenerateInvoice.setOnClickListener {
            GstInvoiceGenerator.generateAndShare(this, order)
        }
    }

    private fun setupActionButtons(order: SellerOrder) {
        btnRejectOrder.visibility = View.GONE
        when (order.orderStatus) {
            "pending" -> {
                layoutActions.visibility = View.VISIBLE
                btnPrimaryAction.text = "Accept Order"
                btnPrimaryAction.setOnClickListener { showAcceptDialog(order) }
                btnRejectOrder.visibility = View.VISIBLE
                btnRejectOrder.setOnClickListener { showRejectDialog(order) }
                btnSecondaryAction.visibility = View.GONE
            }
            "confirmed" -> {
                layoutActions.visibility = View.VISIBLE
                btnPrimaryAction.text = "Mark as Packed"
                btnPrimaryAction.setOnClickListener {
                    viewModel.updateOrderStatus(order.orderId, "packed", "Order packed and ready to ship")
                }
                btnSecondaryAction.visibility = View.GONE
            }
            "packed" -> {
                layoutActions.visibility = View.VISIBLE
                btnPrimaryAction.text = "Mark as Shipped"
                btnPrimaryAction.setOnClickListener { showShipDialog(order) }
                btnSecondaryAction.visibility = View.GONE
            }
            "shipped" -> {
                layoutActions.visibility = View.VISIBLE
                btnPrimaryAction.text = "Mark as Delivered"
                btnPrimaryAction.setOnClickListener { showDeliverDialog(order) }
                btnSecondaryAction.visibility = View.GONE
            }
            else -> layoutActions.visibility = View.GONE
        }
    }

    private fun showAcceptDialog(order: SellerOrder) {
        AlertDialog.Builder(this)
            .setTitle("Accept Order")
            .setMessage("Confirm acceptance of this order?\n\nCustomer: ${order.customerName}\nAmount: ₹${String.format("%.0f", order.totalAmount)}")
            .setPositiveButton("Accept") { _, _ ->
                viewModel.updateOrderStatus(order.orderId, "confirmed", "Accepted by seller")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(order: SellerOrder) {
        val reasons = arrayOf("Out of stock", "Cannot fulfill", "Delivery not available", "Price mismatch", "Other")
        var selectedReason = reasons[0]
        AlertDialog.Builder(this)
            .setTitle("Reject Order")
            .setSingleChoiceItems(reasons, 0) { _, which -> selectedReason = reasons[which] }
            .setPositiveButton("Reject") { _, _ -> viewModel.rejectOrder(order.orderId, selectedReason) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showShipDialog(order: SellerOrder) {
        val view = layoutInflater.inflate(R.layout.dialog_ship_order, null)
        val etTracking = view.findViewById<EditText>(R.id.etTrackingNumber)
        val etCourier = view.findViewById<EditText>(R.id.etCourierName)
        AlertDialog.Builder(this)
            .setTitle("Ship Order")
            .setView(view)
            .setPositiveButton("Mark Shipped") { _, _ ->
                val tracking = etTracking.text.toString().trim()
                val courier = etCourier.text.toString().trim()
                viewModel.updateOrderStatus(order.orderId, "shipped", "Shipped by seller", tracking, courier)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeliverDialog(order: SellerOrder) {
        val msg = if (order.paymentMethod == "COD")
            "Mark as delivered? Payment of ₹${String.format("%.0f", order.totalAmount)} will be marked as PAID (COD)."
        else
            "Confirm delivery of this order?"
        AlertDialog.Builder(this)
            .setTitle("Confirm Delivery")
            .setMessage(msg)
            .setPositiveButton("Delivered") { _, _ ->
                viewModel.updateOrderStatus(order.orderId, "delivered", "Delivered - confirmed by seller")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getStatusLabel(status: String) = when (status) {
        "pending" -> "New Order"
        "confirmed" -> "Confirmed"
        "packed" -> "Packed"
        "shipped" -> "Shipped"
        "out_for_delivery" -> "Out for Delivery"
        "delivered" -> "Delivered"
        "cancelled" -> "Cancelled"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}
