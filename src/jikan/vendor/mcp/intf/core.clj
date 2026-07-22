(ns jikan.vendor.mcp.intf.core
  "The MCP port: a tools-only Model Context Protocol server over stdio,
   generic over any tool registry.  Delegates to the stdio/JSON-RPC
   implementation in jikan.vendor.mcp.impl.core."
  (:require [jikan.vendor.mcp.impl.core :as impl]))

(defn serve!
  "Run the MCP stdio JSON-RPC loop over REGISTRY (name ->
   {:description :input-schema :handler}, handler an arguments-map -> text
   fn), advertising SERVER-INFO {:name :version}.  Blocks until stdin closes."
  [registry server-info]
  (impl/serve! registry server-info))

(defn handle-message
  "Pure dispatch for one MCP request: keyword-keyed MSG + REGISTRY +
   SERVER-INFO -> a response map, or nil for notifications."
  [msg registry server-info]
  (impl/handle-message msg registry server-info))
