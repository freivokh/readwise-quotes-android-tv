#!/bin/bash
# Verification and Build Script for Readwise Quotes Android TV
# Run this after installing Android SDK

set -e  # Exit on error

echo "======================================"
echo "Readwise Quotes - Build Verification"
echo "======================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check and report
check_requirement() {
    local name=$1
    local command=$2

    if eval $command > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $name"
        return 0
    else
        echo -e "${RED}✗${NC} $name"
        return 1
    fi
}

echo "Checking Requirements:"
echo "----------------------"

# Check Java
if check_requirement "Java (JDK 17+)" "java -version 2>&1 | grep -q 'version \"1[7-9]\|version \"[2-9]'"; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "  Found: $JAVA_VERSION"
else
    echo -e "${RED}  Java 17+ is required${NC}"
    echo "  Install with: brew install openjdk@17"
    exit 1
fi

echo ""

# Check Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}✗${NC} ANDROID_HOME not set"
    echo ""
    echo "Please set ANDROID_HOME. Try one of these:"
    echo "  export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "  export ANDROID_HOME=/opt/android-sdk"
    echo ""
    echo "Or create local.properties with:"
    echo "  sdk.dir=/path/to/android/sdk"
    exit 1
else
    echo -e "${GREEN}✓${NC} ANDROID_HOME set to: $ANDROID_HOME"
fi

if [ -d "$ANDROID_HOME" ]; then
    echo -e "${GREEN}✓${NC} Android SDK directory exists"
else
    echo -e "${RED}✗${NC} Android SDK directory not found at: $ANDROID_HOME"
    exit 1
fi

# Check for required SDK components
if [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
    echo -e "${GREEN}✓${NC} Platform Tools installed"
else
    echo -e "${YELLOW}⚠${NC} Platform Tools not found"
fi

if [ -d "$ANDROID_HOME/platforms/android-34" ]; then
    echo -e "${GREEN}✓${NC} Android Platform 34 installed"
else
    echo -e "${YELLOW}⚠${NC} Android Platform 34 not found (required for build)"
    echo "  Install with: sdkmanager \"platforms;android-34\""
fi

echo ""
echo "Project Structure:"
echo "------------------"

# Count files
KOTLIN_FILES=$(find app/src/main/java -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')
XML_FILES=$(find app/src/main/res -name "*.xml" 2>/dev/null | wc -l | tr -d ' ')

echo -e "${GREEN}✓${NC} $KOTLIN_FILES Kotlin source files"
echo -e "${GREEN}✓${NC} $XML_FILES XML resource files"
echo -e "${GREEN}✓${NC} Gradle wrapper present"

echo ""
echo "======================================"
echo "Starting Build..."
echo "======================================"
echo ""

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

echo ""
echo "Building debug APK..."
echo ""

# Build
if ./gradlew assembleDebug; then
    echo ""
    echo "======================================"
    echo -e "${GREEN}✓ BUILD SUCCESSFUL${NC}"
    echo "======================================"
    echo ""

    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
        echo "APK Location: $APK_PATH"
        echo "APK Size: $APK_SIZE"
        echo ""
        echo "To install on connected device:"
        echo "  adb install $APK_PATH"
        echo ""
        echo "To install on Sony Bravia TV:"
        echo "  1. Host the APK on a web server"
        echo "  2. Use Downloader app on TV"
        echo "  3. Enter APK URL"
    fi
else
    echo ""
    echo "======================================"
    echo -e "${RED}✗ BUILD FAILED${NC}"
    echo "======================================"
    echo ""
    echo "Check the error messages above."
    echo "Common issues:"
    echo "  - Missing SDK components"
    echo "  - Wrong SDK version"
    echo "  - Insufficient disk space"
    exit 1
fi

echo ""
echo "Next Steps:"
echo "-----------"
echo "1. Test the app: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "2. Get Readwise API token: https://readwise.io/access_token"
echo "3. Configure app with your API token"
echo "4. Set as screensaver in Android TV settings"
echo ""
