# AGENTS.md

## What this is

Jikan drives a running Emacs daemon's org-mode from a Clojure REPL: Clojure builds elisp call
forms, `emacsclient --eval` runs them in the daemon, and org files stay the single source of
truth. No database, no org semantics reimplemented in Clojure. Slice 1 only — REPL + elisp, no
assistant layer yet.

`CLAUDE.md` (repo root) documents the same three-layer architecture in detail. Keep the two files
consistent when the architecture changes.

## Run & verify

- REPL: `clj -A:dev` — loads `dev/user.clj`, which wraps the operations (`ensure-file`,
  `add-todo`, `list-todos`, `mark-done`, `clock-in`, `clock-out`).
- After editing `elisp/jikan.el` you MUST run `(init!)` in the REPL. It `(load ...)`s the file
  into the running daemon; without it the daemon keeps the old definitions and your edit silently
  has no effect.
- There is no test, lint, or build tooling (`deps.edn` has only the `:dev` alias). Don't hunt for
  a test runner — verify by calling the REPL wrappers and inspecting the resulting `.org` files.

## Operational gotchas

- Everything requires a running Emacs daemon reachable via `emacsclient`; without it every call
  fails in `jikan.emacs/eval!`. Check `emacsclient --eval t` before debugging Clojure code.
- Calls mutate the user's real org files under `:org-root` (`~/Dropbox/org` in `config.edn` —
  outside this repo), and clocking changes live daemon state. Verify with a scratch project
  (e.g. `"personal" "jikan-test"`) and delete entries you create.
- The daemon is stateful across calls (open buffers, the running clock). `clock-out` returning
  `"no-clock"` is a normal result, not an error.
- `config.edn` is re-read on every call — edits apply immediately, no REPL restart.

## Conventions when extending

- Public elisp functions take only string arguments and return plain strings; `jikan-list-todos`
  is the exception — it writes JSON to a file path instead of returning data.
- Headline-targeted writes go through `jikan--find-exact`, which errors on zero or multiple
  matches. Never add fuzzy matching to the elisp layer; that belongs in callers.
- Areas are hardcoded to `{"work" "personal"}` in `jikan.org-tools/areas`, and `resolve-path`
  rejects `/`/`..` in project names — that pair is the path-traversal boundary, don't bypass it.
- New Clojure-side operations follow the `rpc` pattern in `org_tools.clj`: build a
  `(jikan-fn "arg" ...)` form and escape every argument with `emacs/elisp-str`.

## Housekeeping

- `.opencode/` is OpenCode's own config (the `/ship` command plus plugin `node_modules/`) and
  `.cpcache/` is Clojure's cache — neither is project code; exclude them from searches.
- To commit/push/open a PR, use the `/ship` flow (`.opencode/command/ship.md`), which drives
  `bin/ship.sh` — not raw git commands.
