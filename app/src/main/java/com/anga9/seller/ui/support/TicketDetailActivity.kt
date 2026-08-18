package com.anga9.seller.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.support.TicketMessage
import com.anga9.seller.data.repository.SupportRepository
import com.anga9.seller.network.TicketWebSocketClient
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

/**
 * Ticket Detail Screen - Seller App
 * Chat-like thread with 4 ViewHolder types.
 * SECURITY: isInternal == true messages filtered before rendering.
 */
class TicketDetailActivity : BaseActivity() {

    companion object {
        const val EXTRA_TICKET_ID     = "TICKET_ID"
        const val EXTRA_TICKET_NUMBER = "TICKET_NUMBER"

        private const val VIEW_TYPE_CUSTOMER   = 0
        private const val VIEW_TYPE_ADMIN      = 1
        private const val VIEW_TYPE_SYSTEM     = 2
        private const val VIEW_TYPE_ATTACHMENT = 3

        private const val PREFS_CSAT = "anga_seller_csat_prefs"
    }

    private lateinit var repository: SupportRepository
    private lateinit var markwon: Markwon

    private lateinit var btnBack: ImageView
    private lateinit var tvTicketNumber: TextView
    private lateinit var tvTicketStatus: TextView
    private lateinit var btnMenu: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutError: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button
    private lateinit var cardHeader: androidx.cardview.widget.CardView
    private lateinit var tvSubject: TextView
    private lateinit var layoutRelatedOrder: LinearLayout
    private lateinit var tvRelatedOrder: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var layoutReplyBox: LinearLayout
    private lateinit var rvReplyAttachments: RecyclerView
    private lateinit var btnAttach: ImageView
    private lateinit var etReply: EditText
    private lateinit var btnSend: ImageView
    private lateinit var layoutResolvedBanner: LinearLayout
    private lateinit var btnReopen: Button

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var replyAttachmentAdapter: AttachmentPreviewAdapter

    private var ticketId: String = ""
    private var currentStatus: String = "open"
    private val replyAttachments = mutableListOf<Uri>()

    // WebSocket live chat (Section 20 - BACKEND_API_REFERENCE.md)
    private var webSocketClient: TicketWebSocketClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = 5 - replyAttachments.size
            replyAttachments.addAll(uris.take(remaining))
            replyAttachmentAdapter.notifyDataSetChanged()
            updateReplyAttachmentVisibility()
            if (uris.size > remaining) showToast("Maximum 5 files allowed")
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        repository = SupportRepository(this)
        markwon    = Markwon.create(this)
        ticketId   = intent.getStringExtra(EXTRA_TICKET_ID) ?: ""
        val ticketNumber = intent.getStringExtra(EXTRA_TICKET_NUMBER) ?: ""

        initViews()
        if (ticketNumber.isNotEmpty()) tvTicketNumber.text = ticketNumber

        if (ticketId.isEmpty()) { showError("Invalid ticket ID"); return }

        setupMessageAdapter()
        setupReplyAttachmentAdapter()
        setupClickListeners()
        loadTicket()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient?.disconnect()
        webSocketClient = null
    }

    // ─── Init ─────────────────────────────────────────────────────────────

    private fun initViews() {
        btnBack              = findViewById(R.id.btnBack)
        tvTicketNumber       = findViewById(R.id.tvTicketNumber)
        tvTicketStatus       = findViewById(R.id.tvTicketStatus)
        btnMenu              = findViewById(R.id.btnMenu)
        progressBar          = findViewById(R.id.progressBar)
        layoutError          = findViewById(R.id.layoutError)
        tvError              = findViewById(R.id.tvError)
        btnRetry             = findViewById(R.id.btnRetry)
        cardHeader           = findViewById(R.id.cardHeader)
        tvSubject            = findViewById(R.id.tvSubject)
        layoutRelatedOrder   = findViewById(R.id.layoutRelatedOrder)
        tvRelatedOrder       = findViewById(R.id.tvRelatedOrder)
        rvMessages           = findViewById(R.id.rvMessages)
        layoutReplyBox       = findViewById(R.id.layoutReplyBox)
        rvReplyAttachments   = findViewById(R.id.rvReplyAttachments)
        btnAttach            = findViewById(R.id.btnAttach)
        etReply              = findViewById(R.id.etReply)
        btnSend              = findViewById(R.id.btnSend)
        layoutResolvedBanner = findViewById(R.id.layoutResolvedBanner)
        btnReopen            = findViewById(R.id.btnReopen)
    }

    private fun setupMessageAdapter() {
        messageAdapter = MessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = messageAdapter
    }

    private fun setupReplyAttachmentAdapter() {
        replyAttachmentAdapter = AttachmentPreviewAdapter(replyAttachments) { position ->
            replyAttachments.removeAt(position)
            replyAttachmentAdapter.notifyItemRemoved(position)
            replyAttachmentAdapter.notifyItemRangeChanged(position, replyAttachments.size)
            updateReplyAttachmentVisibility()
        }
        rvReplyAttachments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvReplyAttachments.adapter = replyAttachmentAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnRetry.setOnClickListener { loadTicket() }
        btnMenu.setOnClickListener { showOverflowMenu() }
        btnAttach.setOnClickListener {
            if (replyAttachments.size < 5) filePickerLauncher.launch("*/*")
            else showToast("Maximum 5 files allowed")
        }
        btnSend.setOnClickListener { sendReply() }
        btnReopen.setOnClickListener { updateStatus("reopened") }
    }

    // ─── Data Loading ─────────────────────────────────────────────────────

    private fun loadTicket() {
        showLoading(true)
        lifecycleScope.launch {
            val result = repository.getTicketById(ticketId)
            showLoading(false)
            result.fold(
                onSuccess = { detail ->
                    val ticket = detail.ticket
                    currentStatus = ticket.status
                    tvTicketNumber.text = ticket.ticketNumber
                    tvTicketStatus.text = ticket.status.replaceFirstChar { it.uppercase() }
                    tvSubject.text = ticket.subject
                    cardHeader.visibility = View.VISIBLE

                    if (!ticket.relatedOrderId.isNullOrBlank()) {
                        layoutRelatedOrder.visibility = View.VISIBLE
                        tvRelatedOrder.text = "Order #${ticket.relatedOrderId}"
                    }

                    // SECURITY: Filter isInternal == true messages
                    val visibleMessages = detail.messages.filter { !it.isInternal }
                    messageAdapter.submitList(visibleMessages)
                    if (visibleMessages.isNotEmpty()) rvMessages.scrollToPosition(visibleMessages.size - 1)
                    updateStatusUI(ticket.status)

                    // Connect WebSocket for real-time updates
                    if (ticket.status != "resolved" && ticket.status != "closed") {
                        connectWebSocket()
                    }
                },
                onFailure = { showError(it.message ?: "Failed to load ticket") }
            )
        }
    }

    private fun updateStatusUI(status: String) {
        val closed = status == "resolved" || status == "closed"
        layoutReplyBox.visibility       = if (closed) View.GONE else View.VISIBLE
        layoutResolvedBanner.visibility = if (closed) View.VISIBLE else View.GONE
    }

    // ─── Actions ──────────────────────────────────────────────────────────

    private fun sendReply() {
        val body = etReply.text.toString().trim()
        if (body.isEmpty() && replyAttachments.isEmpty()) { etReply.error = "Please type a message"; return }
        if (body.isEmpty()) { showToast("Please add a message with your attachments"); return }

        btnSend.isEnabled = false
        etReply.isEnabled = false

        lifecycleScope.launch {
            repository.replyToTicket(ticketId, body).fold(
                onSuccess = {
                    if (replyAttachments.isNotEmpty()) {
                        repository.uploadAttachments(ticketId, replyAttachments.toList())
                        replyAttachments.clear()
                        replyAttachmentAdapter.notifyDataSetChanged()
                        updateReplyAttachmentVisibility()
                    }
                    etReply.setText("")
                    btnSend.isEnabled = true
                    etReply.isEnabled = true
                    loadTicket()
                },
                onFailure = {
                    btnSend.isEnabled = true
                    etReply.isEnabled = true
                    showToast("Failed to send: ${it.message}")
                }
            )
        }
    }

    private fun showOverflowMenu() {
        val popup = PopupMenu(this, btnMenu)
        if (currentStatus == "resolved" || currentStatus == "closed") {
            popup.menu.add(0, 1, 0, "Reopen Ticket")
        } else {
            popup.menu.add(0, 2, 0, "Mark as Resolved")
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> updateStatus("reopened")
                2 -> AlertDialog.Builder(this)
                    .setTitle("Mark as Resolved")
                    .setMessage("Are you sure you want to mark this ticket as resolved?")
                    .setPositiveButton("Resolve") { _, _ -> updateStatus("resolved") }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            true
        }
        popup.show()
    }

    private fun updateStatus(status: String) {
        lifecycleScope.launch {
            repository.updateTicketStatus(ticketId, status).fold(
                onSuccess = { ticket ->
                    currentStatus = ticket.status
                    tvTicketStatus.text = ticket.status.replaceFirstChar { it.uppercase() }
                    updateStatusUI(ticket.status)
                    // Connect WebSocket for real-time updates
                    if (ticket.status != "resolved" && ticket.status != "closed") {
                        connectWebSocket()
                    }
                    val msg = if (status == "resolved") "Ticket marked as resolved" else "Ticket reopened"
                    showToast(msg)
                    if (status == "resolved") showCsatDialogIfNeeded()
                },
                onFailure = { showToast("Failed to update status") }
            )
        }
    }

    // ─── WebSocket Live Chat ───────────────────────────────────────────────

    private fun connectWebSocket() {
        webSocketClient?.disconnect()

        webSocketClient = TicketWebSocketClient(
            context  = this,
            ticketId = ticketId,
            listener = object : TicketWebSocketClient.TicketWebSocketListener {

                override fun onReady(ticketId: String) {
                    android.util.Log.d("SellerTicketDetail", "WebSocket ready: $ticketId")
                }

                override fun onNewMessage(message: TicketWebSocketClient.WebSocketMessage) {
                    mainHandler.post {
                        val currentList = messageAdapter.currentList.toMutableList()
                        if (currentList.none { it.id == message.id }) {
                            val newMsg = TicketMessage(
                                id          = message.id,
                                body        = message.body,
                                authorRole  = message.authorRole,
                                createdAt   = message.createdAt,
                                isInternal  = false,
                                attachments = null
                            )
                            currentList.add(newMsg)
                            messageAdapter.submitList(currentList)
                            rvMessages.scrollToPosition(currentList.size - 1)
                        }
                    }
                }

                override fun onError(error: String) {
                    android.util.Log.w("SellerTicketDetail", "WebSocket error: $error")
                }

                override fun onClosed() {
                    android.util.Log.d("SellerTicketDetail", "WebSocket closed")
                }
            }
        )
        webSocketClient?.connect()
    }

    // ─── CSAT Rating Dialog ────────────────────────────────────────────────

    private fun showCsatDialogIfNeeded() {
        val prefs = getSharedPreferences(PREFS_CSAT, MODE_PRIVATE)
        if (prefs.getBoolean("rated_$ticketId", false)) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_csat_rating, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        val stars = listOf<ImageView>(
            dialogView.findViewById(R.id.ivStar1), dialogView.findViewById(R.id.ivStar2),
            dialogView.findViewById(R.id.ivStar3), dialogView.findViewById(R.id.ivStar4),
            dialogView.findViewById(R.id.ivStar5)
        )
        val etComment: EditText = dialogView.findViewById(R.id.etCsatComment)
        val btnSubmit: Button   = dialogView.findViewById(R.id.btnSubmitRating)
        val btnSkip: TextView   = dialogView.findViewById(R.id.btnSkipRating)

        var selectedScore = 0
        fun updateStars(score: Int) {
            selectedScore = score
            stars.forEachIndexed { i, iv ->
                iv.setImageResource(if (i < score) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            }
        }
        stars.forEachIndexed { i, iv -> iv.setOnClickListener { updateStars(i + 1) } }

        btnSubmit.setOnClickListener {
            if (selectedScore == 0) { showToast("Please select a rating"); return@setOnClickListener }
            val comment = etComment.text.toString().trim().ifEmpty { null }
            lifecycleScope.launch {
                repository.rateTicket(ticketId, selectedScore, comment)
                prefs.edit().putBoolean("rated_$ticketId", true).apply()
                showToast("Thank you for your feedback!")
            }
            dialog.dismiss()
        }
        btnSkip.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun updateReplyAttachmentVisibility() {
        rvReplyAttachments.visibility = if (replyAttachments.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) { cardHeader.visibility = View.GONE; rvMessages.visibility = View.GONE; layoutError.visibility = View.GONE }
        else rvMessages.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE; cardHeader.visibility = View.GONE
        rvMessages.visibility = View.GONE; layoutError.visibility = View.VISIBLE
        tvError.text = message
    }

    // ─── Message Thread Adapter - 4 ViewHolder types ──────────────────────

    inner class MessageAdapter : ListAdapter<TicketMessage, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<TicketMessage>() {
        override fun areItemsTheSame(old: TicketMessage, new: TicketMessage) = old.id == new.id
        override fun areContentsTheSame(old: TicketMessage, new: TicketMessage) = old == new
    }) {
        override fun getItemViewType(position: Int): Int {
            val msg = getItem(position)
            return when {
                msg.authorRole == "system"       -> VIEW_TYPE_SYSTEM
                msg.authorRole == "admin"        -> VIEW_TYPE_ADMIN
                !msg.attachments.isNullOrEmpty() -> VIEW_TYPE_ATTACHMENT
                else                             -> VIEW_TYPE_CUSTOMER
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = android.view.LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_CUSTOMER   -> CustomerVH(inf.inflate(R.layout.item_message_customer, parent, false))
                VIEW_TYPE_ADMIN      -> AdminVH(inf.inflate(R.layout.item_message_admin, parent, false))
                VIEW_TYPE_SYSTEM     -> SystemVH(inf.inflate(R.layout.item_message_system, parent, false))
                VIEW_TYPE_ATTACHMENT -> AttachmentVH(inf.inflate(R.layout.item_message_attachment, parent, false))
                else                 -> CustomerVH(inf.inflate(R.layout.item_message_customer, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = getItem(position)
            when (holder) {
                is CustomerVH   -> holder.bind(msg)
                is AdminVH      -> holder.bind(msg)
                is SystemVH     -> holder.bind(msg)
                is AttachmentVH -> holder.bind(msg)
            }
        }

        inner class CustomerVH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvBody: TextView = view.findViewById(R.id.tvMessageBody)
            private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
            fun bind(msg: TicketMessage) { tvBody.text = msg.body; tvTime.text = formatTime(msg.createdAt) }
        }

        inner class AdminVH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvBody: TextView = view.findViewById(R.id.tvMessageBody)
            private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
            fun bind(msg: TicketMessage) { markwon.setMarkdown(tvBody, msg.body); tvTime.text = formatTime(msg.createdAt) }
        }

        inner class SystemVH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvBody: TextView = view.findViewById(R.id.tvMessageBody)
            fun bind(msg: TicketMessage) { tvBody.text = msg.body }
        }

        inner class AttachmentVH(view: View) : RecyclerView.ViewHolder(view) {
            private val ivThumb: ImageView    = view.findViewById(R.id.ivAttachmentThumb)
            private val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
            private val tvFilename: TextView  = view.findViewById(R.id.tvFilename)
            private val tvFileSize: TextView  = view.findViewById(R.id.tvFileSize)
            private val ivDownload: ImageView = view.findViewById(R.id.ivDownload)
            private val tvTime: TextView      = view.findViewById(R.id.tvAttachmentTime)

            fun bind(msg: TicketMessage) {
                val attachment = msg.attachments?.firstOrNull() ?: return
                tvTime.text     = formatTime(msg.createdAt)
                tvFilename.text = attachment.filename
                tvFileSize.text = formatSize(attachment.sizeBytes)

                if (attachment.mimeType.startsWith("image/")) {
                    ivThumb.visibility = View.VISIBLE; ivFileIcon.visibility = View.GONE
                    ivThumb.load(attachment.url) { crossfade(true) }
                } else {
                    ivThumb.visibility = View.GONE; ivFileIcon.visibility = View.VISIBLE
                    ivFileIcon.setImageResource(R.drawable.ic_attachment)
                }

                val openFile = View.OnClickListener {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url))) }
                    catch (e: Exception) { showToast("Cannot open file") }
                }
                ivDownload.setOnClickListener(openFile)
                itemView.setOnClickListener(openFile)
            }

            private fun formatSize(bytes: Long): String = when {
                bytes < 1024        -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else                -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }

        private fun formatTime(dateStr: String): String {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(date)
            } catch (_: Exception) { dateStr }
        }
    }
}