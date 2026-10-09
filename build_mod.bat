@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo [Foundations Soil] Minecraft 26.1.2 / 0.1.0-beta-port.1 native build
where java >nul 2>nul || (echo ERROR: Java 25 is required on PATH. & exit /b 1)
if not exist "gradle\wrapper\gradle-wrapper.jar" (echo ERROR: Gradle wrapper missing; extract the complete source package. & exit /b 1)
call gradlew.bat --no-daemon clean test build soilScaleBenchmark
if errorlevel 1 exit /b %errorlevel%
echo Native compiled port: build\libs\foundations_soil-26.1.2-0.1.0-beta-port.1.jar
echo Requires genuine native TFC/Framework. Model tests do not establish a playable modpack.
