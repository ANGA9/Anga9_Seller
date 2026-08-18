package com.anga9.seller.data.model

import com.google.gson.annotations.SerializedName

// =============================================================================
// Legal Pages - Data Models (Seller App)
// Identical structure to Customer App - same JSON files from CDN
// kind values: "p", "h3", "ul", "h2"
// =============================================================================

data class PrivacyBlock(
    @SerializedName("kind")  val kind: String,
    @SerializedName("text")  val text: String? = null,
    @SerializedName("items") val items: List<String>? = null
)

data class LegalMeta(
    @SerializedName("title")       val title: String = "",
    @SerializedName("lastUpdated") val lastUpdated: String? = null,
    @SerializedName("version")     val version: String? = null,
    @SerializedName("headings")    val headings: Map<String, String>? = null
)

data class PrivacyLangContent(
    @SerializedName("meta")    val meta: LegalMeta = LegalMeta(),
    @SerializedName("content") val content: Map<String, List<PrivacyBlock>> = emptyMap()
)

data class TermsSection(
    @SerializedName("heading")    val heading: String = "",
    @SerializedName("paragraphs") val paragraphs: List<String> = emptyList(),
    @SerializedName("listIntro")  val listIntro: String? = null,
    @SerializedName("list")       val list: List<String> = emptyList(),
    @SerializedName("blocks")     val blocks: List<PrivacyBlock>? = null
)

data class TermsLangContent(
    @SerializedName("meta")     val meta: LegalMeta = LegalMeta(),
    @SerializedName("sections") val sections: Map<String, TermsSection> = emptyMap()
)

data class LegalResult<T>(
    val data: T?,
    val source: LegalDataSource,
    val error: String? = null
)

enum class LegalDataSource {
    CDN,
    CACHE,
    ASSETS,
    NONE
}
