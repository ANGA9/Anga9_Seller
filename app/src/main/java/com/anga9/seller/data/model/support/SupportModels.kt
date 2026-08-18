package com.anga9.seller.data.model.support

import com.google.gson.annotations.SerializedName

// Support Ticket Models - Seller App
// Same backend endpoints - role inferred from JWT claim

data class SupportTicket(
    @SerializedName("id")               val id: String = "",
    @SerializedName("ticket_number")    val ticketNumber: String = "",
    @SerializedName("subject")          val subject: String = "",
    @SerializedName("status")           val status: String = "open",
    @SerializedName("priority")         val priority: String = "medium",
    @SerializedName("category")         val category: String = "",
    @SerializedName("related_order_id") val relatedOrderId: String? = null,
    @SerializedName("source")           val source: String? = null,
    @SerializedName("created_at")       val createdAt: String = "",
    @SerializedName("updated_at")       val updatedAt: String = ""
)

data class SupportAttachment(
    @SerializedName("id")         val id: String = "",
    @SerializedName("message_id") val messageId: String = "",
    @SerializedName("url")        val url: String = "",
    @SerializedName("filename")   val filename: String = "",
    @SerializedName("mime_type")  val mimeType: String = "",
    @SerializedName("size_bytes") val sizeBytes: Long = 0L
)

/**
 * SECURITY: isInternal == true messages MUST be filtered before rendering.
 * authorRole: "customer" | "admin" | "system"
 */
data class TicketMessage(
    @SerializedName("id")          val id: String = "",
    @SerializedName("author_role") val authorRole: String = "customer",
    @SerializedName("body")        val body: String = "",
    @SerializedName("created_at")  val createdAt: String = "",
    @SerializedName("is_internal") val isInternal: Boolean = false,
    @SerializedName("attachments") val attachments: List<SupportAttachment>? = null
)

data class TicketEvent(
    @SerializedName("id")         val id: String = "",
    @SerializedName("type")       val type: String = "",
    @SerializedName("body")       val body: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class TicketDetail(
    @SerializedName("ticket")   val ticket: SupportTicket = SupportTicket(),
    @SerializedName("messages") val messages: List<TicketMessage> = emptyList(),
    @SerializedName("events")   val events: List<TicketEvent>? = null
)

data class SupportArticle(
    @SerializedName("slug")     val slug: String = "",
    @SerializedName("title")    val title: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("body_md")  val bodyMd: String? = null,
    @SerializedName("views")    val views: Int = 0
)

data class CreateTicketRequest(
    @SerializedName("subject")          val subject: String,
    @SerializedName("category")         val category: String,
    @SerializedName("initial_message")  val initialMessage: String,
    @SerializedName("priority")         val priority: String? = null,
    @SerializedName("attachments")      val attachments: List<String>? = null,
    @SerializedName("related_order_id") val relatedOrderId: String? = null
)

data class ReplyRequest(
    @SerializedName("body") val body: String
)

data class TicketStatusRequest(
    @SerializedName("status") val status: String
)

data class RateTicketRequest(
    @SerializedName("score")   val score: Int,
    @SerializedName("comment") val comment: String? = null
)

data class ArticleFeedbackRequest(
    @SerializedName("helpful") val helpful: Boolean
)

data class TicketListResponse(
    @SerializedName("tickets")  val tickets: List<SupportTicket> = emptyList(),
    @SerializedName("total")    val total: Int = 0,
    @SerializedName("page")     val page: Int = 1,
    @SerializedName("limit")    val limit: Int = 20,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class TicketResponse(
    @SerializedName("ticket") val ticket: SupportTicket = SupportTicket()
)

data class ArticleListResponse(
    @SerializedName("articles") val articles: List<SupportArticle> = emptyList(),
    @SerializedName("total")    val total: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class ArticleResponse(
    @SerializedName("article") val article: SupportArticle = SupportArticle()
)

data class AttachmentUploadResponse(
    @SerializedName("id")            val id: String = "",
    @SerializedName("url")           val url: String = "",
    @SerializedName("filename")      val filename: String = "",
    @SerializedName("mime_type")     val mimeType: String = "",
    @SerializedName("size_bytes")    val sizeBytes: Long = 0L,
    @SerializedName("presigned_url") val presignedUrl: String? = null
)

data class MessageResponse(
    @SerializedName("id")         val id: String = "",
    @SerializedName("body")       val body: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class RateTicketResponse(
    @SerializedName("success") val success: Boolean = true
)

data class ArticleFeedbackResponse(
    @SerializedName("success") val success: Boolean = true
)
