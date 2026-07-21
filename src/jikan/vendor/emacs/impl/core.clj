(ns jikan.vendor.emacs.impl.core
  "emacsclient implementation of the emacs port.  Owns all transport
   concerns: shelling out to `emacsclient --eval`, escaping arguments into
   elisp string literals, and stripping the prin1 quoting emacsclient adds
   around string results.  The exposed fns here are called from
   jikan.vendor.emacs.intf.core."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [jikan.vendor.emacs.impl.constants :as c]))

(defn- elisp-str [s]
  (-> s
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")))

(defn- strip-prin1 [s]
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

(defn- eval! [emacsclient form]
  (let [{:keys [exit out err]} (sh/sh emacsclient "--eval" form)]
    (when (or (not (zero? exit))
              (str/includes? out "*ERROR*")
              (str/includes? err "*ERROR*"))
      (throw (ex-info (str "emacsclient --eval failed: "
                           (str/trim (if (str/blank? err) out err)))
                      {:exit exit :out out :err err :form form})))
    (strip-prin1 (str/trim out))))

(defn- rpc [emacsclient fn-name & args]
  (let [form (str "(" fn-name
                  (apply str (map #(str " \"" (elisp-str %) "\"") args))
                  ")")]
    (eval! emacsclient form)))

(defn load-elisp [emacsclient elisp-file]
  (let [path (.getAbsolutePath (io/file elisp-file))]
    (eval! emacsclient (format "(load \"%s\")" (elisp-str path)))))

(defn ensure-file [emacsclient path]
  (rpc emacsclient c/ensure-file path))

(defn add-todo [emacsclient path headline]
  (rpc emacsclient c/add-todo path headline))

(defn mark-done [emacsclient path headline]
  (rpc emacsclient c/mark-done path headline))

(defn clock-in [emacsclient path headline]
  (rpc emacsclient c/clock-in path headline))

(defn clock-out [emacsclient]
  (rpc emacsclient c/clock-out))

(defn list-todos [emacsclient org-root out-path]
  (rpc emacsclient c/list-todos org-root out-path))
