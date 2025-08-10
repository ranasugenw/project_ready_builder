<#
PowerShell build script for the project.
Usage (PowerShell as Admin or normal):
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    .\build_windows.ps1

This script attempts to:
1. Ensure Node and Java are available.
2. Build the web frontend (Vite) and copy the output to Android assets.
3. Create a local.properties template if missing (you must edit it to set sdk.dir).
4. Run Gradle to assemble the APK (uses gradlew if present, else system gradle).
#>

function ExitWith($msg) {
    Write-Host $msg -ForegroundColor Red
    exit 1
}

Write-Host "== Project Ready Builder (Windows) =="

# Paths
$projectRoot = Join-Path -Path $PSScriptRoot -ChildPath "..\project"
$androidApp = Join-Path -Path $projectRoot -ChildPath "app"
$frontendDir = $projectRoot

# Check Node
$node = Get-Command node -ErrorAction SilentlyContinue
$npm = Get-Command npm -ErrorAction SilentlyContinue
if (-not $node -or -not $npm) {
    ExitWith "Node or npm not found. Install Node.js (LTS) and try again."
}

# Build frontend
Write-Host "Running npm install && npm run build in frontend..."
Push-Location $frontendDir
npm install
if ($LASTEXITCODE -ne 0) { ExitWith "npm install failed." }
npm run build
if ($LASTEXITCODE -ne 0) { ExitWith "npm run build failed." }
Pop-Location

# Determine build output folder (Vite usually outputs 'dist')
$distFolder = Join-Path $frontendDir "dist"
if (-not (Test-Path $distFolder)) {
    $distFolder = Join-Path $frontendDir "build"
}
if (-not (Test-Path $distFolder)) {
    ExitWith "Could not find frontend build output (dist/ or build/). Check your Vite config."
}

# Copy frontend assets to Android app assets/www
$targetAssets = Join-Path $androidApp "src\main\assets\www"
New-Item -ItemType Directory -Force -Path $targetAssets | Out-Null
Write-Host "Copying frontend build to $targetAssets ..."
robocopy $distFolder $targetAssets /MIR | Out-Null

# Ensure local.properties exists
$localProps = Join-Path $projectRoot "local.properties"
if (-not (Test-Path $localProps)) {
    $sdkEnv = $env:ANDROID_SDK_ROOT
    if (-not $sdkEnv) { $sdkEnv = $env:ANDROID_HOME }
    if ($sdkEnv) {
        $content = "sdk.dir=$sdkEnv".Replace("/","\\")
        Set-Content -Path $localProps -Value $content -Encoding UTF8
        Write-Host "Created local.properties with sdk.dir from environment."
    } else {
        Copy-Item -Path (Join-Path $PSScriptRoot "..\local.properties.template") -Destination $localProps
        Write-Host "local.properties created from template. Edit it to set your SDK path: $localProps"
    }
} else {
    Write-Host "local.properties already present."
}

# Run Gradle build (prefer gradlew)
$gradlew = Join-Path $projectRoot "gradlew.bat"
Push-Location $projectRoot
if (Test-Path $gradlew) {
    Write-Host "Using gradlew.bat to build APK..."
    & $gradlew assembleDebug
    if ($LASTEXITCODE -ne 0) { ExitWith "gradlew build failed." }
} else {
    $gradle = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradle) {
        Write-Host "Using system gradle to build APK..."
        gradle assembleDebug
        if ($LASTEXITCODE -ne 0) { ExitWith "gradle build failed." }
    } else {
        ExitWith "No gradlew wrapper found and system 'gradle' not available. Install Gradle or add gradlew into project."
    }
}
Pop-Location

# Copy APK to output
$apkSrc = Get-ChildItem -Path (Join-Path $androidApp "build\outputs\apk") -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue | Select-Object -Last 1
if ($apkSrc) {
    $outDir = Join-Path $PSScriptRoot "..\build_output"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    Copy-Item -Path $apkSrc.FullName -Destination $outDir -Force
    Write-Host "APK copied to: $outDir"
} else {
    Write-Host "No APK found in app/build/outputs/apk. Build may have failed or path differs."
}

Write-Host "Done."