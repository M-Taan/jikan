(ns jikan.vendor.emacs.intf.core
  "The emacs port: the org operations Jikan drives in a running Emacs
   daemon, expressed as plain functions over file paths (area/project
   resolution belongs to callers).  Delegates to the emacsclient
   implementation in jikan.vendor.emacs.impl.core."
  (:require [jikan.vendor.emacs.impl.core :as impl]))

(defn load-elisp
  "Load ELISP-FILE into the daemon so it has the current definitions."
  [emacsclient elisp-file]
  (impl/load-elisp emacsclient elisp-file))

(defn ensure-file
  "Create the org file at PATH (with a * Tasks section) if missing."
  [emacsclient path]
  (impl/ensure-file emacsclient path))

(defn add-todo
  "Append \"** TODO HEADLINE\" under * Tasks in the file at PATH."
  [emacsclient path headline]
  (impl/add-todo emacsclient path headline))

(defn mark-done
  "Mark HEADLINE as DONE.  Errors unless exactly one entry matches."
  [emacsclient path headline]
  (impl/mark-done emacsclient path headline))

(defn clock-in
  "Clock in on HEADLINE.  Errors unless exactly one entry matches."
  [emacsclient path headline]
  (impl/clock-in emacsclient path headline))

(defn clock-out
  "Clock out of the running clock.  Returns \"clocked-out\" or \"no-clock\"."
  [emacsclient]
  (impl/clock-out emacsclient))

(defn list-todos
  "Write all TODO/DONE entries under ORG-ROOT to OUT-PATH as JSON."
  [emacsclient org-root out-path]
  (impl/list-todos emacsclient org-root out-path))
