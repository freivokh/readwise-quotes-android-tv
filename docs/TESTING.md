# Testing Environment Documentation

This document describes the testing setup for the Readwise Quotes Android TV app.

## Current Environment

| Component | Version/Details |
|-----------|----------------|
| **macOS** | Darwin 24.6.0 (arm64 / Apple Silicon) |
| **Java** | OpenJDK 17.0.17 (Homebrew) |
| **Android SDK** | Homebrew android-commandlinetools |
| **ADB** | 36.0.0-13206524 |
| **Target Platform** | Android 14 (API 34) |
| **Emulator** | Android TV 1080p |

## Installed Dependencies

### 1. Java (OpenJDK 17)

Installed via Homebrew:
```bash
brew install openjdk@17
```

Location: `/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home`

**Important:** Java must be set before running Gradle:
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
```

### 2. Android SDK (Command Line Tools)

Installed via Homebrew:
```bash
brew install --cask android-commandlinetools
```

Location: `/opt/homebrew/share/android-commandlinetools/`

**SDK Components Installed:**
```
/opt/homebrew/share/android-commandlinetools/
├── build-tools/          # Build tools for compiling
├── cmdline-tools/        # SDK manager, avdmanager
├── emulator/             # Android emulator
├── licenses/             # Accepted licenses
├── platform-tools/       # ADB, fastboot
├── platforms/            # android-34 platform
└── system-images/        # android-34/android-tv/arm64-v8a
```

### 3. Android TV Emulator (AVD)

**AVD Name:** `AndroidTV`

**Configuration:**
- Device: `tv_1080p` (Google)
- Resolution: 1920x1080
- Density: 320 dpi
- API Level: 34 (Android 14)
- ABI: arm64-v8a
- System Image: `android-tv`
- SD Card: 512 MB
- D-Pad: Enabled

**AVD Location:** `~/.android/avd/AndroidTV.avd/`

**Critical AVD Settings** (in `config.ini`):
```ini
hw.keyboard = yes      # Enables Mac keyboard → D-pad mapping
hw.gpu.enabled = yes   # GPU acceleration for performance
```

## How to Test

### Step 1: Launch the Emulator

```bash
/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd AndroidTV &
```

Wait for boot (about 30-60 seconds):
```bash
adb wait-for-device
adb shell getprop sys.boot_completed  # Returns "1" when ready
```

### Step 2: Build and Install the App

```bash
# Set Java (required)
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home

# Build and install
./gradlew installDebug
```

### Step 3: Launch the App

```bash
adb shell am start -n com.readwisequotes/.ui.MainActivity
```

## Controlling the Emulator

### Keyboard Controls (Recommended)

With `hw.keyboard = yes` enabled in the AVD config, use your Mac's keyboard:

| Key | Action |
|-----|--------|
| Arrow keys | D-pad navigation |
| Enter | Select/OK |
| Escape | Back |
| F1 | Home |

### ADB Commands (Alternative)

If keyboard doesn't work, use ADB:

```bash
# D-Pad directions
adb shell input keyevent KEYCODE_DPAD_UP
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_LEFT
adb shell input keyevent KEYCODE_DPAD_RIGHT

# Select/Enter
adb shell input keyevent KEYCODE_DPAD_CENTER

# Back
adb shell input keyevent KEYCODE_BACK
```

### Touch/Tap

This app supports tap-to-open-settings:
```bash
# Tap at coordinates (x, y) - screen is 1920x1080
adb shell input tap 540 320  # Center-ish tap opens settings
```

### Taking Screenshots

```bash
adb exec-out screencap -p > screenshot.png
```

### Viewing Logs

```bash
# All logs from the app
adb logcat -s "SettingsManager" "MainActivity" "QuoteRepository"

# Filter by tag
adb logcat SettingsManager:D *:S
```

## Quick Reference Commands

```bash
# Set environment (run once per terminal session)
export ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home

# Full workflow
$ANDROID_SDK_ROOT/emulator/emulator -avd AndroidTV &  # Start emulator
sleep 30                                               # Wait for boot
./gradlew installDebug                                 # Build & install
adb shell am start -n com.readwisequotes/.ui.MainActivity  # Launch app
adb exec-out screencap -p > /tmp/screen.png           # Screenshot
```

## App-Specific Testing Notes

### Opening Settings
- **Method 1:** Tap anywhere on the quote display
- **Method 2:** Use the "Settings" button (if focused)

### Tag Groups Testing
1. Open Settings (tap on quote)
2. Set filter to "By Tag"
3. Scroll down to see Tag Groups section
4. Click "+ CREATE GROUP" to create a group
5. Groups persist across app restarts

### Known Emulator Notes
- Swipe gestures can trigger unintended back navigation
- First boot may show Android TV home screen (just launch app via ADB)
- If keyboard doesn't work, check AVD config has `hw.keyboard = yes`

## Troubleshooting

### "Unable to locate a Java Runtime"
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
```

### "No devices/emulators found"
```bash
# Check if emulator is running
adb devices

# If empty, start emulator
/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd AndroidTV &
```

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
```bash
# Uninstall existing app first
adb uninstall com.readwisequotes
./gradlew installDebug
```

### Emulator Frozen
```bash
# Kill and restart
pkill -f qemu-system
/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd AndroidTV -no-snapshot-load &
```

### Keyboard/D-pad Not Working
Edit `~/.android/avd/AndroidTV.avd/config.ini`:
```ini
hw.keyboard = yes
hw.gpu.enabled = yes
```
Then restart the emulator.

## File Locations Summary

| Item | Path |
|------|------|
| Android SDK | `/opt/homebrew/share/android-commandlinetools/` |
| Java Home | `/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home` |
| AVD Config | `~/.android/avd/AndroidTV.avd/config.ini` |
| ADB | `/opt/homebrew/share/android-commandlinetools/platform-tools/adb` |
| Emulator | `/opt/homebrew/share/android-commandlinetools/emulator/emulator` |
| APK Output | `app/build/outputs/apk/debug/app-debug.apk` |
| Project SDK Config | `local.properties` (sdk.dir) |
