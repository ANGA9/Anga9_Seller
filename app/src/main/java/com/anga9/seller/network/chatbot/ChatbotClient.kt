package com.anga9.seller.network.chatbot

import android.content.Context
import android.util.Log
import com.anga9.seller.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ChatbotClient — OkHttp-based SSE client for the ANGA9 chatbot-service.
 * Surface: "seller"
 */
class ChatbotClient(private val context: Context) {

    companion object {
        private const val TAG = "ChatbotClient"
        private const val SURFACE = "seller"
        private const val BASE_URL = "https://api.anga9.com"
        private const val CHATBOT_BASE = "$BASE_URL/api/chatbot"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private fun authHeader(): String? {
        val supabaseToken = com.anga9.seller.network.SupabaseClient.auth.currentAccessTokenOrNull()
        val token = supabaseToken ?: TokenManager.getToken(context)
        return if (token != null) "Bearer $token" else null
    }

    suspend fun createSession(): CreateSessionResponse {
        val body = """{"surface":"$SURFACE"}""".toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/sessions")
            .post(body)
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response body from createSession")
        if (!response.isSuccessful) {
            throw Exception("createSession failed: HTTP ${response.code} — $responseBody")
        }
        return json.decodeFromString(CreateSessionResponse.serializer(), responseBody)
    }

    suspend fun getSession(sessionId: String): SessionHistoryResponse {
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/sessions/$sessionId")
            .get()
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response body from getSession")
        if (!response.isSuccessful) {
            throw Exception("getSession failed: HTTP ${response.code} — $responseBody")
        }
        return json.decodeFromString(SessionHistoryResponse.serializer(), responseBody)
    }

    fun sendMessage(sessionId: String, message: String): Flow<StreamFrame> = flow {
        val escapedMessage = message.replace("\"", "\\\"")
        val body = """{"message":"$escapedMessage"}""".toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/sessions/$sessionId/messages")
            .post(body)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            emit(StreamFrame.Error("HTTP ${response.code}: $errorBody"))
            return@flow
        }
        val source = response.body?.source()
            ?: run { emit(StreamFrame.Error("Empty SSE stream")); return@flow }
        emitSseFrames(source)
    }.flowOn(Dispatchers.IO)

    fun confirmAction(sessionId: String, toolName: String, args: JsonObject): Flow<StreamFrame> = flow {
        val argsJson = args.toString()
        val body = """{"args":$argsJson}""".toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/sessions/$sessionId/actions/$toolName")
            .post(body)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            emit(StreamFrame.Error("HTTP ${response.code}: $errorBody"))
            return@flow
        }
        val source = response.body?.source()
            ?: run { emit(StreamFrame.Error("Empty SSE stream")); return@flow }
        emitSseFrames(source)
    }.flowOn(Dispatchers.IO)

    suspend fun submitFeedback(assistantMessageId: String, rating: Int) {
        val body = """{"rating":$rating}""".toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/messages/$assistantMessageId/feedback")
            .post(body)
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "submitFeedback failed: HTTP ${response.code}")
        }
        response.body?.close()
    }

    suspend fun escalate(sessionId: String): EscalateResponse {
        val body = "{}".toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("$CHATBOT_BASE/sessions/$sessionId/escalate")
            .post(body)
        authHeader()?.let { requestBuilder.header("Authorization", it) }
        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response body from escalate")
        if (!response.isSuccessful) {
            throw Exception("escalate failed: HTTP ${response.code} — $responseBody")
        }
        return json.decodeFromString(EscalateResponse.serializer(), responseBody)
    }

    suspend fun checkHealth(): HealthResponse {
        val request = Request.Builder()
            .url("$CHATBOT_BASE/health")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response body from health check")
        if (!response.isSuccessful) {
            throw Exception("health check failed: HTTP ${response.code}")
        }
        return json.decodeFromString(HealthResponse.serializer(), responseBody)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<StreamFrame>.emitSseFrames(
        source: okio.BufferedSource
    ) {
        var currentEvent = ""
        var currentData = ""
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith(":") -> { /* keepalive */ }
                    line.startsWith("event:") -> {
                        currentEvent = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        currentData = line.removePrefix("data:").trim()
                    }
                    line.isEmpty() -> {
                        if (currentData.isNotEmpty()) {
                            val frame = parseFrame(currentEvent, currentData)
                            emit(frame)
                            if (frame is StreamFrame.Final || frame is StreamFrame.Error) break
                        }
                        currentEvent = ""
                        currentData = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSE stream error: ${e.message}", e)
            emit(StreamFrame.Error(e.message ?: "Stream read error"))
        } finally {
            source.close()
        }
    }

    private fun parseFrame(event: String, data: String): StreamFrame {
        return try {
            val jsonObj = json.parseToJsonElement(data).jsonObject
            when (event) {
                "text" -> {
                    val delta = jsonObj["delta"]?.jsonPrimitive?.content ?: ""
                    StreamFrame.Text(delta)
                }
                "tool_call" -> {
                    val name = jsonObj["name"]?.jsonPrimitive?.content ?: ""
                    val args = jsonObj["args"]?.jsonObject ?: JsonObject(emptyMap())
                    val requiresConfirmation = jsonObj["requiresConfirmation"]?.jsonPrimitive?.boolean ?: false
                    StreamFrame.ToolCall(name, args, requiresConfirmation)
                }
                "tool_result" -> {
                    val name = jsonObj["name"]?.jsonPrimitive?.content ?: ""
                    val status = jsonObj["status"]?.jsonPrimitive?.content ?: "ok"
                    val output = jsonObj["output"]
                    val error = jsonObj["error"]?.jsonPrimitive?.content
                    StreamFrame.ToolResult(name, status, output, error)
                }
                "final" -> {
                    val assistantMessageId = jsonObj["assistantMessageId"]?.jsonPrimitive?.content ?: ""
                    val text = jsonObj["text"]?.jsonPrimitive?.content ?: ""
                    val tokensIn = jsonObj["tokensIn"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val tokensOut = jsonObj["tokensOut"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val provider = jsonObj["provider"]?.jsonPrimitive?.content ?: ""
                    StreamFrame.Final(assistantMessageId, text, tokensIn, tokensOut, provider)
                }
                "error" -> {
                    val errorMsg = jsonObj["error"]?.jsonPrimitive?.content ?: "Unknown error"
                    StreamFrame.Error(errorMsg)
                }
                else -> {
                    Log.d(TAG, "Unknown SSE event: '$event'")
                    StreamFrame.Keepalive
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SSE frame: ${e.message}")
            StreamFrame.Error("Failed to parse server response")
        }
    }
}
