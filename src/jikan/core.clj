(ns jikan.core
  "Entry point for the Jikan MCP stdio server.  Loads elisp/jikan.el into
   the running Emacs daemon (so it has the current definitions, like the
   REPL's init!), then serves jikan.org-tools as MCP tools over stdio."
  (:require [jikan.config :as config]
            [jikan.mcp-tools :as mcp-tools]
            [jikan.vendor.emacs.intf.core :as emacs]
            [jikan.vendor.mcp.intf.core :as mcp]))

(def ^:private server-info {:name "jikan" :version "0.1.0"})

(defn -main [& _]
  (emacs/load-elisp (:emacsclient (config/load-config)) "elisp/jikan.el")
  (mcp/serve! (mcp-tools/registry) server-info))
