package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

/** POST /api/notifications/device-tokens */
data class DeviceTokenRequest(
    @SerializedName("token")    val token: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("app_type") val appType: String
)

/** DELETE /api/notifications/device-tokens */
data class UnregisterTokenRequest(
    @SerializedName("token") val token: String
)

// GET /api/notifications
data class SellerNotification(
    @SerializedName("id")       val id: String,
    @SerializedName("type")     val type: String,
    @SerializedName("title")    val title: String,
    @SerializedName("body")     val body: String,
    @SerializedName("data")     val data: Map<String, String>? = null,
    @SerializedName("channel")  val channel: String = "in_app",
    @SerializedName("read")     val read: Boolean = false,
    @SerializedName("sent_at")  val sentAt: String = ""
)

data class NotificationListResponse(
    @SerializedName("data")  val data: List<SellerNotification> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page")  val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20
)

// GET /api/notifications/unread-count
data class UnreadCountResponse(
    @SerializedName("count") val count: Int = 0
)

// GET/PATCH /api/notifications/preferences
data class NotificationPreferencesResponse(
    @SerializedName("email_enabled")     val emailEnabled: Boolean = true,
    @SerializedName("sms_enabled")       val smsEnabled: Boolean = false,
    @SerializedName("push_enabled")      val pushEnabled: Boolean = true,
    @SerializedName("quiet_hours_start") val quietHoursStart: String? = null,
    @SerializedName("quiet_hours_end")   val quietHoursEnd: String? = null,
    @SerializedName("preferences")       val preferences: Map<String, Boolean>? = null
)

data class UpdatePreferencesRequest(
    @SerializedName("email_enabled")     val emailEnabled: Boolean? = null,
    @SerializedName("sms_enabled")       val smsEnabled: Boolean? = null,
    @SerializedName("push_enabled")      val pushEnabled: Boolean? = null,
    @SerializedName("quiet_hours_start") val quietHoursStart: String? = null,
    @SerializedName("quiet_hours_end")   val quietHoursEnd: String? = null,
    @SerializedName("preferences")       val preferences: Map<String, Boolean>? = null
)