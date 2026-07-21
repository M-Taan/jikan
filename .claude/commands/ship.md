---
description: Commit the current work to a new branch and open a GitHub PR for review
---

Ship the current changes as a pull request for the user to review.

Steps:

1. Run `git status` and `git diff` (and `git diff --cached`) to understand what changed.
2. Compose:
   - a concise, imperative **commit message** (subject + body if useful),
   - a clear **PR title**, and
   - a **PR body** summarizing what changed and why, plus any test/verification notes.
3. Run the pipeline script, quoting each argument:

   ```
   ./bin/ship.sh -t "<PR title>" -m "<commit message>" -b "<PR body>"
   ```

   - Pass `$ARGUMENTS` through as extra hints if the user provided any (e.g. a branch name via `-B`, or `-d` for a draft PR).
   - The script creates a feature branch (or reuses the current one), commits, pushes to `origin`, and opens the PR against the repo's default branch.
4. Report the PR URL that the script prints back to the user.

If the script exits with an error about `gh` auth or a missing `origin` remote, relay the exact fix it suggests instead of trying to work around it.
