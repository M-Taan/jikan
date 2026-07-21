(ns jikan.vendor.emacs.impl.constants
  "Names of the elisp functions defined in elisp/jikan.el.  Kept here so
   callers reference a constant instead of a magic string, and so the
   Clojure/elisp contract lives in one place.")

(def ensure-file "jikan-ensure-file")
(def add-todo    "jikan-add-todo")
(def mark-done   "jikan-mark-done")
(def clock-in    "jikan-clock-in")
(def clock-out   "jikan-clock-out")
(def list-todos  "jikan-list-todos")
