$ErrorActionPreference = "Stop"
$RepoName = "Kabox"
$RepoVisibility = "public" # Change to "private" before running if preferred.

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git is required. Install Git, then run this script again."
}
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI is required. Install it from https://cli.github.com/ and reopen PowerShell."
}

 gh auth status *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Sign in first by running: gh auth login"
}

$Account = gh api user --jq .login
$RemoteUrl = "https://github.com/$Account/$RepoName.git"

if (-not (Test-Path ".git")) {
    git init
}

git add -A
if ($LASTEXITCODE -ne 0) { throw "git add failed" }
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git -c "user.name=$Account" -c "user.email=$Account@users.noreply.github.com" commit -m "Initial Kabox Android project"
    if ($LASTEXITCODE -ne 0) { throw "git commit failed" }
}
git branch -M main
if ($LASTEXITCODE -ne 0) { throw "Could not set main branch" }

gh repo view "$Account/$RepoName" *> $null
if ($LASTEXITCODE -eq 0) {
    $ExistingRemote = git remote get-url origin 2>$null
    if ($LASTEXITCODE -eq 0) {
        git remote set-url origin $RemoteUrl
    } else {
        git remote add origin $RemoteUrl
    }
    git push -u origin main
    if ($LASTEXITCODE -ne 0) { throw "git push failed" }
} else {
    gh repo create $RepoName "--$RepoVisibility" --source=. --remote=origin --push
    if ($LASTEXITCODE -ne 0) { throw "GitHub repository creation or push failed" }
}

Write-Host "Done. Repository: $RemoteUrl"
