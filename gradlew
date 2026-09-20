#!/bin/sh

# Minimal Gradle wrapper launcher for TimeDensity-Resonator.
# The Gradle distribution and wrapper JAR are resolved from gradle-wrapper.properties.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_PROPS" ]; then
  echo "Missing $WRAPPER_PROPS" >&2
  exit 1
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle is not installed and the Gradle wrapper JAR is not available in this checkout." >&2
echo "Install Gradle or add gradle/wrapper/gradle-wrapper.jar, then rerun: ./gradlew $*" >&2
exit 1
