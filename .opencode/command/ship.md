---
description: Commit the current work to a new branch and open a GitHub PR for review
---

Ship the current changes as a pull request for the user to review.

Current status:

!`git status --short`

Steps:

1. Review the diff above (and run `git diff` if you need more detail) to understand what changed.
2. Compose a concise imperative commit message, a clear PR title, and a PR body
   summarizing what changed and why.
3. Run the pipeline script, quoting each argument:

   `./bin/ship.sh -t "<PR title>" -m "<commit message>" -b "<PR body>"`

   Extra hints from the user: $ARGUMENTS
   (e.g. `-B <branch-name>` to name the branch, or `-d` for a draft PR.)

   The script creates a feature branch (or reuses the current one), commits,
   pushes to `origin`, and opens the PR against the repo's default branch.
4. Report the PR URL the script prints back to the user.

If the script errors about `gh` auth or a missing `origin` remote, relay the
exact fix it suggests.
