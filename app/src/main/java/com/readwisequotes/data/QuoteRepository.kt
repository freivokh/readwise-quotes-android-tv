// app/src/main/java/com/readwisequotes/data/QuoteRepository.kt
package com.readwisequotes.data

import androidx.sqlite.db.SimpleSQLiteQuery
import com.readwisequotes.data.local.QuoteDao
import com.readwisequotes.data.model.Quote
import com.readwisequotes.data.model.TagGroup
import com.readwisequotes.data.remote.ReadwiseApi
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.settings.TagFilterMode
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepository @Inject constructor(
    private val quoteDao: QuoteDao,
    private val api: ReadwiseApi,
    private val settingsManager: SettingsManager
) {
    fun getQuotes(): Flow<List<Quote>> {
        return when (settingsManager.getQuoteFilter()) {
            QuoteFilter.ALL -> quoteDao.getAllQuotes()
            QuoteFilter.FAVORITES -> quoteDao.getFavoriteQuotes()
            QuoteFilter.BY_TAG -> getQuotesByTags()
            QuoteFilter.RECENT -> {
                val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
                quoteDao.getRecentQuotes(thirtyDaysAgo)
            }
        }
    }

    private fun getQuotesByTags(): Flow<List<Quote>> {
        val enabledGroups = settingsManager.getEnabledTagGroups()

        if (enabledGroups.isEmpty()) {
            // Fall back to all quotes if no groups enabled
            return quoteDao.getAllQuotes()
        }

        return buildGroupedTagQuery(enabledGroups)
    }

    private fun buildGroupedTagQuery(groups: List<TagGroup>): Flow<List<Quote>> {
        // Build per-group conditions, then OR them together
        val groupConditions = groups.map { group ->
            val tagConditions = group.tags.map { "tags LIKE '%$it%'" }
            val operator = if (group.matchMode == TagFilterMode.ANY) " OR " else " AND "
            "(${tagConditions.joinToString(operator)})"
        }

        val whereClause = groupConditions.joinToString(" OR ")
        val query = SimpleSQLiteQuery("SELECT * FROM quotes WHERE $whereClause ORDER BY RANDOM()")

        return quoteDao.getQuotesByTagsRaw(query)
    }

    suspend fun getAllTags(): List<String> {
        val rawTags = quoteDao.getAllTags()
        return rawTags.flatMap { tagJson ->
            // Parse the JSON array string and extract tag names
            tagJson.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    suspend fun sync(): SyncResult {
        val token = settingsManager.getApiToken()
        if (token.isEmpty()) {
            return SyncResult.Error("No API token configured")
        }

        return try {
            val updatedAfter = settingsManager.getLastSyncTime()
            val quotes = mutableListOf<Quote>()
            var cursor: String? = null

            do {
                val response = api.exportHighlights(
                    updatedAfter = updatedAfter,
                    pageCursor = cursor
                )

                if (!response.isSuccessful) {
                    return SyncResult.Error("API error: ${response.code()}")
                }

                val body = response.body() ?: break
                cursor = body.nextPageCursor

                body.results.forEach { book ->
                    book.highlights.forEach { highlight ->
                        quotes.add(
                            Quote(
                                id = highlight.id,
                                text = highlight.text,
                                note = highlight.note,
                                title = book.title,
                                author = book.author,
                                bookCover = book.coverImageUrl,
                                tags = highlight.tags.map { it.name },
                                isFavorite = highlight.isFavorite,
                                updatedAt = highlight.updated,
                                sourceType = book.sourceType,
                                bookId = book.userBookId
                            )
                        )
                    }
                }
            } while (cursor != null)

            if (quotes.isNotEmpty()) {
                quoteDao.insertAll(quotes)
            }

            settingsManager.setLastSyncTime(Instant.now().toString())
            SyncResult.Success(quotes.size)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun verifyToken(token: String): Boolean {
        return try {
            // Temporarily set token for verification
            val oldToken = settingsManager.getApiToken()
            settingsManager.setApiToken(token)
            val response = api.verifyToken()
            if (!response.isSuccessful) {
                settingsManager.setApiToken(oldToken)
            }
            response.isSuccessful || response.code() == 204
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getQuoteCount(): Int = quoteDao.getCount()
}

sealed class SyncResult {
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
