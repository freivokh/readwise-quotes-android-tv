// app/src/main/java/com/readwisequotes/data/model/Quote.kt
package com.readwisequotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.readwisequotes.data.local.Converters

@Entity(tableName = "quotes")
@TypeConverters(Converters::class)
data class Quote(
    @PrimaryKey val id: Long,
    val text: String,
    val title: String?,
    val author: String?,
    val bookCover: String?,
    val tags: List<String>,
    val isFavorite: Boolean,
    val updatedAt: String,
    val sourceType: String?,
    val bookId: Long?
)
