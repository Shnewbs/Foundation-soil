@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo [Foundations Soil] 0.0.3a.R6 Field Guide Soil Management Hardening build bootstrap
where java >nul 2>nul || (
  echo ERROR: Java 21 was not found on PATH.
  exit /b 1
)

if not exist "gradle\wrapper\gradle-wrapper.jar" (
  echo Downloading official NeoForge MDK Gradle wrapper JAR...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$u='https://raw.githubusercontent.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/main/gradle/wrapper/gradle-wrapper.jar'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
  if errorlevel 1 (
    echo ERROR: Could not download the Gradle wrapper JAR.
    exit /b 1
  )
)

if not exist "libs\foundations_framework-0.0.5a.jar" (
  echo NOTE: libs\foundations_framework-0.0.5a.jar is not present.
  echo       Soil uses a reflection bridge and does not require Framework API classes at compile time.
  echo       Minecraft runtime still requires a compatible Foundations Framework ^>= 0.0.5a.
)

call gradlew.bat --no-daemon clean test build
if errorlevel 1 exit /b %errorlevel%

echo.
echo Build complete. Check build\libs\foundations_soil-1.21.1-0.0.3a.R6.jar
