package com.anga9.seller.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.repository.SupportRepository
import kotlinx.coroutines.launch

/**
 * Create Ticket Screen — Seller App
 *
 * Features:
 * - Category spinner (Orders / Returns / Payments / Account / Other)
 * - Subject + Description fields
 * - Optional related_order_id pre-fill from OrderDetailActivity
 * - File attachment picker (max 5 files)
 * - Submit → POST /api/support/tickets → upload attachments
 */
class CreateTicketActivity : BaseActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        private const val MAX_ATTACHMENTS = 5
    }

    private lateinit var repository: SupportRepository

    private lateinit var btnBack: ImageView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etSubject: EditText
    private lateinit var etDescription: EditText
    private lateinit var layoutRelatedOrder: LinearLayout
    private lateinit var tvRelatedOrderId: TextView
    private lateinit var btnClearOrder: ImageView
    private lateinit var btnAddAttachment: LinearLayout
    private lateinit var rvAttachments: RecyclerView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private val categories = listOf("Orders", "Returns", "Payments", "Account", "Other")
    private val categorySlugs = listOf("orders", "returns", "payments", "account", "other")
    private var selectedCategoryIndex = 0
    private var relatedOrderId: String? = null
    private val selectedAttachments = mutableListOf<Uri>()
    private lateinit var attachmentAdapter: AttachmentPreviewAdapter

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = MAX_ATTACHMENTS - selectedAttachments.size
            selectedAttachments.addAll(uris.take(remaining))
            attachmentAdapter.notifyDataSetChanged()
            updateAttachmentPreview()
            if (uris.size > remaining) {
                showToast("Maximum $MAX_ATTACHMENTS files allowed")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_ticket)

        repository = SupportRepository(this)
        relatedOrderId = intent.getStringExtra(EXTRA_ORDER_ID)

        initViews()
        setupCategorySpinner()
        setupRelatedOrder()
        setupAttachmentAdapter()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack          = findViewById(R.id.btnBack)
        spinnerCategory  = findViewById(R.id.spinnerCategory)
        etSubject        = findViewById(R.id.etSubject)
        etDescription    = findViewById(R.id.etDescription)
        layoutRelatedOrder = findViewById(R.id.layoutRelatedOrder)
        tvRelatedOrderId = findViewById(R.id.tvRelatedOrderId)
        btnClearOrder    = findViewById(R.id.btnClearOrder)
        btnAddAttachment = findViewById(R.id.btnAddAttachment)
        rvAttachments    = findViewById(R.id.rvAttachments)
        btnSubmit        = findViewById(R.id.btnSubmit)
        progressBar      = findViewById(R.id.progressBar)
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryIndex = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRelatedOrder() {
        if (!relatedOrderId.isNullOrBlank()) {
            layoutRelatedOrder.visibility = View.VISIBLE
            tvRelatedOrderId.text = "Order #${relatedOrderId}"
        }
    }

    private fun setupAttachmentAdapter() {
        attachmentAdapter = AttachmentPreviewAdapter(selectedAttachments) { position ->
            selectedAttachments.removeAt(position)
            attachmentAdapter.notifyItemRemoved(position)
            attachmentAdapter.notifyItemRangeChanged(position, selectedAttachments.size)
            updateAttachmentPreview()
        }
        rvAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAttachments.adapter = attachmentAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnClearOrder.setOnClickListener {
            relatedOrderId = null
            layoutRelatedOrder.visibility = View.GONE
        }

        btnAddAttachment.setOnClickListener {
            if (selectedAttachments.size >= MAX_ATTACHMENTS) {
                showToast("Maximum $MAX_ATTACHMENTS files allowed")
                return@setOnClickListener
            }
            filePickerLauncher.launch("*/*")
        }

        btnSubmit.setOnClickListener { submitTicket() }
    }

    private fun updateAttachmentPreview() {
        rvAttachments.visibility = if (selectedAttachments.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun submitTicket() {
        val subject     = etSubject.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category    = categorySlugs[selectedCategoryIndex]

        if (subject.isEmpty()) {
            etSubject.error = "Subject is required"
            etSubject.requestFocus()
            return
        }
        if (subject.length < 5) {
            etSubject.error = "Subject must be at least 5 characters"
            etSubject.requestFocus()
            return
        }
        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return
        }
        if (description.length < 10) {
            etDescription.error = "Please provide more detail (at least 10 characters)"
            etDescription.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val ticketResult = repository.createTicket(
                subject        = subject,
                category       = category,
                initialMessage = description,
                relatedOrderId = relatedOrderId
            )

            ticketResult.fold(
                onSuccess = { ticket ->
                    if (selectedAttachments.isNotEmpty()) {
                        repository.uploadAttachments(ticket.id, selectedAttachments)
                    }
                    setLoading(false)
                    showToast("Ticket ${ticket.ticketNumber} created successfully")

                    val intent = Intent(this@CreateTicketActivity, TicketDetailActivity::class.java).apply {
                        putExtra(TicketDetailActivity.EXTRA_TICKET_ID,     ticket.id)
                        putExtra(TicketDetailActivity.EXTRA_TICKET_NUMBER, ticket.ticketNumber)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    setLoading(false)
                    showToast("Failed to create ticket: ${error.message}")
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSubmit.isEnabled = !loading
        btnSubmit.text = if (loading) "Submitting..." else "Submit Ticket"
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
