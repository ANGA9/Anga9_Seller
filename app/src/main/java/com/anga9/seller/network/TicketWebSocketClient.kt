package com.anga9.seller.network

import android.content.Context
import android.util.Log
import com.anga9.seller.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * TicketWebSocketClient — real-time live chat for support tickets.
 *
 * Backend spec (Section 20, BACKEND_API_REFERENCE.md):
 *   URL: wss://api.anga9.com/ws/support/tickets/:ticketId?token=<supabase_access_token>
 *
 * Message types received:
 *   { "type": "ready",   "ticketId": "uuid" }
 *   { "type": "message", "id": "...", "body": "...", "authorRole": "admin|seller|system", "createdAt": "ISO" }
 *   { "type": "pong",    "ts": 1234 }
 *
 * Send keepalive: { "type": "ping" }
 * NOTE: WebSocket is READ-ONLY. Message sending uses REST API.
 */
class TicketWebSocketClient(
    private val context: Context,
    private val ticketId: String,
    private val listener: TicketWebSocketListener
) {

    companion object {
        private const val TAG = "SellerTicketWebSocket"
        private const val WS_BASE = "wss://api.anga9.com/ws/support/tickets"
        private const val PING_INTERVAL_MS = 30_000L
    }

    interface TicketWebSocketListener {
        fun onReady(ticketId: String)
        fun onNewMessage(message: WebSocketMessage)
        fun onError(error: String)
        fun onClosed()
    }

    data class WebSocketMessage(
        val id: String,
        val body: String,
        val authorRole: String,
        val createdAt: String,
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false

    fun connect() {
        val supabaseToken = com.anga9.seller.network.SupabaseClient.auth.currentAccessTokenOrNull()
        val token = supabaseToken ?: TokenManager.getToken(context)
        if (token == null) {
            Log.w(TAG, "No auth token — cannot connect to ticket WebSocket")
            listener.onError("Authentication required for live chat")
            return
        }

        val url = "$WS_BASE/$ticketId?token=$token"
        val request = Request.Builder().url(url).build()

        Log.d(TAG, "Connecting to ticket WebSocket: ticketId=$ticketId")

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "WebSocket connected for ticket: $ticketId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndDispatch(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                listener.onError(t.message ?: "WebSocket connection failed")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener.onClosed()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Activity closed")
        webSocket = null
        isConnected = false
    }

    val connected: Boolean get() = isConnected

    private fun parseAndDispatch(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type", "")) {
                "ready" -> listener.onReady(json.optString("ticketId", ticketId))
                "message" -> {
                    val msg = WebSocketMessage(
                        id         = json.optString("id", ""),
                        body       = json.optString("body", ""),
                        authorRole = json.optString("authorRole", "system"),
                        createdAt  = json.optString("createdAt", ""),
                    )
                    listener.onNewMessage(msg)
                }
                "pong" -> Log.v(TAG, "pong ts=${json.optLong("ts")}")
                else -> Log.d(TAG, "Unknown frame: ${json.optString("type")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WebSocket message: $text", e)
        }
    }
}
