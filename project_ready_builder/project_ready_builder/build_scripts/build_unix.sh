#!/usr/bin/env bash
set -e
echo "== Project Ready Builder (Unix) =="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../project"
ANDROID_APP="$PROJECT_ROOT/app"
FRONTEND_DIR="$PROJECT_ROOT"

# Check node
command -v node >/dev/null 2>&1 || { echo "Node is required. Install Node.js and try again." >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "npm is required. Install Node.js and try again." >&2; exit 1; }

# Build frontend
echo "Running npm install && npm run build in frontend..."
cd "$FRONTEND_DIR"
npm install
npm run build

# Determine dist folder
if [ -d "$FRONTEND_DIR/dist" ]; then
  DIST="$FRONTEND_DIR/dist"
elif [ -d "$FRONTEND_DIR/build" ]; then
  DIST="$FRONTEND_DIR/build"
else
  echo "Could not find frontend build output (dist/ or build/). Check your Vite config."
  exit 1
fi

# Copy to Android assets
TARGET_ASSETS="$ANDROID_APP/src/main/assets/www"
mkdir -p "$TARGET_ASSETS"
rsync -a --delete "$DIST/" "$TARGET_ASSETS/"

# Create local.properties if missing
if [ ! -f "$PROJECT_ROOT/local.properties" ]; then
  if [ ! -z "$ANDROID_SDK_ROOT" ]; then
    echo "sdk.dir=$ANDROID_SDK_ROOT" > "$PROJECT_ROOT/local.properties"
    echo "Created local.properties from \$ANDROID_SDK_ROOT"
  else
    cp "$SCRIPT_DIR/../local.properties.template" "$PROJECT_ROOT/local.properties"
    echo "local.properties created from template. Edit it to set your SDK path."
  fi
else
  echo "local.properties already present."
fi

# Build APK (prefer wrapper)
cd "$PROJECT_ROOT"
if [ -f "./gradlew" ]; then
  ./gradlew assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug
else
  echo "No gradlew wrapper found and system 'gradle' not available. Install Gradle or add gradlew into project."
  exit 1
fi

# Copy APK
APK_PATH=$(find "$ANDROID_APP/build/outputs/apk" -name "*.apk" | tail -n 1)
if [ -n "$APK_PATH" ]; then
  mkdir -p "$SCRIPT_DIR/../build_output"
  cp "$APK_PATH" "$SCRIPT_DIR/../build_output/"
  echo "APK copied to build_output/"
else
  echo "No APK found. Build may have failed."
fi

echo "Done."