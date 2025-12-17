// app/src/main/java/com/readwisequotes/data/local/QuoteDao.kt
package com.readwisequotes.data.local

import androidx.room.*
import com.readwisequotes.data.model.Quote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY RANDOM()")
    fun getAllQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY RANDOM()")
    fun getFavoriteQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE tags LIKE '%' || :tag || '%' ORDER BY RANDOM()")
    fun getQuotesByTag(tag: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE updatedAt >= :since ORDER BY RANDOM()")
    fun getRecentQuotes(since: String): Flow<List<Quote>>

    @Query("SELECT DISTINCT tags FROM quotes")
    suspend fun getAllTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<Quote>)

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun getCount(): Int
}
