#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WRAPPER_FILE="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"
BUILD_FILE="$ROOT_DIR/build.gradle.kts"

agp="$(grep -E 'id\("com\.android\.application"\) version' "$BUILD_FILE" | sed -nE 's/.*version "([0-9]+\.[0-9]+\.[0-9]+)".*/\1/p' | head -n1)"
gradle="$(grep -oE 'gradle-[0-9]+\.[0-9]+(\.[0-9]+)?-bin\.zip' "$WRAPPER_FILE" | head -n1 | sed -E 's/gradle-([0-9.]+)-bin\.zip/\1/')"

if [[ -z "$agp" || -z "$gradle" ]]; then
  echo "ERROR: Could not determine AGP or Gradle Wrapper version."
  exit 1
fi

if [[ "$agp" == 8.7.* ]]; then
  expected="8.9"
else
  echo "ERROR: This project's compatibility guard currently supports AGP 8.7.x only."
  echo "Detected AGP: $agp"
  echo "Update build.gradle.kts and this compatibility guard together when upgrading AGP."
  exit 1
fi

if [[ "$gradle" != "$expected" ]]; then
  echo "ERROR: AGP $agp requires Gradle $expected for this project, but Wrapper is $gradle."
  echo "Update gradle/wrapper/gradle-wrapper.properties."
  exit 1
fi

echo "Build tool compatibility OK: AGP $agp + Gradle $gradle"
