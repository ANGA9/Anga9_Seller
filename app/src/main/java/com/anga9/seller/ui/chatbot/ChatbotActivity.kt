package com.anga9.seller.ui.chatbot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.network.chatbot.StreamFrame
import com.anga9.seller.ui.support.TicketDetailActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SOURCE = "SOURCE"
    }

    private val viewModel: ChatbotViewModel by viewModels()

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnEscalate: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvOfflineBanner: TextView
    private lateinit var tvError: TextView
    private lateinit var quickRepliesContainer: LinearLayout
    private lateinit var confirmationCard: CardView
    private lateinit var tvConfirmTitle: TextView
    private lateinit var tvConfirmArgs: TextView
    private lateinit var btnConfirm: TextView
    private lateinit var btnCancelConfirm: TextView

    private lateinit var adapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)
        initViews()
        setupRecyclerView()
        setupClickListeners()
        observeState()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnEscalate = findViewById(R.id.btnEscalate)
        progressBar = findViewById(R.id.progressBar)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        tvError = findViewById(R.id.tvError)
        quickRepliesContainer = findViewById(R.id.quickRepliesContainer)
        confirmationCard = findViewById(R.id.confirmationCard)
        tvConfirmTitle = findViewById(R.id.tvConfirmTitle)
        tvConfirmArgs = findViewById(R.id.tvConfirmArgs)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancelConfirm = findViewById(R.id.btnCancelConfirm)
    }

    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(
            onThumbsUp = { msgId -> viewModel.submitFeedback(msgId, 1) },
            onThumbsDown = { msgId -> viewModel.submitFeedback(msgId, -1) },
        )
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) { viewModel.sendMessage(text); etMessage.setText("") }
        }
        btnEscalate.setOnClickListener { handleEscalate() }
        setupQuickReplyChip(R.id.chipWhereIsMyOrder, "Where is my order?")
        setupQuickReplyChip(R.id.chipStartReturn, "Start a return")
        setupQuickReplyChip(R.id.chipTalkToHuman, "Talk to a human") { handleEscalate() }
        btnConfirm.setOnClickListener {
            // Show working state immediately on Confirm tap
            btnConfirm.visibility = android.view.View.GONE
            btnCancelConfirm.visibility = android.view.View.GONE
            viewModel.confirmAction()
        }
        btnCancelConfirm.setOnClickListener { viewModel.cancelAction() }
    }

    private fun setupQuickReplyChip(viewId: Int, text: String, customAction: (() -> Unit)? = null) {
        val chip = findViewById<TextView>(viewId) ?: return
        chip.setOnClickListener { if (customAction != null) customAction() else viewModel.sendMessage(text) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                tvOfflineBanner.visibility = if (state.isOffline) View.VISIBLE else View.GONE
                adapter.submitList(state.messages.toList()) {
                    if (state.messages.isNotEmpty()) rvMessages.scrollToPosition(state.messages.size - 1)
                }
                etMessage.isEnabled = state.inputEnabled && !state.isSending
                btnSend.isEnabled = state.inputEnabled && !state.isSending
                btnSend.alpha = if (state.inputEnabled && !state.isSending) 1f else 0.4f
                quickRepliesContainer.visibility = if (state.showQuickReplies && !state.isLoading) View.VISIBLE else View.GONE
                if (state.error != null) { tvError.text = state.error; tvError.visibility = View.VISIBLE }
                else tvError.visibility = View.GONE
                if (state.pendingToolCall != null) showConfirmationCard(state.pendingToolCall)
                else confirmationCard.visibility = View.GONE
            }
        }
    }

    private fun showConfirmationCard(toolCall: StreamFrame.ToolCall) {
        val friendlyTitle = when (toolCall.name) {
            "cancel_order" -> "Cancel Order"
            "reorder" -> "Reorder Items"
            "request_payout" -> "Request Payout"
            else -> toolCall.name.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
        val argsSummary = try {
            when (toolCall.name) {
                "cancel_order" -> "Order #${toolCall.args["order_id"]?.toString()?.trim('"') ?: ""}"
                "request_payout" -> "Amount: ₹${toolCall.args["amount"]?.toString()?.trim('"') ?: ""}"
                else -> toolCall.args.toString()
            }
        } catch (e: Exception) { "" }
        tvConfirmTitle.text = friendlyTitle
        tvConfirmArgs.text = argsSummary
        // Reset to default state (buttons visible)
        btnConfirm.visibility = android.view.View.VISIBLE
        btnCancelConfirm.visibility = android.view.View.VISIBLE
        confirmationCard.visibility = View.VISIBLE
    }

    private fun handleEscalate() {
        viewModel.escalate(
            onSuccess = { ticketId ->
                val intent = Intent(this, TicketDetailActivity::class.java).apply {
                    putExtra(TicketDetailActivity.EXTRA_TICKET_ID, ticketId)
                    putExtra(EXTRA_SOURCE, "chatbot")
                }
                startActivity(intent)
                finish()
            },
            onAuthRequired = {
                Toast.makeText(this, "Please sign in to talk to a human agent.", Toast.LENGTH_LONG).show()
            },
            onError = { error ->
                Snackbar.make(rvMessages, "Could not connect to support: $error", Snackbar.LENGTH_LONG).show()
            }
        )
    }
}

