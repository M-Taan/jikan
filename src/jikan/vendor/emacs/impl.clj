(ns jikan.vendor.emacs.impl
  "emacsclient adapter for jikan.vendor.emacs.intf/EmacsDaemon.  Owns all
   transport concerns: shelling out to `emacsclient --eval`, escaping
   arguments into elisp string literals, and stripping the prin1 quoting
   emacsclient adds around string results."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [jikan.vendor.emacs.constants :as c]
            [jikan.vendor.emacs.intf :as intf]))

(defn- elisp-str
  "Escape S for safe embedding as an elisp string literal."
  [s]
  (-> s
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")))

(defn- strip-prin1
  "Strip the prin1 quoting emacsclient adds around string results:
   \"added\" -> added.  Non-string results (t, numbers) pass through."
  [s]
  (if (and (>= (count s) 2)
           (str/starts-with? s "\"")
           (str/ends-with? s "\""))
    (-> (subs s 1 (dec (count s)))
        (str/replace "\\\\" "\u0000")
        (str/replace "\\\"" "\"")
        (str/replace "\\n" "\n")
        (str/replace "\\t" "\t")
        (str/replace "\\r" "\r")
        (str/replace "\u0000" "\\"))
    s))

(defn- eval!
  "Evaluate FORM (an elisp source string) in the running Emacs daemon.
   Returns the daemon's result as a plain string (prin1 quoting stripped).
   Throws ex-info on non-zero exit or *ERROR* output."
  [emacsclient form]
  (let [{:keys [exit out err]} (sh/sh emacsclient "--eval" form)]
    (when (or (not (zero? exit))
              (str/includes? out "*ERROR*")
              (str/includes? err "*ERROR*"))
      (throw (ex-info (str "emacsclient --eval failed: "
                           (str/trim (if (str/blank? err) out err)))
                      {:exit exit :out out :err err :form form})))
    (strip-prin1 (str/trim out))))

(defn- rpc
  "Evaluate (FN-NAME \"arg\" ...) in the daemon; return its result string."
  [emacsclient fn-name & args]
  (let [form (str "(" fn-name
                  (apply str (map #(str " \"" (elisp-str %) "\"") args))
                  ")")]
    (eval! emacsclient form)))

(defrecord EmacsclientDaemon [emacsclient]
  intf/EmacsDaemon
  (load-elisp [_ elisp-file]
    (let [path (.getAbsolutePath (io/file elisp-file))]
      (eval! emacsclient (format "(load \"%s\")" (elisp-str path)))))
  (ensure-file [_ path]
    (rpc emacsclient c/ensure-file path))
  (add-todo [_ path headline]
    (rpc emacsclient c/add-todo path headline))
  (mark-done [_ path headline]
    (rpc emacsclient c/mark-done path headline))
  (clock-in [_ path headline]
    (rpc emacsclient c/clock-in path headline))
  (clock-out [_]
    (rpc emacsclient c/clock-out))
  (list-todos [_ org-root out-path]
    (rpc emacsclient c/list-todos org-root out-path)))

(defn make
  "Build an EmacsDaemon backed by the given EMACSCLIENT executable."
  [emacsclient]
  (->EmacsclientDaemon emacsclient))
