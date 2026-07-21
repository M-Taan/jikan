(ns jikan.vendor.mcp.impl.core
  "Transport for a tools-only MCP server over stdio.  Owns the JSON-RPC 2.0
   framing (newline-delimited JSON on stdin/stdout, all logging to stderr so
   stdout stays the protocol channel) and the method dispatch for
   `initialize`, `notifications/initialized`, `tools/list`, and `tools/call`.
   Knows nothing about org-mode: it dispatches against a tool registry
   (name -> {:description :input-schema :handler}) supplied by callers.  The
   exposed fns here are called from jikan.vendor.mcp.intf.core."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [jikan.vendor.mcp.impl.constants :as c]))

(defn- result-response [id result]
  {:jsonrpc "2.0" :id id :result result})

(defn- error-response [id code message]
  {:jsonrpc "2.0" :id id :error {:code code :message message}})

(defn- text-result [text error?]
  (cond-> {:content [{:type "text" :text (str text)}]}
    error? (assoc :isError true)))

(defn- tool-schemas [registry]
  (mapv (fn [[name {:keys [description input-schema]}]]
          {:name name
           :description description
           :inputSchema input-schema})
        registry))

(defn- call-tool [id registry {:keys [name arguments]}]
  (if-let [handler (:handler (get registry name))]
    (try
      (result-response id (text-result (handler (or arguments {})) false))
      (catch clojure.lang.ExceptionInfo e
        (result-response id (text-result (.getMessage e) true)))
      (catch Exception e
        (result-response id (text-result (or (.getMessage e) (str e)) true))))
    (error-response id c/invalid-params (str "Unknown tool: " name))))

(defn handle-message [{:keys [id method params]} registry server-info]
  (case method
    "initialize"
    (result-response id {:protocolVersion (or (:protocolVersion params)
                                              c/protocol-version)
                         :capabilities {:tools {}}
                         :serverInfo server-info})

    "notifications/initialized"
    nil

    "tools/list"
    (result-response id {:tools (tool-schemas registry)})

    "tools/call"
    (call-tool id registry params)

    (when id
      (error-response id c/method-not-found (str "Method not found: " method)))))

(defn- log! [& args]
  (binding [*out* *err*]
    (apply println "jikan-mcp:" args)
    (flush)))

(defn serve! [registry server-info]
  (let [in (io/reader System/in)]
    (loop []
      (when-let [line (.readLine ^java.io.BufferedReader in)]
        (let [resp (try
                     (handle-message (json/read-str line :key-fn keyword)
                                     registry server-info)
                     (catch Exception e
                       (log! "dropping unparseable message:" (.getMessage e))
                       nil))]
          (when resp
            (print (str (json/write-str resp) "\n"))
            (flush)))
        (recur)))))
