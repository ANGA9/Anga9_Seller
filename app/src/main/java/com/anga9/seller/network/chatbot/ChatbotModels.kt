package com.anga9.seller.network.chatbot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ─────────────────────────────────────────────────────────────────────────────
// Session & Message Models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ChatSession(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_role") val userRole: String,
    val surface: String,                          // "seller"
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("escalated_ticket_id") val escalatedTicketId: String? = null,
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,                             // "user" | "assistant" | "tool" | "system"
    val content: String,
    @SerialName("tool_name") val toolName: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Request Models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CreateSessionRequest(
    val surface: String = "seller",
)

@Serializable
data class SendMessageRequest(
    val message: String,
)

@Serializable
data class FeedbackRequest(
    val rating: Int,
)

@Serializable
data class ConfirmActionRequest(
    val args: JsonObject,
)

// ─────────────────────────────────────────────────────────────────────────────
// Response Models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CreateSessionResponse(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_role") val userRole: String,
    val surface: String,
    @SerialName("started_at") val startedAt: String,
)

@Serializable
data class SessionHistoryResponse(
    val session: ChatSession,
    val messages: List<ChatMessage>,
)

@Serializable
data class EscalateResponse(
    val ok: Boolean,
    @SerialName("ticket_id") val ticketId: String,
)

@Serializable
data class HealthResponse(
    val status: String,
    val llm: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// SSE Stream Frame Types
// ─────────────────────────────────────────────────────────────────────────────

sealed class StreamFrame {
    data class Text(val delta: String) : StreamFrame()
    data class ToolCall(
        val name: String,
        val args: JsonObject,
        val requiresConfirmation: Boolean,
    ) : StreamFrame()
    data class ToolResult(
        val name: String,
        val status: String,
        val output: JsonElement? = null,
        val error: String? = null,
    ) : StreamFrame()
    data class Final(
        val assistantMessageId: String,
        val text: String,
        val tokensIn: Int,
        val tokensOut: Int,
        val provider: String,
    ) : StreamFrame()
    data class Error(val error: String) : StreamFrame()
    object Keepalive : StreamFrame()
}

// ─────────────────────────────────────────────────────────────────────────────
// Tool Hint Copy (Seller surface — includes seller-specific tools)
// ─────────────────────────────────────────────────────────────────────────────

fun toolHintCopy(toolName: String): String = when (toolName) {
    "get_recent_orders"      -> "Looking up your recent orders..."
    "get_order"              -> "Fetching order details..."
    "get_shipment_tracking"  -> "Checking shipment status..."
    "get_return_window"      -> "Checking return eligibility..."
    "get_my_addresses"       -> "Loading your addresses..."
    "get_recent_payouts"     -> "Pulling recent payouts..."
    "get_listing_status"     -> "Checking your listings..."
    "get_kyc_status"         -> "Checking KYC status..."
    "get_inventory_alerts"   -> "Scanning inventory..."
    "search_articles"        -> "Searching the help center..."
    "mark_notifications_read"-> "Marking notifications read..."
    else                     -> "Working on it..."
}

// ─────────────────────────────────────────────────────────────────────────────
// Token Cap Detection
// ─────────────────────────────────────────────────────────────────────────────

private const val TOKEN_CAP_SESSION =
    "You have reached the maximum token budget for this session. Please start a new session."

private const val TOKEN_CAP_DAILY =
    "You have reached your daily AI usage limit. Please try again tomorrow."

fun isTokenCapReached(text: String): Boolean =
    text == TOKEN_CAP_SESSION || text == TOKEN_CAP_DAILY
