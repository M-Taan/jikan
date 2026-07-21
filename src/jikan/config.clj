(ns jikan.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::org-root string?)
(s/def ::emacsclient string?)
(s/def ::config (s/keys :req-un [::org-root ::emacsclient]))

(defn- expand-home
  "Expand a leading ~ to the user's home directory."
  [s]
  (if (and (string? s) (.startsWith ^String s "~"))
    (str (System/getProperty "user.home") (subs s 1))
    s))

(defn load-config
  "Read config.edn from the project root, expanding ~ in :org-root.
   Read fresh on every call so config flips take effect without a restart."
  []
  (let [cfg (-> (slurp (io/file "config.edn"))
                edn/read-string
                (update :org-root expand-home))]
    (when-not (s/valid? ::config cfg)
      (throw (ex-info (str "Invalid config.edn: " (s/explain-str ::config cfg))
                      {:config cfg})))
    cfg))
