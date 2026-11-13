#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="${1:-Aisaarthi13/Kabox}"
SOURCE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WORK_DIR="$(mktemp -d)"
cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

command -v gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) is missing. Run: pkg install gh git unzip"; exit 1; }
command -v git >/dev/null 2>&1 || { echo "Git is missing. Run: pkg install git"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Sign in first with: gh auth login"; exit 1; }
gh auth setup-git >/dev/null

echo "Cloning https://github.com/${REPO} ..."
gh repo clone "$REPO" "$WORK_DIR/repo"
BRANCH="$(git -C "$WORK_DIR/repo" branch --show-current)"
if [ -z "$BRANCH" ]; then
  BRANCH="main"
  git -C "$WORK_DIR/repo" checkout -B "$BRANCH"
fi

echo "Replacing old source files with the clean Kabox v0.2.0 project ..."
find "$WORK_DIR/repo" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
cp -a "$SOURCE_DIR"/. "$WORK_DIR/repo"/

LOGIN="$(gh api user --jq .login)"
git -C "$WORK_DIR/repo" config user.name "$LOGIN"
git -C "$WORK_DIR/repo" config user.email "${LOGIN}@users.noreply.github.com"
git -C "$WORK_DIR/repo" add -A
if git -C "$WORK_DIR/repo" diff --cached --quiet; then
  echo "Repository already contains this exact source; nothing to push."
  exit 0
fi
git -C "$WORK_DIR/repo" commit -m "Rebuild Kabox with stable Android API 36 configuration"
git -C "$WORK_DIR/repo" push origin "HEAD:${BRANCH}"
echo
echo "Kabox source uploaded successfully: https://github.com/${REPO}"
echo "Open the Actions tab to watch the Android APK build."
