package com.anga9.seller.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.anga9.seller.data.model.support.*
import com.anga9.seller.network.ApiClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for Help & Support operations - Seller App.
 *
 * Sellers can:
 * - Create tickets for platform issues (payments, KYC, product approval, etc.)
 * - View and reply to their own tickets
 * - Browse knowledge base articles
 *
 * JWT is automatically attached via AuthInterceptor in ApiClient.
 *
 * SECURITY: Filter messages where isInternal == true before rendering.
 */
class SupportRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)

    companion object {
        private const val TAG = "SellerSupportRepository"
    }

    // -------------------------------------------------------------------------
    // TICKETS
    // -------------------------------------------------------------------------

    suspend fun createTicket(
        subject: String,
        category: String,
        initialMessage: String,
        relatedOrderId: String? = null
    ): Result<SupportTicket> {
        return try {
            val response = apiService.createSupportTicket(
                CreateTicketRequest(
                    subject = subject,
                    category = category,
                    initialMessage = initialMessage,
                    relatedOrderId = relatedOrderId
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body.ticket)
                else Result.failure(Exception("Empty response from server"))
            } else {
                Result.failure(Exception("Failed to create ticket: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createTicket error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getTickets(page: Int = 1, limit: Int = 20): Result<TicketListResponse> {
        return try {
            val response = apiService.getSupportTickets(page = page, limit = limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: TicketListResponse())
            } else {
                Result.failure(Exception("Failed to load tickets: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTickets error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /**
     * NOTE: Filter messages where isInternal == true before rendering.
     * messages.filter { !it.isInternal }
     */
    suspend fun getTicketById(ticketId: String): Result<TicketDetail> {
        return try {
            val response = apiService.getSupportTicketById(ticketId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Ticket not found"))
            } else {
                val msg = if (response.code() == 404) "Ticket not found" else "Failed to load ticket: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTicketById error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun replyToTicket(ticketId: String, body: String): Result<MessageResponse> {
        return try {
            val response = apiService.replySupportTicket(ticketId, ReplyRequest(body = body))
            if (response.isSuccessful) {
                Result.success(response.body() ?: MessageResponse())
            } else {
                Result.failure(Exception("Failed to send reply: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "replyToTicket error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun updateTicketStatus(ticketId: String, status: String): Result<SupportTicket> {
        return try {
            val response = apiService.updateSupportTicketStatus(
                ticketId, TicketStatusRequest(status = status)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body.ticket)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateTicketStatus error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun rateTicket(ticketId: String, score: Int, comment: String? = null): Result<Boolean> {
        return try {
            val response = apiService.rateSupportTicket(
                ticketId, RateTicketRequest(score = score, comment = comment)
            )
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("Failed to submit rating: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "rateTicket error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    // -------------------------------------------------------------------------
    // ATTACHMENTS
    // -------------------------------------------------------------------------

    suspend fun uploadAttachments(
        ticketId: String,
        fileUris: List<Uri>
    ): List<Result<AttachmentUploadResponse>> {
        return fileUris.map { uri -> uploadSingleAttachment(ticketId, uri) }
    }

    private suspend fun uploadSingleAttachment(
        ticketId: String,
        uri: Uri
    ): Result<AttachmentUploadResponse> {
        return try {
            val filename = getFilenameFromUri(uri) ?: "attachment_${System.currentTimeMillis()}"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val initResponse = apiService.uploadSupportAttachment(
                ticketId, mapOf("filename" to filename, "mime_type" to mimeType)
            )

            if (!initResponse.isSuccessful) {
                return Result.failure(Exception("Failed to initiate upload: ${initResponse.code()}"))
            }

            val uploadData = initResponse.body()
                ?: return Result.failure(Exception("Empty upload response"))

            if (!uploadData.presignedUrl.isNullOrBlank()) {
                uploadToPresignedUrl(uri, uploadData.presignedUrl, mimeType)
            }

            Result.success(uploadData)
        } catch (e: Exception) {
            Log.e(TAG, "uploadSingleAttachment error", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        }
    }

    private fun uploadToPresignedUrl(uri: Uri, presignedUrl: String, mimeType: String) {
        try {
            val tempFile = copyUriToTempFile(uri) ?: return
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val request = Request.Builder().url(presignedUrl).put(requestBody).build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.w(TAG, "Presigned upload returned ${response.code}")
            }
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "uploadToPresignedUrl error", e)
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("support_attach_", null, context.cacheDir)
            FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "copyUriToTempFile error", e)
            null
        }
    }

    private fun getFilenameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) { null }
    }

    // -------------------------------------------------------------------------
    // KNOWLEDGE BASE
    // -------------------------------------------------------------------------

    suspend fun getArticles(
        query: String? = null,
        category: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<ArticleListResponse> {
        return try {
            val response = apiService.getSupportArticles(
                query = query, category = category, page = page, limit = limit
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: ArticleListResponse())
            } else {
                Result.failure(Exception("Failed to load articles: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getArticles error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getArticleBySlug(slug: String): Result<SupportArticle> {
        return try {
            val response = apiService.getSupportArticleBySlug(slug)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body.article)
                else Result.failure(Exception("Article not found"))
            } else {
                val msg = if (response.code() == 404) "Article not found" else "Failed to load article: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getArticleBySlug error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun rateArticle(slug: String, helpful: Boolean): Result<Boolean> {
        return try {
            val response = apiService.rateSupportArticle(slug, ArticleFeedbackRequest(helpful = helpful))
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("Failed to submit feedback: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "rateArticle error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
