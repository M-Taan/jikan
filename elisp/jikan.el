;;; jikan.el --- Jikan org-mode RPC surface -*- lexical-binding: t; -*-

;; Org operations exposed to Jikan callers (the Clojure REPL today, the
;; assistant layer later).  Real org-mode is the single org
;; implementation; every function here delegates to it.
;;
;; Conventions:
;; - All public functions take only string arguments.
;; - Buffers are opened with `find-file-noselect' and saved after every
;;   mutation.
;; - Writes go through `jikan--find-exact', which signals an error on
;;   zero or multiple headline matches: elisp never writes on a guess.
;;   Fuzzy matching of user phrasing is the caller's job.

;;; Code:

(require 'org)
(require 'org-clock)
(require 'json)
(require 'seq)

(defun jikan--goto-tasks-heading ()
  "Move point to the \"* Tasks\" top-level heading in the current buffer,
creating it at end of buffer if missing.  Return the heading position."
  (goto-char (point-min))
  (if (re-search-forward "^\\* Tasks[ \t]*$" nil t)
      (progn
        (beginning-of-line)
        (point))
    (goto-char (point-max))
    (unless (or (= (point) (point-min)) (bolp))
      (insert "\n"))
    (insert "* Tasks\n")
    (forward-line -1)
    (point)))

(defun jikan-ensure-file (path)
  "Create PATH (and parent directories) if missing; ensure a \"* Tasks\"
heading exists.  Return \"ok\"."
  (let ((dir (file-name-directory path)))
    (unless (file-directory-p dir)
      (make-directory dir t)))
  (with-current-buffer (find-file-noselect path)
    (jikan--goto-tasks-heading)
    (save-buffer)
    "ok"))

(defun jikan-add-todo (path headline)
  "Append \"** TODO HEADLINE\" under the \"* Tasks\" heading in PATH,
creating the file and section if missing.  Return \"added\"."
  (jikan-ensure-file path)
  (with-current-buffer (find-file-noselect path)
    (goto-char (jikan--goto-tasks-heading))
    (org-end-of-subtree t)
    (unless (or (= (point) (point-min)) (bolp))
      (insert "\n"))
    (insert "** TODO " headline "\n")
    (save-buffer)
    "added"))

(defun jikan--find-exact (path headline)
  "Return the buffer position of the single entry in PATH whose heading
equals HEADLINE exactly (tags, TODO keyword, priority and comment
markers excluded).  Signal an error on zero or multiple matches."
  (with-current-buffer (find-file-noselect path)
    (let ((matches nil))
      (org-map-entries
       (lambda ()
         (when (equal (org-get-heading t t t t) headline)
           (push (point) matches)))
       nil 'file)
      (cond
       ((null matches)
        (error "jikan: no headline %S in %s" headline path))
       ((cdr matches)
        (error "jikan: %d headlines %S in %s; refusing to pick one"
               (length matches) headline path))
       (t (car matches))))))

(defun jikan-mark-done (path headline)
  "Mark the entry with exact HEADLINE in PATH as DONE.  Return \"done\"."
  (with-current-buffer (find-file-noselect path)
    (goto-char (jikan--find-exact path headline))
    (org-todo "DONE")
    (save-buffer)
    "done"))

(defun jikan-clock-in (path headline)
  "Clock in on the entry with exact HEADLINE in PATH.
Return \"clocked-in\"."
  (with-current-buffer (find-file-noselect path)
    (goto-char (jikan--find-exact path headline))
    (org-clock-in)
    (save-buffer)
    "clocked-in"))

(defun jikan-clock-out ()
  "Clock out of the currently running clock, whatever file it is in.
Return \"clocked-out\", or \"no-clock\" if nothing is clocked in."
  (if (org-clocking-p)
      (progn
        (with-current-buffer (marker-buffer org-clock-marker)
          (goto-char org-clock-marker)
          (org-clock-out)
          (save-buffer))
        "clocked-out")
    "no-clock"))

(defun jikan-list-todos (root out-file)
  "Scan <ROOT>/work/*.org and <ROOT>/personal/*.org for TODO/DONE
entries.  Write a JSON array of {area, project, headline, state} to
OUT-FILE.  Return \"ok\"."
  (let ((entries nil))
    (dolist (area '("work" "personal"))
      (let ((dir (expand-file-name area root)))
        (when (file-directory-p dir)
          (dolist (file (seq-filter #'file-regular-p
                                    (directory-files dir t "\\.org\\'")))
            (with-current-buffer (find-file-noselect file)
              (org-map-entries
               (lambda ()
                 (let ((state (org-get-todo-state)))
                   (when (member state '("TODO" "DONE"))
                     (push (list (cons 'area area)
                                 (cons 'project (file-name-base file))
                                 (cons 'headline (org-get-heading t t t t))
                                 (cons 'state state))
                           entries))))
               nil 'file))))))
    (let ((coding-system-for-write 'utf-8-unix))
      (with-temp-file out-file
        (insert (json-serialize (vconcat (nreverse entries))))))
    "ok"))

(provide 'jikan)
;;; jikan.el ends here
