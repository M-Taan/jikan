(ns jikan.emacs
  "Transport to the running Emacs daemon via emacsclient --eval."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn elisp-str
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

(defn eval!
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

(defn load-elisp!
  "Load ELISP-FILE (e.g. elisp/jikan.el) into the running daemon, so the
   daemon always has the current definitions."
  [emacsclient elisp-file]
  (let [path (.getAbsolutePath (io/file elisp-file))]
    (eval! emacsclient (format "(load \"%s\")" (elisp-str path)))))
