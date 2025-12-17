# Readwise Quotes - Android TV Screensaver App

## Overview

An Android TV app that displays your Readwise highlights as a beautiful screensaver. Works both as a system screensaver (DreamService) and as a standalone app.

**Target Device**: Sony Bravia 4K (Android TV OS 10)

## Architecture

```
┌─────────────────────────────────────────────┐
│              Readwise Quotes                │
├─────────────────────────────────────────────┤
│  MainActivity          │  QuoteDreamService │
│  (Standalone App)      │  (Screensaver)     │
│         │              │         │          │
│         └──────┬───────┴─────────┘          │
│                ▼                            │
│        QuoteDisplayView                     │
│   (Shared UI - quotes + animations)         │
│                │                            │
│                ▼                            │
│         QuoteRepository                     │
│   (Local DB + Readwise API sync)            │
│                │                            │
│                ▼                            │
│        SettingsManager                      │
│   (Filters, timing, visual style)           │
└─────────────────────────────────────────────┘
```

Both modes share the same quote display logic and data layer.

## Data Model

### Local Storage (Room/SQLite)

```kotlin
@Entity
data class Quote(
    @PrimaryKey val id: String,      // Readwise highlight ID
    val text: String,                 // The highlight text
    val title: String?,               // Book/article name
    val author: String?,              // Author name
    val bookCover: String?,           // Cover image URL
    val tags: List<String>,           // Tags from Readwise
    val isFavorite: Boolean,          // Favorited in Readwise
    val updatedAt: String,            // ISO timestamp
    val sourceType: String            // book, article, podcast, etc.
)

@Entity
data class SyncMetadata(
    @PrimaryKey val id: Int = 1,
    val lastSyncedAt: String?,        // ISO timestamp
    val apiToken: String              // Encrypted
)
```

### Sync Strategy

1. **Initial sync**: Paginated fetch of all highlights via `GET /api/v2/export/`
2. **Incremental sync**: Use `?updatedAfter=` parameter to fetch only new/updated quotes
3. **Sync trigger**: On app/screensaver launch if cache older than configured interval (default 24h)
4. **Offline support**: Always displays from local cache

## Visual Design

### Ambient Mode (Default)

- Slow-moving gradient background (dark blues/purples)
- Large, readable quote text (serif or sans-serif)
- Author and source displayed below quote
- Smooth fade transitions between quotes

### Minimal Mode (OLED-friendly)

- Pure black background (#000000)
- White text
- No animations
- Maximum contrast

### Typography

- Quote text: Auto-scales based on length (never unreadable)
- Attribution: Smaller text below quote
- TV-optimized: Readable from typical viewing distance

## Settings

Accessible via:
- Launching app directly
- Pressing OK/Select during screensaver

### Configuration Options

| Setting | Options | Default |
|---------|---------|---------|
| Quote Filter | All / Favorites / By Tag / Recent | All |
| Tags | Multi-select from synced tags | None |
| Visual Style | Ambient / Minimal | Ambient |
| Quote Duration | 10-60 seconds | 20 seconds |
| Transition Speed | Fast / Smooth / Slow | Smooth |
| Auto-sync Interval | 1h / 6h / 24h / Manual | 24 hours |

## Installation

### Method: Downloader App

1. Open Downloader app on Sony Bravia
2. Enter APK hosting URL
3. Install app
4. Grant permissions if prompted

### First Launch Setup

1. Open "Readwise Quotes" from apps
2. Enter Readwise API token (from readwise.io/access_token)
3. Wait for initial sync to complete
4. Configure preferences

### Set as System Screensaver (Optional)

```bash
adb connect <TV_IP_ADDRESS>
adb shell settings put secure screensaver_components com.readwisequotes/.QuoteDreamService
```

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 29 (Android 10)
- **Target**: Android TV (Leanback)
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **DI**: Hilt
- **UI**: Custom View with Canvas animations

## API Integration

### Readwise API

- **Auth**: `Authorization: Token <access_token>` header
- **Export endpoint**: `GET https://readwise.io/api/v2/export/`
- **Rate limit**: 240 req/min (20/min for list endpoints)
- **Pagination**: Via `pageCursor` parameter

## Package Structure

```
com.readwisequotes/
├── MainActivity.kt           # Standalone app entry
├── QuoteDreamService.kt      # Screensaver service
├── ui/
│   ├── QuoteDisplayView.kt   # Shared quote display
│   ├── SettingsActivity.kt   # Configuration screen
│   └── GradientBackground.kt # Animated background
├── data/
│   ├── QuoteRepository.kt    # Data access layer
│   ├── QuoteDao.kt           # Room DAO
│   ├── AppDatabase.kt        # Room database
│   └── ReadwiseApi.kt        # API interface
├── sync/
│   └── SyncManager.kt        # Sync orchestration
└── settings/
    └── SettingsManager.kt    # SharedPreferences wrapper
```
