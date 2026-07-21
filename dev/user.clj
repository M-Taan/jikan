(ns user
  "REPL conveniences for Jikan slice 1."
  (:require [jikan.config :as config]
            [jikan.org-tools :as tools]
            [jikan.vendor.emacs.intf.core :as emacs]))

(defn init!
  "Load elisp/jikan.el into the running Emacs daemon, so the daemon
   always has the current definitions."
  []
  (emacs/load-elisp (:emacsclient (config/load-config)) "elisp/jikan.el"))

(defn ensure-file [area project] (tools/ensure-file area project))
(defn add-todo [area project headline] (tools/add-todo area project headline))
(defn mark-done [area project headline] (tools/mark-done area project headline))
(defn clock-in [area project headline] (tools/clock-in area project headline))
(defn clock-out [] (tools/clock-out))
(defn list-todos [] (tools/list-todos))

(comment
  (init!)
  (add-todo "work" "acme" "Write proposal")
  (list-todos)
  (mark-done "work" "acme" "Write proposal")
  (clock-in "work" "acme" "Write proposal")
  (clock-out))
