(ns jikan.vendor.mcp.impl.constants
  "Protocol constants for the MCP stdio server: the default protocol
   version to advertise when a client sends none, and the JSON-RPC 2.0
   error codes.  Referenced from jikan.vendor.mcp.impl.core.")

(def protocol-version "2024-11-05")

(def parse-error -32700)
(def invalid-request -32600)
(def method-not-found -32601)
(def invalid-params -32602)
(def internal-error -32603)
