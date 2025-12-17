// app/src/main/java/com/readwisequotes/data/model/ReadwiseResponse.kt
package com.readwisequotes.data.model

import com.google.gson.annotations.SerializedName

data class ReadwiseExportResponse(
    val count: Int,
    @SerializedName("nextPageCursor") val nextPageCursor: String?,
    val results: List<BookResult>
)

data class BookResult(
    @SerializedName("user_book_id") val userBookId: Long,
    val title: String?,
    val author: String?,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    @SerializedName("source_type") val sourceType: String?,
    val highlights: List<HighlightResult>
)

data class HighlightResult(
    val id: Long,
    val text: String,
    @SerializedName("is_favorite") val isFavorite: Boolean,
    val tags: List<TagResult>,
    @SerializedName("updated") val updated: String
)

data class TagResult(
    val name: String
)
