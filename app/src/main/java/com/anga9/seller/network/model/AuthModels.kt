package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

data class AuthVerifyRequest(
    @SerializedName("accessToken") val accessToken: String
)

data class AuthVerifyResponse(
    @SerializedName("user") val user: UserProfileResponse,
    @SerializedName("isNew") val isNew: Boolean = false,
    @SerializedName("sellerProfile") val sellerProfile: SellerProfileResponse? = null
)

data class UserProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("auth_uid") val authUid: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("kyc_status") val kycStatus: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ApiErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("statusCode") val statusCode: Int
)