// app/src/main/java/com/readwisequotes/data/remote/ReadwiseApi.kt
package com.readwisequotes.data.remote

import com.readwisequotes.data.model.ReadwiseExportResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ReadwiseApi {
    @GET("api/v2/export/")
    suspend fun exportHighlights(
        @Query("updatedAfter") updatedAfter: String? = null,
        @Query("pageCursor") pageCursor: String? = null
    ): Response<ReadwiseExportResponse>

    @GET("api/v2/auth/")
    suspend fun verifyToken(): Response<Unit>

    companion object {
        const val BASE_URL = "https://readwise.io/"
    }
}
