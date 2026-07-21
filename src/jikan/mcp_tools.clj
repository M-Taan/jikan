(ns jikan.mcp-tools
  "Application glue mapping Jikan's six org operations onto an MCP tool
   registry: each entry is {:description :input-schema :handler}, where the
   handler takes the parsed JSON arguments map and returns text.  Handlers
   delegate straight to jikan.org-tools and let its ex-info escape; the MCP
   server turns that into an isError result the calling LLM can recover from.
   Natural-language parsing is the LLM's job (the caller layer), so these
   tools stay structured and deterministic."
  (:require [clojure.data.json :as json]
            [jikan.org-tools :as tools]))

(def ^:private area-schema
  {:type "string"
   :enum ["work" "personal"]
   :description "Top-level area the project lives under."})

(def ^:private project-schema
  {:type "string"
   :description "Project name; the org file is <area>/<project>.org. No / or .."})

(def ^:private headline-schema
  {:type "string"
   :description "Exact task headline text, without the TODO/DONE keyword."})

(defn- object-schema [props required]
  {:type "object" :properties props :required required})

(defn registry
  "Build the MCP tool registry for Jikan's org operations."
  []
  {"add_todo"
   {:description "Append a new TODO with HEADLINE under the area/project's org file."
    :input-schema (object-schema {:area area-schema
                                  :project project-schema
                                  :headline headline-schema}
                                 ["area" "project" "headline"])
    :handler (fn [{:keys [area project headline]}]
               (tools/add-todo area project headline))}

   "mark_done"
   {:description (str "Mark an existing headline DONE. Requires an exact, unique "
                     "match: call list_todos first to get the real headline text.")
    :input-schema (object-schema {:area area-schema
                                  :project project-schema
                                  :headline headline-schema}
                                 ["area" "project" "headline"])
    :handler (fn [{:keys [area project headline]}]
               (tools/mark-done area project headline))}

   "clock_in"
   {:description (str "Clock in on an existing headline. Requires an exact, unique "
                     "match: call list_todos first to get the real headline text.")
    :input-schema (object-schema {:area area-schema
                                  :project project-schema
                                  :headline headline-schema}
                                 ["area" "project" "headline"])
    :handler (fn [{:keys [area project headline]}]
               (tools/clock-in area project headline))}

   "clock_out"
   {:description "Clock out of the running clock. Returns clocked-out or no-clock."
    :input-schema (object-schema {} [])
    :handler (fn [_] (tools/clock-out))}

   "list_todos"
   {:description (str "List every TODO/DONE grouped area -> project -> tasks, as "
                     "JSON. Read this to find exact headlines before mark_done or "
                     "clock_in.")
    :input-schema (object-schema {} [])
    :handler (fn [_] (json/write-str (tools/list-todos)))}

   "ensure_file"
   {:description "Create the area/project org file (with a * Tasks section) if missing."
    :input-schema (object-schema {:area area-schema :project project-schema}
                                 ["area" "project"])
    :handler (fn [{:keys [area project]}]
               (tools/ensure-file area project))}})
