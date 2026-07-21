#!/usr/bin/env bash
#
# pr-feedback.sh — print review feedback for the PR of the current branch.
#
# Read-only. Shows unresolved inline review threads, review verdicts, and the
# PR conversation so an agent (or you) can act on them.
#
# Usage:
#   bin/pr-feedback.sh [options]
#
# Options:
#   -n <num>   PR number. Defaults to the PR for the current branch.
#   -u         Only show UNRESOLVED review threads (hide resolved/addressed ones).
#   -j         Emit raw JSON instead of the formatted digest.
#   -h         Show this help.
#
set -euo pipefail
die(){ printf 'pr-feedback: %s\n' "$1" >&2; exit 1; }

NUM="" JSON="" UNRESOLVED_ONLY=""
while getopts ":n:juh" o; do case "$o" in
  n) NUM="$OPTARG";;
  j) JSON="1";;
  u) UNRESOLVED_ONLY="1";;
  h) sed -n '2,/^set -/{/^set -/d;s/^# \{0,1\}//p}' "$0"; exit 0;;
  :) die "option -$OPTARG requires an argument";;
  \?) die "unknown option -$OPTARG";;
esac; done

command -v git >/dev/null || die "git not found"
command -v gh  >/dev/null || die "gh not found — https://cli.github.com"
command -v jq  >/dev/null || die "jq not found"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "not inside a git repository"
gh auth status >/dev/null 2>&1 || die "gh not authenticated — run: gh auth login"

read -r OWNER REPO < <(gh repo view --json owner,name --jq '.owner.login+" "+.name') \
  || die "could not resolve repo from 'origin'"
[[ -n "${NUM}" ]] || NUM="$(gh pr view --json number --jq .number 2>/dev/null || true)"
[[ -n "${NUM}" ]] || die "no PR found for the current branch — open one first (e.g. /ship)"

data="$(gh api graphql -F owner="$OWNER" -F repo="$REPO" -F num="$NUM" -f query='
query($owner:String!,$repo:String!,$num:Int!){
  repository(owner:$owner,name:$repo){
    pullRequest(number:$num){
      url state reviewDecision
      reviews(first:50){nodes{author{login} state body submittedAt}}
      comments(first:50){nodes{author{login} body createdAt}}
      reviewThreads(first:100){nodes{isResolved isOutdated path line
        comments(first:30){nodes{author{login} body}}}}
    }
  }
}')"

if [[ -n "$JSON" ]]; then printf '%s\n' "$data" | jq '.data.repository.pullRequest'; exit 0; fi

printf '%s' "$data" | jq -r --arg num "$NUM" --arg only "$UNRESOLVED_ONLY" '
  .data.repository.pullRequest as $pr
  | "PR #\($num)  \($pr.state)  decision=\($pr.reviewDecision // "none")\n\($pr.url)\n"
  + "\n== Review threads ==\n"
  + ( [ $pr.reviewThreads.nodes[] | select($only=="" or (.isResolved|not)) ]
      | if length==0 then "(none)" else
        map( (if .isResolved then "[resolved]" else "[OPEN]" end)
             + (if .isOutdated then "[outdated]" else "" end)
             + " \(.path):\(.line // "?")\n"
             + (.comments.nodes | map("    @\(.author.login): \(.body)") | join("\n"))
           ) | join("\n\n")
        end )
  + "\n\n== Reviews ==\n"
  + ( [ $pr.reviews.nodes[] | select((.body//"")!="" or .state!="COMMENTED") ]
      | if length==0 then "(none)" else
        map("@\(.author.login) [\(.state)]" + (if (.body//"")!="" then ": \(.body)" else "" end)) | join("\n")
        end )
  + "\n\n== Conversation ==\n"
  + ( $pr.comments.nodes
      | if length==0 then "(none)" else map("@\(.author.login): \(.body)") | join("\n\n") end )
'
