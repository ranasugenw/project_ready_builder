
# Project Ready Builder - Instructions

This package prepares your project so you can build an Android APK with minimum manual setup.
We made these helper scripts to run locally on your machine (Windows PowerShell or Unix shell).

## What we prepared for you
- `build_scripts/build_windows.ps1` - PowerShell script that will build frontend and Android APK.
- `build_scripts/build_unix.sh` - Bash script (Linux/macOS) that does the same.
- `local.properties.template` - Template for Android SDK path.
- `README.md` - This file.

## How to use (Windows)
1. Install prerequisites:
   - Java JDK 11 or later
   - Android SDK (Android Studio or command-line tools)
   - Node.js (LTS) + npm
   - Optional: Gradle (if `gradlew` wrapper is missing)

2. Open PowerShell in the project root and run:
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   .\build_scripts\build_windows.ps1
   ```
3. If `local.properties` wasn't created automatically, edit it and set your SDK path (sdk.dir=...).
4. After the script finishes, the APK (if built) will be in `build_output/` inside the root of this package.

## How to use (Linux/macOS)
1. Install Java, Android SDK, Node.js, npm.
2. Run:
   ```bash
   ./build_scripts/build_unix.sh
   ```
3. Edit `local.properties` if needed.
4. APK will be copied to `build_output/` on success.

## Troubleshooting
- If build fails due to missing `gradlew` wrapper, install Gradle or add the Gradle wrapper files to the project.
- If frontend build fails, run `npm install` and `npm run build` manually in the `project/` folder to inspect errors.
- Check `app/build/outputs/` for logs and APK files.
- If app crashes on device, view `adb logcat` to see runtime error stack traces.

## Notes from me (assistant)
- I couldn't run the builds here due to environment limitations, so this package contains helper scripts for you to run locally.
- If you want, I can try to add a Gradle wrapper to the project, but that requires generating `gradle-wrapper.jar` which is not possible in this environment. Better approach: run Gradle wrapper generation on your machine:
  1. Install Gradle.
  2. Run `gradle wrapper` in the project root.
