#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
EXPECTED_OWNER="${1:-winhok}"
REPOSITORY="${2:-earth-online-android}"
[[ "$REPOSITORY" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "Invalid repository name." >&2; exit 1; }
for tool in git gh; do command -v "$tool" >/dev/null || { echo "Install $tool first." >&2; exit 1; }; done
gh auth status --hostname github.com
LOGIN="$(gh api user --jq .login)"
[[ "$LOGIN" == "$EXPECTED_OWNER" ]] || { echo "Expected $EXPECTED_OWNER, authenticated as $LOGIN. Aborting." >&2; exit 1; }
FULL="$LOGIN/$REPOSITORY"
if gh repo view "$FULL" --json name >/dev/null 2>&1; then echo "$FULL already exists; no changes made." >&2; exit 1; fi
[[ ! -e .git ]] || { echo "Local .git exists. Review and publish manually; no overwrite attempted." >&2; exit 1; }
sh scripts/bootstrap-wrapper.sh
git init -b main
git config user.name "$LOGIN"
ID="$(gh api user --jq .id)"
git config user.email "${ID}+${LOGIN}@users.noreply.github.com"
git add .
git commit -m "feat: Earth Online Android 1.0 release candidate"
gh repo create "$FULL" --private --source . --remote origin --push     --description "Offline-first Earth Online quest manager for Android, built with Kotlin and Jetpack Compose"
echo "Created private repository: https://github.com/$FULL"
echo "Check Actions separately. A successful push is not a passing Android build."
