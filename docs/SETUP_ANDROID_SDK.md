# Android SDK Setup Guide

This guide will help you install the Android SDK required to build the Readwise Quotes Android TV app.

## Option 1: Install Android Studio (Recommended)

Android Studio includes the Android SDK and provides the easiest setup experience.

### Steps:

1. **Download Android Studio**
   - Visit: https://developer.android.com/studio
   - Download the macOS version (for Apple Silicon or Intel)
   - Size: ~1.1 GB download

2. **Install Android Studio**
   ```bash
   # Move to Applications folder
   open ~/Downloads  # Navigate to downloaded file
   # Drag Android Studio.app to Applications folder
   ```

3. **Launch Android Studio**
   - Open Android Studio from Applications
   - Follow the setup wizard
   - Choose "Standard" installation
   - Accept licenses when prompted

4. **Verify SDK Location**
   - Open Android Studio
   - Go to: **Preferences** → **Appearance & Behavior** → **System Settings** → **Android SDK**
   - Default location: `/Users/YOUR_USERNAME/Library/Android/sdk`
   - Note this path for later

5. **Install Required SDK Components**

   In the Android SDK settings, ensure these are installed:
   - ✓ Android SDK Platform 34
   - ✓ Android SDK Build-Tools 34.0.0
   - ✓ Android SDK Platform-Tools
   - ✓ Android SDK Command-line Tools

6. **Set Environment Variables**

   Add to your `~/.zshrc` (or `~/.bash_profile` for bash):
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

   Then reload:
   ```bash
   source ~/.zshrc
   ```

7. **Verify Installation**
   ```bash
   echo $ANDROID_HOME
   adb --version
   ```

## Option 2: Install Command Line Tools Only (Lightweight)

If you don't want the full Android Studio IDE, you can install just the command line tools.

### Steps:

1. **Download Command Line Tools**
   - Visit: https://developer.android.com/studio#command-tools
   - Scroll to "Command line tools only"
   - Download the macOS version
   - Size: ~140 MB

2. **Create SDK Directory Structure**
   ```bash
   mkdir -p ~/Library/Android/sdk/cmdline-tools
   ```

3. **Extract Command Line Tools**
   ```bash
   cd ~/Downloads
   unzip commandlinetools-mac-*.zip
   mv cmdline-tools ~/Library/Android/sdk/cmdline-tools/latest
   ```

4. **Set Environment Variables**

   Add to your `~/.zshrc`:
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

   Reload:
   ```bash
   source ~/.zshrc
   ```

5. **Accept Licenses**
   ```bash
   yes | sdkmanager --licenses
   ```

6. **Install Required Packages**
   ```bash
   sdkmanager "platform-tools"
   sdkmanager "platforms;android-34"
   sdkmanager "build-tools;34.0.0"
   ```

7. **Verify Installation**
   ```bash
   sdkmanager --list_installed
   ```

## Option 3: Use Homebrew (Experimental)

You can try installing via Homebrew, though this is less common.

```bash
brew install --cask android-commandlinetools
```

Then follow steps 5-7 from Option 2.

## Verify Your Setup

After installation via any method, run the verification script:

```bash
cd /Users/freivokh/Code/Claude/readwise-quotes-android-tv
./verify-and-build.sh
```

This will:
- Check Java installation
- Verify Android SDK setup
- Validate project structure
- Attempt to build the APK

## Build the Project

Once the SDK is installed:

```bash
# Set Java home (if using Homebrew OpenJDK)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Build debug APK
./gradlew assembleDebug
```

Expected output location:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### "SDK location not found"

**Solution 1:** Set ANDROID_HOME
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
```

**Solution 2:** Create `local.properties` file in project root:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### "Unable to locate a Java Runtime"

```bash
# Install Java 17
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### "Failed to install Android SDK"

Make sure you have enough disk space:
- Android Studio: ~4 GB
- Command Line Tools: ~2 GB

### "License not accepted"

```bash
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

## Disk Space Requirements

- **Option 1 (Android Studio):** ~4 GB
  - Android Studio IDE: 1.1 GB
  - Android SDK: 2-3 GB

- **Option 2 (Command Line):** ~2 GB
  - Command line tools: 140 MB
  - SDK Platform & Build Tools: ~2 GB

- **Project Build:** ~500 MB
  - Gradle cache: ~200 MB
  - Build outputs: ~300 MB

## Next Steps

After successful SDK installation and build:

1. **Test on Emulator:**
   ```bash
   # Create Android TV emulator in Android Studio
   # Or use: avdmanager create avd -n tv -k "system-images;android-34;google_apis;arm64-v8a"
   ```

2. **Install on Physical Device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Deploy to Sony Bravia TV:**
   - Host APK on web server or GitHub Releases
   - Use Downloader app on TV to install
   - See main README for detailed TV installation instructions

## Resources

- Android Studio: https://developer.android.com/studio
- Command Line Tools: https://developer.android.com/studio#command-tools
- SDK Manager: https://developer.android.com/studio/command-line/sdkmanager
- ADB Documentation: https://developer.android.com/studio/command-line/adb

## Getting Help

If you encounter issues:
1. Check the BUILD_STATUS.md file in this repository
2. Run `./verify-and-build.sh` for diagnostic information
3. Check Android Studio's Event Log for detailed errors
4. Ensure you have the correct Java version (17+)
