#!/usr/bin/env bash
#
# ship.sh — branch, commit, push, and open a GitHub PR for review.
#
# Designed to be driven by an agent (Claude Code or opencode) that supplies a
# good commit message and PR title/body, but also works standalone.
#
# Usage:
#   bin/ship.sh -t "PR title" [options]
#
# Options:
#   -t <title>    PR title (required). Also used as the commit subject if -m is omitted.
#   -m <msg>      Commit message. Defaults to the PR title.
#   -b <body>     PR body / description. Defaults to a generated summary.
#   -B <branch>   Feature branch name. Defaults to a slug derived from the title.
#   -l <base>     Base branch to open the PR against. Defaults to the remote's
#                 default branch (falls back to "main").
#   -d            Draft PR.
#   -n            Dry run: print what would happen, change nothing.
#   -h            Show this help.
#
set -euo pipefail

die() { printf 'ship: %s\n' "$1" >&2; exit 1; }

TITLE="" MSG="" BODY="" BRANCH="" BASE="" DRAFT="" DRY=""

while getopts ":t:m:b:B:l:dnh" opt; do
  case "$opt" in
    t) TITLE="$OPTARG" ;;
    m) MSG="$OPTARG" ;;
    b) BODY="$OPTARG" ;;
    B) BRANCH="$OPTARG" ;;
    l) BASE="$OPTARG" ;;
    d) DRAFT="--draft" ;;
    n) DRY="1" ;;
    h) sed -n '2,/^set -/{/^set -/d;s/^# \{0,1\}//p}' "$0"; exit 0 ;;
    :) die "option -$OPTARG requires an argument" ;;
    \?) die "unknown option -$OPTARG" ;;
  esac
done

run() { if [[ -n "$DRY" ]]; then printf '+ %s\n' "$*"; else "$@"; fi; }

# --- Preconditions -----------------------------------------------------------
command -v git >/dev/null || die "git not found"
command -v gh  >/dev/null || die "gh (GitHub CLI) not found — install it: https://cli.github.com"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "not inside a git repository"
gh auth status >/dev/null 2>&1 || die "gh is not authenticated — run: gh auth login"
git remote get-url origin >/dev/null 2>&1 || die "no 'origin' remote — add one, e.g.: gh repo create --source=. --private --remote=origin"

[[ -n "$TITLE" ]] || die "PR title is required (-t)"
[[ -n "$MSG"  ]] || MSG="$TITLE"
[[ -n "$BODY" ]] || BODY="$MSG"

# Anything to ship?
if git diff --quiet && git diff --cached --quiet && [[ -z "$(git status --porcelain)" ]]; then
  die "working tree is clean — nothing to commit"
fi

# --- Base branch -------------------------------------------------------------
if [[ -z "$BASE" ]]; then
  BASE="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's@^origin/@@')" || true
  [[ -n "$BASE" ]] || BASE="main"
fi

# --- Feature branch ----------------------------------------------------------
CURRENT="$(git rev-parse --abbrev-ref HEAD)"
if [[ -z "$BRANCH" ]]; then
  if [[ "$CURRENT" != "$BASE" && "$CURRENT" != "master" ]]; then
    # Already on a feature branch — reuse it.
    BRANCH="$CURRENT"
  else
    slug="$(printf '%s' "$TITLE" | tr '[:upper:]' '[:lower:]' \
            | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//' | cut -c1-40)"
    [[ -n "$slug" ]] || slug="change"
    BRANCH="feat/${slug}-$(date +%Y%m%d-%H%M%S)"
  fi
fi

if [[ "$BRANCH" != "$CURRENT" ]]; then
  run git checkout -b "$BRANCH"
fi

# --- Commit ------------------------------------------------------------------
run git add -A
run git commit -m "$MSG"

# --- Push --------------------------------------------------------------------
run git push -u origin "$BRANCH"

# --- PR ----------------------------------------------------------------------
if [[ -n "$DRY" ]]; then
  printf '+ gh pr create --base %s --head %s --title %q --body <body> %s\n' \
    "$BASE" "$BRANCH" "$TITLE" "$DRAFT"
else
  gh pr create --base "$BASE" --head "$BRANCH" \
    --title "$TITLE" --body "$BODY" $DRAFT
  gh pr view --web >/dev/null 2>&1 || true
fi
