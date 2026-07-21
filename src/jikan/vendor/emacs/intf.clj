(ns jikan.vendor.emacs.intf
  "The port to a running Emacs daemon: the org operations Jikan drives,
   expressed as file paths (area/project resolution belongs to callers).
   The emacsclient adapter lives in jikan.vendor.emacs.impl.")

(defprotocol EmacsDaemon
  (load-elisp [this elisp-file]
    "Load ELISP-FILE into the daemon so it has the current definitions.")
  (ensure-file [this path]
    "Create the org file at PATH (with a * Tasks section) if missing.")
  (add-todo [this path headline]
    "Append \"** TODO HEADLINE\" under * Tasks in the file at PATH.")
  (mark-done [this path headline]
    "Mark HEADLINE as DONE.  Errors unless exactly one entry matches.")
  (clock-in [this path headline]
    "Clock in on HEADLINE.  Errors unless exactly one entry matches.")
  (clock-out [this]
    "Clock out of the running clock.  Returns \"clocked-out\" or \"no-clock\".")
  (list-todos [this org-root out-path]
    "Write all TODO/DONE entries under ORG-ROOT to OUT-PATH as JSON."))
