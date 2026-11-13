#!/usr/bin/env bash
set -euo pipefail

REPO_NAME="Kabox"
REPO_VISIBILITY="public" # Change to "private" before running if preferred.

if ! command -v git >/dev/null 2>&1; then
  echo "Git is required. Install Git, then run this script again." >&2
  exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required. Install it from https://cli.github.com/" >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "Please sign in to GitHub first: gh auth login" >&2
  exit 1
fi

ACCOUNT="$(gh api user --jq .login)"
REMOTE_URL="https://github.com/${ACCOUNT}/${REPO_NAME}.git"

if [ ! -d .git ]; then
  git init
fi

git add -A
if ! git diff --cached --quiet; then
  git -c user.name="${ACCOUNT}" -c user.email="${ACCOUNT}@users.noreply.github.com" commit -m "Initial Kabox Android project"
fi

git branch -M main
if gh repo view "${ACCOUNT}/${REPO_NAME}" >/dev/null 2>&1; then
  if ! git remote get-url origin >/dev/null 2>&1; then
    git remote add origin "${REMOTE_URL}"
  else
    git remote set-url origin "${REMOTE_URL}"
  fi
  git push -u origin main
else
  gh repo create "${REPO_NAME}" "--${REPO_VISIBILITY}" --source=. --remote=origin --push
fi

echo "Done. Repository: ${REMOTE_URL}"
