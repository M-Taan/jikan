# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Jikan drives a running Emacs daemon's org-mode from a Clojure REPL, so org files stay the single
source of truth for tasks and clocking. There is no database and no reimplementation of org
semantics in Clojure — every mutation happens in Emacs via `org-*` functions.

## Commands

- REPL: `clj -A:dev` (loads `dev/user.clj`, which requires `jikan.config`/`jikan.emacs`/`jikan.org-tools`).
- After editing `elisp/jikan.el`, reload it into the running daemon from the REPL: `(init!)` — this
  calls `emacs/load-elisp!`, which does `(load "<absolute-path>")` in the daemon. There is no
  separate elisp test runner; verify by calling the functions through the REPL wrappers in
  `dev/user.clj` (`add-todo`, `list-todos`, `mark-done`, `clock-in`, `clock-out`) and inspecting the
  actual `.org` files under `:org-root`.
- No build/lint/test tooling is configured yet (no `clojure -X:test`, no linter alias in `deps.edn`).
- `bin/ship.sh -t "<title>"` commits, pushes, and opens a PR (see `.claude/commands/ship.md` for the
  `/ship` flow driving it).

## Architecture

Three-layer pipeline, each layer doing exactly one job:

1. **`elisp/jikan.el`** — the only code that touches org-mode APIs. Public functions take only
   string arguments and return plain strings (or write JSON to a file path for `jikan-list-todos`).
   Loaded into a persistent Emacs daemon via `emacsclient`, so state (open buffers, the org clock)
   persists across calls.
2. **`src/jikan/emacs.clj`** — transport only. `eval!` shells out to `emacsclient --eval` and strips
   the `prin1` quoting Emacs adds around string results; throws `ex-info` on nonzero exit or
   `*ERROR*` in stdout/stderr.
3. **`src/jikan/org_tools.clj`** — the five org operations (`ensure-file`, `add-todo`, `mark-done`,
   `clock-in`, `clock-out`, plus `list-todos`). Resolves `area`/`project` to a file path under
   `:org-root`, builds the elisp call form via `rpc`, and parses results. `resolve-path` is the
   security boundary: it rejects unknown areas and `/`/`..` in `project` to prevent path traversal
   into `:org-root`.

Config (`config.edn` at repo root — `:org-root`, `:emacsclient`) is read fresh on every call via
`jikan.config/load-config`, so flipping the config file takes effect without restarting the REPL or
daemon.

### Conventions carried over from `elisp/jikan.el`'s own header comment

- Every mutation goes through `find-file-noselect` and calls `save-buffer` afterward.
- Writes that target a specific headline (`mark-done`, `clock-in`) go through
  `jikan--find-exact`, which errors on zero or multiple matches — elisp never guesses which
  headline to act on. Fuzzy matching of user phrasing belongs in a caller layer, not here.
- Areas are hardcoded to `"work"` and `"personal"` (`jikan.org-tools/areas`); org files live at
  `<org-root>/<area>/<project>.org` under a top-level `* Tasks` heading.
