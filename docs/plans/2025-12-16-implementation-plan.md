# Readwise Quotes Android TV - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an Android TV screensaver app that displays Readwise highlights with ambient animations.

**Architecture:** Dual-mode app (MainActivity + DreamService) sharing a common QuoteDisplayView. Room database for local caching, Retrofit for Readwise API, SharedPreferences for settings.

**Tech Stack:** Kotlin, Android TV SDK (Leanback), Room, Retrofit, OkHttp, Hilt, Coroutines

---

## Task 1: Create Android TV Project Structure

**Files:**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/app_banner.xml`

**Step 1: Create root build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

**Step 2: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ReadwiseQuotes"
include(":app")
```

**Step 3: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**Step 4: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.readwisequotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.readwisequotes"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android TV
    implementation("androidx.leanback:leanback:1.0.0")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Security (encrypted prefs)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

**Step 5: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <uses-feature
        android:name="android.software.leanback"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />

    <application
        android:name=".ReadwiseQuotesApp"
        android:allowBackup="true"
        android:banner="@drawable/app_banner"
        android:icon="@drawable/app_banner"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.ReadwiseQuotes">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.SettingsActivity"
            android:exported="false"
            android:theme="@style/Theme.ReadwiseQuotes" />

        <service
            android:name=".QuoteDreamService"
            android:exported="true"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_DREAM_SERVICE">
            <intent-filter>
                <action android:name="android.service.dreams.DreamService" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <meta-data
                android:name="android.service.dream"
                android:resource="@xml/dream_info" />
        </service>

    </application>

</manifest>
```

**Step 6: Create resource files**

```xml
<!-- app/src/main/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Readwise Quotes</string>
    <string name="settings_title">Settings</string>
    <string name="api_token_label">Readwise API Token</string>
    <string name="api_token_hint">Paste your token from readwise.io/access_token</string>
    <string name="sync_now">Sync Now</string>
    <string name="last_sync">Last sync: %s</string>
    <string name="never_synced">Never synced</string>
    <string name="syncing">Syncing...</string>
    <string name="quote_filter_label">Show Quotes</string>
    <string name="filter_all">All Highlights</string>
    <string name="filter_favorites">Favorites Only</string>
    <string name="filter_tags">By Tag</string>
    <string name="filter_recent">Recent (30 days)</string>
    <string name="visual_style_label">Visual Style</string>
    <string name="style_ambient">Ambient</string>
    <string name="style_minimal">Minimal (OLED)</string>
    <string name="duration_label">Quote Duration</string>
    <string name="duration_format">%d seconds</string>
    <string name="sync_interval_label">Auto-sync Every</string>
    <string name="no_quotes">No quotes found. Check your filters or sync.</string>
    <string name="setup_required">Enter your Readwise API token to get started.</string>
</resources>
```

```xml
<!-- app/src/main/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="background_dark">#000000</color>
    <color name="text_primary">#FFFFFF</color>
    <color name="text_secondary">#B0B0B0</color>
    <color name="gradient_start">#1a1a2e</color>
    <color name="gradient_mid">#16213e</color>
    <color name="gradient_end">#0f3460</color>
    <color name="accent">#e94560</color>
</resources>
```

```xml
<!-- app/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ReadwiseQuotes" parent="@style/Theme.Leanback">
        <item name="android:colorBackground">@color/background_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
    </style>
</resources>
```

**Step 7: Create app banner drawable**

```xml
<!-- app/src/main/res/drawable/app_banner.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:startColor="@color/gradient_start"
        android:endColor="@color/gradient_end"
        android:angle="45" />
    <size
        android:width="320dp"
        android:height="180dp" />
</shape>
```

**Step 8: Create dream service config**

```xml
<!-- app/src/main/res/xml/dream_info.xml -->
<?xml version="1.0" encoding="utf-8"?>
<dream xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.readwisequotes.ui.SettingsActivity" />
```

**Step 9: Create Gradle wrapper**

Run from project root:
```bash
cd /Users/freivokh/Code/Claude/readwise-quotes-android-tv
gradle wrapper --gradle-version 8.2
```

**Step 10: Commit**

```bash
git add -A
git commit -m "feat: initialize Android TV project structure"
```

---

## Task 2: Create Data Models

**Files:**
- Create: `app/src/main/java/com/readwisequotes/data/model/Quote.kt`
- Create: `app/src/main/java/com/readwisequotes/data/model/ReadwiseResponse.kt`

**Step 1: Create Quote entity**

```kotlin
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
```

**Step 2: Create Readwise API response models**

```kotlin
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
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add data models for quotes and Readwise API"
```

---

## Task 3: Create Room Database

**Files:**
- Create: `app/src/main/java/com/readwisequotes/data/local/QuoteDao.kt`
- Create: `app/src/main/java/com/readwisequotes/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/readwisequotes/data/local/Converters.kt`

**Step 1: Create type converters**

```kotlin
// app/src/main/java/com/readwisequotes/data/local/Converters.kt
package com.readwisequotes.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
```

**Step 2: Create QuoteDao**

```kotlin
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
```

**Step 3: Create AppDatabase**

```kotlin
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
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add Room database with QuoteDao"
```

---

## Task 4: Create Readwise API Service

**Files:**
- Create: `app/src/main/java/com/readwisequotes/data/remote/ReadwiseApi.kt`
- Create: `app/src/main/java/com/readwisequotes/data/remote/AuthInterceptor.kt`

**Step 1: Create API interface**

```kotlin
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
```

**Step 2: Create auth interceptor**

```kotlin
// app/src/main/java/com/readwisequotes/data/remote/AuthInterceptor.kt
package com.readwisequotes.data.remote

import com.readwisequotes.settings.SettingsManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val settingsManager: SettingsManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = settingsManager.getApiToken()
        val request = chain.request().newBuilder()
            .apply {
                if (token.isNotEmpty()) {
                    addHeader("Authorization", "Token $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add Readwise API service with auth interceptor"
```

---

## Task 5: Create Settings Manager

**Files:**
- Create: `app/src/main/java/com/readwisequotes/settings/SettingsManager.kt`
- Create: `app/src/main/java/com/readwisequotes/settings/QuoteFilter.kt`
- Create: `app/src/main/java/com/readwisequotes/settings/VisualStyle.kt`

**Step 1: Create enums**

```kotlin
// app/src/main/java/com/readwisequotes/settings/QuoteFilter.kt
package com.readwisequotes.settings

enum class QuoteFilter {
    ALL,
    FAVORITES,
    BY_TAG,
    RECENT
}
```

```kotlin
// app/src/main/java/com/readwisequotes/settings/VisualStyle.kt
package com.readwisequotes.settings

enum class VisualStyle {
    AMBIENT,
    MINIMAL
}
```

**Step 2: Create SettingsManager**

```kotlin
// app/src/main/java/com/readwisequotes/settings/SettingsManager.kt
package com.readwisequotes.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // API Token (encrypted)
    fun getApiToken(): String = securePrefs.getString(KEY_API_TOKEN, "") ?: ""
    fun setApiToken(token: String) = securePrefs.edit().putString(KEY_API_TOKEN, token).apply()

    // Last sync timestamp
    fun getLastSyncTime(): String? = prefs.getString(KEY_LAST_SYNC, null)
    fun setLastSyncTime(time: String) = prefs.edit().putString(KEY_LAST_SYNC, time).apply()

    // Quote filter
    fun getQuoteFilter(): QuoteFilter {
        val value = prefs.getString(KEY_QUOTE_FILTER, QuoteFilter.ALL.name) ?: QuoteFilter.ALL.name
        return QuoteFilter.valueOf(value)
    }
    fun setQuoteFilter(filter: QuoteFilter) = prefs.edit().putString(KEY_QUOTE_FILTER, filter.name).apply()

    // Selected tags (for BY_TAG filter)
    fun getSelectedTags(): Set<String> = prefs.getStringSet(KEY_SELECTED_TAGS, emptySet()) ?: emptySet()
    fun setSelectedTags(tags: Set<String>) = prefs.edit().putStringSet(KEY_SELECTED_TAGS, tags).apply()

    // Visual style
    fun getVisualStyle(): VisualStyle {
        val value = prefs.getString(KEY_VISUAL_STYLE, VisualStyle.AMBIENT.name) ?: VisualStyle.AMBIENT.name
        return VisualStyle.valueOf(value)
    }
    fun setVisualStyle(style: VisualStyle) = prefs.edit().putString(KEY_VISUAL_STYLE, style.name).apply()

    // Quote duration in seconds
    fun getQuoteDuration(): Int = prefs.getInt(KEY_QUOTE_DURATION, 20)
    fun setQuoteDuration(seconds: Int) = prefs.edit().putInt(KEY_QUOTE_DURATION, seconds).apply()

    // Sync interval in hours
    fun getSyncIntervalHours(): Int = prefs.getInt(KEY_SYNC_INTERVAL, 24)
    fun setSyncIntervalHours(hours: Int) = prefs.edit().putInt(KEY_SYNC_INTERVAL, hours).apply()

    fun isSetupComplete(): Boolean = getApiToken().isNotEmpty()

    fun shouldSync(): Boolean {
        val lastSync = getLastSyncTime() ?: return true
        val lastSyncMillis = try {
            java.time.Instant.parse(lastSync).toEpochMilli()
        } catch (e: Exception) {
            return true
        }
        val intervalMillis = getSyncIntervalHours() * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastSyncMillis > intervalMillis
    }

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_QUOTE_FILTER = "quote_filter"
        private const val KEY_SELECTED_TAGS = "selected_tags"
        private const val KEY_VISUAL_STYLE = "visual_style"
        private const val KEY_QUOTE_DURATION = "quote_duration"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add SettingsManager with encrypted token storage"
```

---

## Task 6: Create Quote Repository

**Files:**
- Create: `app/src/main/java/com/readwisequotes/data/QuoteRepository.kt`

**Step 1: Create repository**

```kotlin
// app/src/main/java/com/readwisequotes/data/QuoteRepository.kt
package com.readwisequotes.data

import com.readwisequotes.data.local.QuoteDao
import com.readwisequotes.data.model.Quote
import com.readwisequotes.data.remote.ReadwiseApi
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
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
            QuoteFilter.BY_TAG -> {
                val tags = settingsManager.getSelectedTags()
                if (tags.isNotEmpty()) {
                    quoteDao.getQuotesByTag(tags.first())
                } else {
                    quoteDao.getAllQuotes()
                }
            }
            QuoteFilter.RECENT -> {
                val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
                quoteDao.getRecentQuotes(thirtyDaysAgo)
            }
        }
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
```

**Step 2: Commit**

```bash
git add -A
git commit -m "feat: add QuoteRepository with sync logic"
```

---

## Task 7: Create Hilt Dependency Injection Module

**Files:**
- Create: `app/src/main/java/com/readwisequotes/di/AppModule.kt`
- Create: `app/src/main/java/com/readwisequotes/ReadwiseQuotesApp.kt`

**Step 1: Create Hilt module**

```kotlin
// app/src/main/java/com/readwisequotes/di/AppModule.kt
package com.readwisequotes.di

import android.content.Context
import androidx.room.Room
import com.readwisequotes.data.local.AppDatabase
import com.readwisequotes.data.local.QuoteDao
import com.readwisequotes.data.remote.AuthInterceptor
import com.readwisequotes.data.remote.ReadwiseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "readwise_quotes.db"
        ).build()
    }

    @Provides
    fun provideQuoteDao(database: AppDatabase): QuoteDao {
        return database.quoteDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideReadwiseApi(okHttpClient: OkHttpClient): ReadwiseApi {
        return Retrofit.Builder()
            .baseUrl(ReadwiseApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReadwiseApi::class.java)
    }
}
```

**Step 2: Create Application class**

```kotlin
// app/src/main/java/com/readwisequotes/ReadwiseQuotesApp.kt
package com.readwisequotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReadwiseQuotesApp : Application()
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add Hilt dependency injection setup"
```

---

## Task 8: Create Quote Display View

**Files:**
- Create: `app/src/main/java/com/readwisequotes/ui/QuoteDisplayView.kt`
- Create: `app/src/main/java/com/readwisequotes/ui/GradientBackgroundView.kt`

**Step 1: Create animated gradient background**

```kotlin
// app/src/main/java/com/readwisequotes/ui/GradientBackgroundView.kt
package com.readwisequotes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.readwisequotes.R

class GradientBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var gradientOffset = 0f
    private var animator: ValueAnimator? = null

    private val colorStart = ContextCompat.getColor(context, R.color.gradient_start)
    private val colorMid = ContextCompat.getColor(context, R.color.gradient_mid)
    private val colorEnd = ContextCompat.getColor(context, R.color.gradient_end)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        if (width == 0 || height == 0) return

        val diagonal = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
        val offsetX = (gradientOffset * diagonal * 0.5f)

        paint.shader = LinearGradient(
            -diagonal / 2 + offsetX,
            -diagonal / 2,
            diagonal / 2 + offsetX,
            diagonal / 2,
            intArrayOf(colorStart, colorMid, colorEnd, colorStart),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.REPEAT
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30000L // 30 seconds for full cycle
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                gradientOffset = animation.animatedValue as Float
                updateGradient()
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
```

**Step 2: Create quote display view**

```kotlin
// app/src/main/java/com/readwisequotes/ui/QuoteDisplayView.kt
package com.readwisequotes.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.readwisequotes.R
import com.readwisequotes.data.model.Quote
import com.readwisequotes.settings.VisualStyle

class QuoteDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val gradientBackground: GradientBackgroundView
    private val quoteContainer: LinearLayout
    private val quoteText: TextView
    private val authorText: TextView
    private val sourceText: TextView

    private var currentQuotes: List<Quote> = emptyList()
    private var currentIndex = 0
    private var quoteDurationMs = 20000L
    private var isRunning = false
    private var visualStyle: VisualStyle = VisualStyle.AMBIENT

    private val displayRunnable = Runnable { showNextQuote() }

    init {
        // Create gradient background
        gradientBackground = GradientBackgroundView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(gradientBackground)

        // Create quote container
        quoteContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                val padding = dpToPx(80)
                setPadding(padding, padding, padding, padding)
            }
        }
        addView(quoteContainer)

        // Quote text
        quoteText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(32)
            }
        }
        quoteContainer.addView(quoteText)

        // Author text
        authorText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
        quoteContainer.addView(authorText)

        // Source text
        sourceText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            alpha = 0.7f
        }
        quoteContainer.addView(sourceText)
    }

    fun setVisualStyle(style: VisualStyle) {
        visualStyle = style
        when (style) {
            VisualStyle.AMBIENT -> {
                gradientBackground.visibility = View.VISIBLE
                gradientBackground.startAnimation()
            }
            VisualStyle.MINIMAL -> {
                gradientBackground.visibility = View.GONE
                gradientBackground.stopAnimation()
                setBackgroundColor(Color.BLACK)
            }
        }
    }

    fun setQuoteDuration(durationSeconds: Int) {
        quoteDurationMs = durationSeconds * 1000L
    }

    fun setQuotes(quotes: List<Quote>) {
        currentQuotes = quotes.shuffled()
        currentIndex = 0
        if (isRunning && quotes.isNotEmpty()) {
            displayCurrentQuote()
        }
    }

    fun start() {
        isRunning = true
        if (visualStyle == VisualStyle.AMBIENT) {
            gradientBackground.startAnimation()
        }
        if (currentQuotes.isNotEmpty()) {
            displayCurrentQuote()
        }
    }

    fun stop() {
        isRunning = false
        gradientBackground.stopAnimation()
        handler?.removeCallbacks(displayRunnable)
    }

    private fun displayCurrentQuote() {
        if (currentQuotes.isEmpty()) return

        val quote = currentQuotes[currentIndex]

        // Adjust text size based on quote length
        val textSize = when {
            quote.text.length > 500 -> 20f
            quote.text.length > 300 -> 24f
            quote.text.length > 150 -> 28f
            else -> 32f
        }
        quoteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        // Fade out current content
        fadeOut {
            // Update content
            quoteText.text = "\"${quote.text}\""
            authorText.text = quote.author?.let { "— $it" } ?: ""
            sourceText.text = quote.title ?: ""

            authorText.visibility = if (quote.author.isNullOrEmpty()) View.GONE else View.VISIBLE
            sourceText.visibility = if (quote.title.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Fade in new content
            fadeIn {
                // Schedule next quote
                handler?.postDelayed(displayRunnable, quoteDurationMs)
            }
        }
    }

    private fun showNextQuote() {
        if (!isRunning) return
        currentIndex = (currentIndex + 1) % currentQuotes.size
        displayCurrentQuote()
    }

    private fun fadeOut(onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 1f, 0f).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun fadeIn(onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 0f, 1f).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add QuoteDisplayView with animated gradient background"
```

---

## Task 9: Create Settings Activity

**Files:**
- Create: `app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt`
- Create: `app/src/main/res/layout/activity_settings.xml`

**Step 1: Create settings layout**

```xml
<!-- app/src/main/res/layout/activity_settings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark"
    android:padding="48dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_title"
            android:textColor="@color/text_primary"
            android:textSize="32sp"
            android:textStyle="bold"
            android:layout_marginBottom="32dp" />

        <!-- Account Section -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="ACCOUNT"
            android:textColor="@color/accent"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="16dp" />

        <EditText
            android:id="@+id/apiTokenInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/api_token_hint"
            android:textColor="@color/text_primary"
            android:textColorHint="@color/text_secondary"
            android:backgroundTint="@color/text_secondary"
            android:inputType="textPassword"
            android:layout_marginBottom="16dp"
            android:focusable="true"
            android:focusableInTouchMode="true" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="32dp">

            <Button
                android:id="@+id/syncButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/sync_now"
                android:layout_marginEnd="16dp" />

            <TextView
                android:id="@+id/syncStatus"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_secondary"
                android:gravity="center_vertical"
                android:layout_gravity="center_vertical" />
        </LinearLayout>

        <!-- Quote Filter Section -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="QUOTE FILTERS"
            android:textColor="@color/accent"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/quote_filter_label"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <Spinner
            android:id="@+id/filterSpinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@android:drawable/btn_dropdown"
            android:layout_marginBottom="32dp"
            android:focusable="true" />

        <!-- Display Section -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="DISPLAY"
            android:textColor="@color/accent"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/visual_style_label"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <Spinner
            android:id="@+id/styleSpinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@android:drawable/btn_dropdown"
            android:layout_marginBottom="16dp"
            android:focusable="true" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/duration_label"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <SeekBar
            android:id="@+id/durationSeekBar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:min="10"
            android:max="60"
            android:progress="20"
            android:layout_marginBottom="8dp"
            android:focusable="true" />

        <TextView
            android:id="@+id/durationValue"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_secondary"
            android:layout_marginBottom="32dp" />

        <!-- Sync Section -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="SYNC"
            android:textColor="@color/accent"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/sync_interval_label"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <Spinner
            android:id="@+id/syncIntervalSpinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@android:drawable/btn_dropdown"
            android:focusable="true" />

    </LinearLayout>
</ScrollView>
```

**Step 2: Create SettingsActivity**

```kotlin
// app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt
package com.readwisequotes.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.settings.VisualStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var apiTokenInput: EditText
    private lateinit var syncButton: Button
    private lateinit var syncStatus: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var styleSpinner: Spinner
    private lateinit var durationSeekBar: SeekBar
    private lateinit var durationValue: TextView
    private lateinit var syncIntervalSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupListeners()
        loadCurrentSettings()
    }

    private fun bindViews() {
        apiTokenInput = findViewById(R.id.apiTokenInput)
        syncButton = findViewById(R.id.syncButton)
        syncStatus = findViewById(R.id.syncStatus)
        filterSpinner = findViewById(R.id.filterSpinner)
        styleSpinner = findViewById(R.id.styleSpinner)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        durationValue = findViewById(R.id.durationValue)
        syncIntervalSpinner = findViewById(R.id.syncIntervalSpinner)
    }

    private fun setupListeners() {
        // API Token - save on focus lost
        apiTokenInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val token = apiTokenInput.text.toString()
                if (token.isNotEmpty()) {
                    verifyAndSaveToken(token)
                }
            }
        }

        // Sync button
        syncButton.setOnClickListener {
            performSync()
        }

        // Filter spinner
        val filterAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.filter_all),
                getString(R.string.filter_favorites),
                getString(R.string.filter_tags),
                getString(R.string.filter_recent)
            )
        )
        filterSpinner.adapter = filterAdapter
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = QuoteFilter.entries[position]
                settingsManager.setQuoteFilter(filter)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Style spinner
        val styleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.style_ambient), getString(R.string.style_minimal))
        )
        styleSpinner.adapter = styleAdapter
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val style = VisualStyle.entries[position]
                settingsManager.setVisualStyle(style)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Duration seek bar
        durationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                durationValue.text = getString(R.string.duration_format, progress)
                if (fromUser) {
                    settingsManager.setQuoteDuration(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Sync interval spinner
        val intervalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("1 hour", "6 hours", "24 hours", "Manual only")
        )
        syncIntervalSpinner.adapter = intervalAdapter
        syncIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val hours = when (position) {
                    0 -> 1
                    1 -> 6
                    2 -> 24
                    else -> Int.MAX_VALUE
                }
                settingsManager.setSyncIntervalHours(hours)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCurrentSettings() {
        // Token (show masked)
        val token = settingsManager.getApiToken()
        if (token.isNotEmpty()) {
            apiTokenInput.setText(token)
        }

        // Sync status
        updateSyncStatus()

        // Filter
        filterSpinner.setSelection(settingsManager.getQuoteFilter().ordinal)

        // Style
        styleSpinner.setSelection(settingsManager.getVisualStyle().ordinal)

        // Duration
        val duration = settingsManager.getQuoteDuration()
        durationSeekBar.progress = duration
        durationValue.text = getString(R.string.duration_format, duration)

        // Sync interval
        val intervalPosition = when (settingsManager.getSyncIntervalHours()) {
            1 -> 0
            6 -> 1
            24 -> 2
            else -> 3
        }
        syncIntervalSpinner.setSelection(intervalPosition)
    }

    private fun updateSyncStatus() {
        lifecycleScope.launch {
            val lastSync = settingsManager.getLastSyncTime()
            val count = quoteRepository.getQuoteCount()

            val timeText = if (lastSync != null) {
                try {
                    val instant = Instant.parse(lastSync)
                    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                } catch (e: Exception) {
                    getString(R.string.never_synced)
                }
            } else {
                getString(R.string.never_synced)
            }

            syncStatus.text = "$timeText ($count quotes)"
        }
    }

    private fun verifyAndSaveToken(token: String) {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = "Verifying token..."

            val isValid = quoteRepository.verifyToken(token)
            if (isValid) {
                settingsManager.setApiToken(token)
                syncStatus.text = "Token verified!"
                performSync()
            } else {
                syncStatus.text = "Invalid token"
                Toast.makeText(this@SettingsActivity, "Invalid API token", Toast.LENGTH_SHORT).show()
            }
            syncButton.isEnabled = true
        }
    }

    private fun performSync() {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = getString(R.string.syncing)

            when (val result = quoteRepository.sync()) {
                is SyncResult.Success -> {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Synced ${result.count} quotes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is SyncResult.Error -> {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Sync failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            updateSyncStatus()
            syncButton.isEnabled = true
        }
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add SettingsActivity with full configuration UI"
```

---

## Task 10: Create MainActivity

**Files:**
- Create: `app/src/main/java/com/readwisequotes/ui/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`

**Step 1: Create main layout**

```xml
<!-- app/src/main/res/layout/activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.readwisequotes.ui.QuoteDisplayView
        android:id="@+id/quoteDisplayView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- Empty state -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:background="@color/background_dark"
        android:visibility="gone">

        <TextView
            android:id="@+id/emptyStateText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_primary"
            android:textSize="24sp"
            android:gravity="center"
            android:layout_marginBottom="24dp" />

        <Button
            android:id="@+id/openSettingsButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_title" />
    </LinearLayout>

</FrameLayout>
```

**Step 2: Create MainActivity**

```kotlin
// app/src/main/java/com/readwisequotes/ui/MainActivity.kt
package com.readwisequotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var quoteDisplayView: QuoteDisplayView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var openSettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quoteDisplayView = findViewById(R.id.quoteDisplayView)
        emptyState = findViewById(R.id.emptyState)
        emptyStateText = findViewById(R.id.emptyStateText)
        openSettingsButton = findViewById(R.id.openSettingsButton)

        openSettingsButton.setOnClickListener {
            openSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        initializeDisplay()
    }

    override fun onPause() {
        super.onPause()
        quoteDisplayView.stop()
    }

    private fun initializeDisplay() {
        if (!settingsManager.isSetupComplete()) {
            showEmptyState(getString(R.string.setup_required))
            return
        }

        // Apply settings
        quoteDisplayView.setVisualStyle(settingsManager.getVisualStyle())
        quoteDisplayView.setQuoteDuration(settingsManager.getQuoteDuration())

        // Check if sync needed
        lifecycleScope.launch {
            if (settingsManager.shouldSync()) {
                quoteRepository.sync()
            }

            // Observe quotes
            quoteRepository.getQuotes().collectLatest { quotes ->
                if (quotes.isEmpty()) {
                    showEmptyState(getString(R.string.no_quotes))
                } else {
                    hideEmptyState()
                    quoteDisplayView.setQuotes(quotes)
                    quoteDisplayView.start()
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateText.text = message
        emptyState.visibility = View.VISIBLE
        quoteDisplayView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        quoteDisplayView.visibility = View.VISIBLE
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Open settings on OK/Select button press
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add MainActivity with quote display and settings access"
```

---

## Task 11: Create DreamService (Screensaver)

**Files:**
- Create: `app/src/main/java/com/readwisequotes/QuoteDreamService.kt`

**Step 1: Create DreamService**

```kotlin
// app/src/main/java/com/readwisequotes/QuoteDreamService.kt
package com.readwisequotes

import android.service.dreams.DreamService
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.ui.QuoteDisplayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class QuoteDreamService : DreamService() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var quoteDisplayView: QuoteDisplayView
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Configure dream
        isInteractive = true
        isFullscreen = true
        isScreenBright = false

        // Create and set content view
        quoteDisplayView = QuoteDisplayView(this)
        setContentView(quoteDisplayView)

        // Apply settings
        quoteDisplayView.setVisualStyle(settingsManager.getVisualStyle())
        quoteDisplayView.setQuoteDuration(settingsManager.getQuoteDuration())
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        serviceScope.launch {
            // Sync if needed
            if (settingsManager.shouldSync()) {
                quoteRepository.sync()
            }

            // Observe and display quotes
            quoteRepository.getQuotes().collectLatest { quotes ->
                if (quotes.isNotEmpty()) {
                    quoteDisplayView.setQuotes(quotes)
                    quoteDisplayView.start()
                }
            }
        }
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        quoteDisplayView.stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        serviceScope.cancel()
    }
}
```

**Step 2: Commit**

```bash
git add -A
git commit -m "feat: add QuoteDreamService for screensaver functionality"
```

---

## Task 12: Create Directory Structure and Build

**Step 1: Create all necessary directories**

```bash
mkdir -p app/src/main/java/com/readwisequotes/{data/{local,model,remote},di,settings,ui}
mkdir -p app/src/main/res/{drawable,layout,values,xml}
mkdir -p app/src/test/java/com/readwisequotes
```

**Step 2: Add proguard rules file**

```
# app/proguard-rules.pro
# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*

# Keep Gson
-keep class com.readwisequotes.data.model.** { *; }
```

**Step 3: Verify Gradle wrapper exists**

```bash
ls -la gradle/wrapper/
```

If missing, create wrapper:
```bash
gradle wrapper --gradle-version 8.2
```

**Step 4: Build the project**

```bash
./gradlew assembleDebug
```

**Step 5: Commit final structure**

```bash
git add -A
git commit -m "chore: finalize project structure and build configuration"
```

---

## Task 13: Generate Release APK

**Step 1: Build release APK**

```bash
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release-unsigned.apk`

**Step 2: (Optional) Sign APK for distribution**

For personal use, the debug APK works fine:
```bash
./gradlew assembleDebug
```

Debug APK location: `app/build/outputs/apk/debug/app-debug.apk`

**Step 3: Host APK for Downloader app**

Options:
- Upload to GitHub Releases
- Host on personal server
- Use any file hosting with direct download link

**Step 4: Commit and tag release**

```bash
git add -A
git commit -m "chore: prepare v1.0.0 release"
git tag v1.0.0
git push origin main --tags
```

---

## Installation Instructions (for reference)

### On Sony Bravia TV:

1. Open **Downloader** app
2. Enter APK URL
3. Install the app
4. Open **Readwise Quotes** from apps
5. Enter API token from readwise.io/access_token
6. Wait for sync
7. Configure preferences

### Set as System Screensaver:

```bash
# From computer, connect to TV
adb connect <TV_IP>

# Set screensaver
adb shell settings put secure screensaver_components com.readwisequotes/.QuoteDreamService

# Set timeout (optional, in ms - 60000 = 1 minute)
adb shell settings put system screen_off_timeout 60000
```

---

**Plan complete and saved to `docs/plans/2025-12-16-implementation-plan.md`.**

Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session in this directory with executing-plans skill

Which approach?
