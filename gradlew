#!/usr/bin/env bash
# Small Gradle bootstrapper for this project (the distribution is cached per user).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS="$ROOT/gradle/wrapper/gradle-wrapper.properties"
URL="$(sed -n 's/^distributionUrl=//p' "$PROPS" | sed 's/\\:/:/g')"
VERSION="$(basename "$URL" | sed -E 's/^gradle-([0-9.]+)-bin\.zip$/\1/')"
CACHE_ROOT="${GRADLE_USER_HOME:-$HOME/.gradle}/kabox-bootstrap"
DIST="$CACHE_ROOT/gradle-$VERSION"

if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE_ROOT"
  ARCHIVE="$CACHE_ROOT/gradle-$VERSION-bin.zip"
  echo "Downloading Gradle $VERSION on first run..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Install curl or wget to download Gradle." >&2
    exit 1
  fi
  unzip -q -o "$ARCHIVE" -d "$CACHE_ROOT"
  rm -f "$ARCHIVE"
fi
cd "$ROOT"
exec "$DIST/bin/gradle" "$@"
