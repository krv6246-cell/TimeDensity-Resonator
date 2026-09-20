@echo off
setlocal

rem Minimal Gradle wrapper launcher for TimeDensity-Resonator.
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Gradle is not installed and the Gradle wrapper JAR is not available in this checkout.
echo Install Gradle or add gradle/wrapper/gradle-wrapper.jar, then rerun: gradlew %*
exit /b 1
