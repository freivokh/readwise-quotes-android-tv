# Readwise Quotes Android TV

An Android TV screensaver app that displays Readwise highlights with ambient animations.

## Project Overview

- **Target**: Sony Bravia 4K (Android TV OS 10+)
- **Dual Mode**: Standalone app (MainActivity) + System screensaver (QuoteDreamService)
- **Tech Stack**: Kotlin, Room, Retrofit, Hilt, Leanback

## Architecture

```
MainActivity / QuoteDreamService
        ↓
  QuoteDisplayView (shared UI)
        ↓
   QuoteRepository (data layer)
        ↓
  SettingsManager (preferences)
```

## Key Files

### UI Layer (`app/src/main/java/com/readwisequotes/ui/`)
- `MainActivity.kt` - Standalone app entry, quote display
- `SettingsActivity.kt` - Configuration screen with D-pad navigation
- `QuoteDisplayView.kt` - Shared quote rendering with animations
- `GradientBackgroundView.kt` - Animated ambient background
- `FocusAwareScrollView.kt` - Custom ScrollView that skips hidden views for D-pad nav

### Data Layer (`app/src/main/java/com/readwisequotes/data/`)
- `QuoteRepository.kt` - Data access, Readwise API sync
- `model/Quote.kt` - Room entity for highlights
- `model/TagGroup.kt` - Tag grouping for filtering
- `local/QuoteDao.kt` - Room database queries
- `remote/ReadwiseApi.kt` - Retrofit API interface

### Settings (`app/src/main/java/com/readwisequotes/settings/`)
- `SettingsManager.kt` - SharedPreferences wrapper, tag groups, filter modes
- `QuoteFilter.kt` - Filter enum (ALL, FAVORITES, BY_TAG, RECENT)
- `VisualStyle.kt` - Style enum (AMBIENT, MINIMAL)

### Layouts (`app/src/main/res/layout/`)
- `activity_settings.xml` - Settings screen with FocusAwareScrollView
- `item_tag_group.xml` - Tag group row (checkbox, edit, delete)
- `dialog_tag_selection.xml` - Tag selection dialog

## D-Pad Navigation

This app uses custom focus handling for Android TV remote navigation:

1. **FocusAwareScrollView** - Overrides `focusSearch()` to skip hidden views
2. **dispatchKeyEvent()** in SettingsActivity - Intercepts D-pad when tag section hidden
3. **Dynamic focus chains** - Set programmatically in `renderTagGroups()`
4. **Dialog shortcuts** - LEFT=Cancel, RIGHT=Save from any tag checkbox

## Development

### Build & Run
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
./gradlew installDebug
adb shell am start -n com.readwisequotes/.ui.MainActivity
```

### Emulator
```bash
/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd AndroidTV &
```

### Testing Focus Navigation
Use arrow keys or ADB:
```bash
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_CENTER
```

## Documentation

- `docs/TESTING.md` - Emulator setup, keyboard controls, ADB commands
- `docs/ANDROID_TV_BEST_PRACTICES.md` - D-pad navigation patterns, focus handling
- `docs/SETUP_ANDROID_SDK.md` - SDK installation guide
- `docs/plans/2025-12-16-readwise-quotes-design.md` - Original design spec
- `docs/plans/2025-12-16-implementation-plan.md` - Task-by-task implementation

## API

- **Readwise Export**: `GET https://readwise.io/api/v2/export/`
- **Auth**: `Authorization: Token <access_token>` header
- **Token URL**: https://readwise.io/access_token
