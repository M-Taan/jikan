#!/usr/bin/env bash
#
# mcp.sh — run Jikan's MCP stdio server for the Hermes agent.
#
# Speaks newline-delimited JSON-RPC 2.0 over stdin/stdout. Hermes spawns
# this as a subprocess (see ~/.hermes/config.yaml). cd's to the repo root
# first so the CWD-relative config.edn and elisp/jikan.el resolve no matter
# where Hermes launches it.
#
set -euo pipefail
cd "$(dirname "$0")/.."
exec clojure -M:mcp
