---
description: Pull review feedback on the current branch's PR and push fixes to it
---

Address the review feedback on the pull request for the current branch.

Unresolved feedback:

!`./bin/pr-feedback.sh -u`

Steps:

1. Read the feedback above. If it reports no open threads and no change
   requests, tell the user there's nothing to address and stop.
2. For each actionable comment, make the code change in the working tree. Group
   related comments. Treat questions as replies to send, not edits to make.
3. Verify the changes still build/pass however this repo is normally checked.
4. Commit and push to the **same** branch so the PR updates in place — do NOT
   open a new PR:

   `git add -A && git commit -m "Address review feedback" && git push`

5. Optionally summarize what changed: `gh pr comment --body "<summary>"`.
6. Report what you fixed, what you skipped and why, and any questions for the
   user's next review.

Extra hints from the user: $ARGUMENTS
