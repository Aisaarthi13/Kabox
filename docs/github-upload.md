# Publish the Kabox project to GitHub in one command

The ZIP must be extracted once before Git can publish the source tree. The publishing script then stages, commits, creates the repository if needed, and pushes everything. You do not need to upload files one by one.

## One-time setup
Install **Git** and **GitHub CLI** (`gh`) on your computer, then sign in:

```bash
gh auth login
```

Choose GitHub.com, HTTPS, and browser authentication when prompted.

## Windows
Open PowerShell in the extracted `Kabox` project folder and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\publish-to-github.ps1
```

## macOS / Linux
Open Terminal in the extracted `Kabox` project folder and run:

```bash
bash ./scripts/publish-to-github.sh
```

## Android with Termux
Install Termux from its official distribution source. In Termux, run this one-time setup:

```bash
pkg update -y && pkg install git gh unzip -y
termux-setup-storage
gh auth login
```

Complete GitHub's browser/device sign-in once. After downloading the ZIP to your phone's Downloads folder, publish the full source tree with this single command:

```bash
mkdir -p ~/kabox-upload && unzip -o ~/storage/downloads/Kabox-Android-Source.zip -d ~/kabox-upload && cd ~/kabox-upload/Kabox && bash ./scripts/publish-to-github.sh
```

The `gh` package is provided by the Termux community package repository. If it is unavailable in your Termux installation, use a computer with GitHub CLI.

## What the script does
- Creates a public repository named `Kabox` if it does not already exist under your GitHub account.
- If that repository already exists, connects this folder to it.
- Commits the project and pushes the `main` branch.
- Never asks for your GitHub password inside the script; authentication is handled by GitHub CLI.

To make the repository private, edit `REPO_VISIBILITY` near the top of either script from `public` to `private` before running it.
