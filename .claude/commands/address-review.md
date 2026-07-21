---
description: Pull review feedback on the current branch's PR and push fixes to it
---

Address the review feedback on the pull request for the current branch.

Steps:

1. Fetch the unresolved feedback:

   ```
   ./bin/pr-feedback.sh -u
   ```

   This prints open review threads (with file:line), review verdicts, and the PR
   conversation. If it reports no open threads and no change requests, tell the
   user there's nothing to address and stop.

2. For each actionable comment, make the code change in the working tree. Group
   related comments. If a comment is a question rather than a change request,
   note it for a reply instead of editing.

3. Verify the changes still build/pass however this repo is normally checked.

4. Commit and push to the **same** branch so the PR updates in place — do NOT
   open a new PR:

   ```
   git add -A
   git commit -m "Address review feedback"
   git push
   ```

5. Optionally leave a short summary of what you changed:

   ```
   gh pr comment --body "<what was addressed>"
   ```

6. Report back to the user: what you fixed, anything you deliberately skipped
   (with why), and any questions you're bouncing back for their next review.

Extra hints from the user: $ARGUMENTS

If `pr-feedback.sh` errors about `gh` auth or a missing PR, relay the exact fix
it suggests rather than working around it.
