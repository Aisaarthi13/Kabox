#!/usr/bin/env bash
# Copy this Kabox source tree into the already-created GitHub repo and push it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) is missing. Install it with: pkg install gh git" >&2; exit 1; }
command -v git >/dev/null 2>&1 || { echo "Git is missing. Install it with: pkg install git" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Please sign in first with: gh auth login" >&2; exit 1; }

OWNER="$(gh api user --jq .login)"
REPO="${GH_REPO:-${OWNER}/Kabox}"
WORK_DIR="${KABOX_UPDATE_DIR:-${HOME}/.cache/kabox-github-update}"
CLONE_DIR="${WORK_DIR}/repo"

mkdir -p "$WORK_DIR"
rm -rf "$CLONE_DIR"
echo "Cloning https://github.com/${REPO} ..."
gh repo clone "$REPO" "$CLONE_DIR"

# Copy all project files, including hidden GitHub workflow folders.
cp -a "$SCRIPT_DIR/." "$CLONE_DIR/"

cd "$CLONE_DIR"
git config user.name "$OWNER"
git config user.email "${OWNER}@users.noreply.github.com"
git add -A

if git diff --cached --quiet; then
  echo "No changes to push. The GitHub repository already has these files."
  echo "Repository: https://github.com/${REPO}"
  exit 0
fi

git commit -m "Fix Android SDK setup and refresh Kabox source"
git push origin HEAD:main

echo
echo "Kabox update pushed successfully: https://github.com/${REPO}"
echo "Open Actions to watch the new APK build."
