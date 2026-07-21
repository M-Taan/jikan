(ns jikan.org-tools
  "The five org operations, driven from the REPL.  All real work happens
   in the Emacs daemon (elisp/jikan.el); this namespace resolves
   area/project to paths, then delegates to the emacs port
   (jikan.vendor.emacs)."
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [jikan.config :as config]
            [jikan.vendor.emacs.impl :as emacs-impl]
            [jikan.vendor.emacs.intf :as emacs]))

;; --- private helpers ---

(def ^:private areas #{"work" "personal"})

(s/fdef resolve-path
  :args (s/cat :cfg ::config/config :area string? :project string?)
  :ret string?)

(defn- resolve-path
  "Resolve AREA + PROJECT to an org file path under :org-root.
   Throws on unknown area or on `/`/`..` in PROJECT (no path traversal)."
  [cfg area project]
  (when-not (areas area)
    (throw (ex-info (str "Unknown area " (pr-str area)
                         "; expected \"work\" or \"personal\"")
                    {:area area})))
  (when (or (.contains ^String project "/")
            (.contains ^String project ".."))
    (throw (ex-info (str "Invalid project name " (pr-str project)
                         "; must not contain / or ..")
                    {:project project})))
  (str (:org-root cfg) "/" area "/" project ".org"))

(defn- daemon
  "The emacs port for CFG (a fresh emacsclient-backed adapter)."
  [cfg]
  (emacs-impl/make (:emacsclient cfg)))

(defn- group-by-area-project
  "Flat [{:area :project :headline :state}] -> area -> project -> todos."
  [entries]
  (reduce
   (fn [acc {:keys [area project headline state]}]
     (update-in acc [area project] (fnil conj [])
                {:headline headline :state state}))
   {}
   entries))

;; --- public API ---

(defn ensure-file
  "Create the area/project org file (and a * Tasks section) if missing."
  [area project]
  (let [cfg (config/load-config)]
    (emacs/ensure-file (daemon cfg) (resolve-path cfg area project))))

(defn add-todo
  "Append \"** TODO HEADLINE\" under * Tasks in the area/project file."
  [area project headline]
  (let [cfg (config/load-config)]
    (emacs/add-todo (daemon cfg) (resolve-path cfg area project) headline)))

(defn mark-done
  "Mark HEADLINE as DONE.  Throws unless exactly one entry matches."
  [area project headline]
  (let [cfg (config/load-config)]
    (emacs/mark-done (daemon cfg) (resolve-path cfg area project) headline)))

(defn clock-in
  "Clock in on HEADLINE.  Throws unless exactly one entry matches."
  [area project headline]
  (let [cfg (config/load-config)]
    (emacs/clock-in (daemon cfg) (resolve-path cfg area project) headline)))

(defn clock-out
  "Clock out of the running clock (any file).  Returns \"clocked-out\"
   or \"no-clock\"."
  []
  (let [cfg (config/load-config)]
    (emacs/clock-out (daemon cfg))))

(defn list-todos
  "All TODO/DONE entries under <org-root>/{work,personal}/*.org, grouped
   area -> project -> [{:headline ... :state ...}]."
  []
  (let [cfg (config/load-config)
        tmp (java.io.File/createTempFile "jikan-todos" ".json")
        out-path (.getAbsolutePath tmp)]
    (try
      (emacs/list-todos (daemon cfg) (:org-root cfg) out-path)
      (-> (slurp out-path)
          (json/read-str :key-fn keyword)
          group-by-area-project)
      (finally
        (.delete tmp)))))
