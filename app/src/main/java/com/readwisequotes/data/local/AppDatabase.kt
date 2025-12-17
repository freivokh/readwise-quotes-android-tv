// app/src/main/java/com/readwisequotes/data/local/AppDatabase.kt
package com.readwisequotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.readwisequotes.data.model.Quote

@Database(entities = [Quote::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
}
