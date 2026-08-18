package com.anga9.seller.ui.chatbot

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anga9.seller.network.chatbot.ChatbotClient
import com.anga9.seller.network.chatbot.StreamFrame
import com.anga9.seller.network.chatbot.isTokenCapReached
import com.anga9.seller.network.chatbot.toolHintCopy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val isStreaming: Boolean = false,
    val inlineHint: String? = null,
    val assistantMessageId: String? = null,
    val feedbackGiven: Int? = null,
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

data class ChatbotUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val inputEnabled: Boolean = true,
    val showQuickReplies: Boolean = true,
    val isOffline: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null,
    val pendingToolCall: StreamFrame.ToolCall? = null,
)

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "chatbot_seller_prefs"
        private const val KEY_SESSION_ID = "session_id"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L
        private const val KEY_LAST_ACTIVITY = "last_activity_at"
    }

    private val client = ChatbotClient(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    private var streamingMessageId: String = ""
    private var streamingText: StringBuilder = StringBuilder()

    init { checkHealthAndInit() }

    private fun checkHealthAndInit() {
        viewModelScope.launch {
            try {
                val health = client.checkHealth()
                if (health.llm == "unconfigured") {
                    _uiState.value = _uiState.value.copy(isOffline = true, inputEnabled = false,
                        error = "Anga is offline right now. Please try again later.")
                    return@launch
                }
            } catch (e: Exception) { }
            initSession()
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val savedSessionId = prefs.getString(KEY_SESSION_ID, null)
            val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
            val isExpired = System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS
            if (savedSessionId != null && !isExpired) {
                try {
                    val history = client.getSession(savedSessionId)
                    val messages = history.messages
                        .filter { it.role == "user" || it.role == "assistant" }
                        .map { msg -> ChatUiMessage(id = msg.id,
                            role = if (msg.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
                            text = msg.content) }
                    _uiState.value = _uiState.value.copy(sessionId = savedSessionId,
                        messages = messages, isLoading = false, showQuickReplies = messages.isEmpty())
                    return@launch
                } catch (e: Exception) { }
            }
            createNewSession()
        }
    }

    private suspend fun createNewSession() {
        try {
            val session = client.createSession()
            prefs.edit().putString(KEY_SESSION_ID, session.id)
                .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
            _uiState.value = _uiState.value.copy(sessionId = session.id, messages = emptyList(),
                isLoading = false, showQuickReplies = true, inputEnabled = true, error = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false,
                error = "Could not connect to Anga. Please check your connection.")
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _uiState.value.sessionId ?: return
        if (!_uiState.value.inputEnabled || text.isBlank()) return
        val userMsgId = "user_${System.currentTimeMillis()}"
        val userMessage = ChatUiMessage(id = userMsgId, role = MessageRole.USER, text = text.trim())
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + userMessage,
            isSending = true, showQuickReplies = false, error = null)
        streamingMessageId = "assistant_streaming_${System.currentTimeMillis()}"
        streamingText = StringBuilder()
        val streamingBubble = ChatUiMessage(id = streamingMessageId, role = MessageRole.ASSISTANT,
            text = "", isStreaming = true)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + streamingBubble)
        viewModelScope.launch {
            client.sendMessage(sessionId, text.trim())
                .catch { e -> handleStreamError(e.message ?: "Stream error") }
                .collect { frame -> handleFrame(frame, sessionId) }
        }
        prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    private fun handleFrame(frame: StreamFrame, sessionId: String) {
        when (frame) {
            is StreamFrame.Text -> { streamingText.append(frame.delta); updateStreamingBubble(text = streamingText.toString()) }
            is StreamFrame.ToolCall -> {
                if (frame.requiresConfirmation) _uiState.value = _uiState.value.copy(pendingToolCall = frame, isSending = false)
                else updateStreamingBubble(hint = toolHintCopy(frame.name))
            }
            is StreamFrame.ToolResult -> updateStreamingBubble(hint = null)
            is StreamFrame.Final -> {
                val tokenCapHit = isTokenCapReached(frame.text)
                val finalMessage = ChatUiMessage(id = frame.assistantMessageId.ifEmpty { streamingMessageId },
                    role = MessageRole.ASSISTANT, text = frame.text, isStreaming = false,
                    assistantMessageId = frame.assistantMessageId)
                val updatedMessages = _uiState.value.messages.filter { it.id != streamingMessageId }.plus(finalMessage)
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isSending = false,
                    inputEnabled = !tokenCapHit, error = if (tokenCapHit) frame.text else null)
            }
            is StreamFrame.Error -> {
                if (frame.error.contains("409") || frame.error.contains("Session is closed")) {
                    viewModelScope.launch { removeStreamingBubble(); createNewSession() }
                } else handleStreamError(frame.error)
            }
            is StreamFrame.Keepalive -> { }
        }
    }

    fun confirmAction() {
        val sessionId = _uiState.value.sessionId ?: return
        val pendingTool = _uiState.value.pendingToolCall ?: return
        _uiState.value = _uiState.value.copy(pendingToolCall = null, isSending = true)
        streamingMessageId = "assistant_confirm_${System.currentTimeMillis()}"
        streamingText = StringBuilder()
        val streamingBubble = ChatUiMessage(id = streamingMessageId, role = MessageRole.ASSISTANT, text = "", isStreaming = true)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + streamingBubble)
        viewModelScope.launch {
            client.confirmAction(sessionId, pendingTool.name, pendingTool.args)
                .catch { e -> handleStreamError(e.message ?: "Action error") }
                .collect { frame -> handleFrame(frame, sessionId) }
        }
    }

    fun cancelAction() { _uiState.value = _uiState.value.copy(pendingToolCall = null) }

    fun submitFeedback(assistantMessageId: String, rating: Int) {
        viewModelScope.launch {
            try {
                client.submitFeedback(assistantMessageId, rating)
                val updatedMessages = _uiState.value.messages.map { msg ->
                    if (msg.assistantMessageId == assistantMessageId) msg.copy(feedbackGiven = rating) else msg
                }
                _uiState.value = _uiState.value.copy(messages = updatedMessages)
            } catch (e: Exception) { }
        }
    }

    fun escalate(onSuccess: (ticketId: String) -> Unit, onAuthRequired: () -> Unit, onError: (String) -> Unit) {
        val sessionId = _uiState.value.sessionId ?: run { onError("No active session"); return }
        viewModelScope.launch {
            try {
                val response = client.escalate(sessionId)
                _uiState.value = _uiState.value.copy(inputEnabled = false)
                onSuccess(response.ticketId)
            } catch (e: Exception) {
                when { e.message?.contains("401") == true -> onAuthRequired()
                    else -> onError(e.message ?: "Escalation failed") }
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            prefs.edit().remove(KEY_SESSION_ID).apply()
            createNewSession()
        }
    }

    private fun updateStreamingBubble(text: String = streamingText.toString(),
        hint: String? = _uiState.value.messages.find { it.id == streamingMessageId }?.inlineHint) {
        val updatedMessages = _uiState.value.messages.map { msg ->
            if (msg.id == streamingMessageId) msg.copy(text = text, inlineHint = hint) else msg
        }
        _uiState.value = _uiState.value.copy(messages = updatedMessages)
    }

    private fun removeStreamingBubble() {
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages.filter { it.id != streamingMessageId })
    }

    private fun handleStreamError(error: String) {
        removeStreamingBubble()
        _uiState.value = _uiState.value.copy(isSending = false, error = error)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
