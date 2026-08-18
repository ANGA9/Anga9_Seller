package com.anga9.seller.network.model

import com.google.gson.annotations.SerializedName

data class BulkImportResult(
    @SerializedName("success_count")
    val successCount: Int,
    @SerializedName("failed_count")
    val failedCount: Int,
    @SerializedName("errors")
    val errors: List<BulkImportError>
)

data class BulkImportError(
    @SerializedName("index")
    val index: Int?,
    @SerializedName("error")
    val error: String?
)
