(ns dev
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all clear]]
            [com.stuartsierra.component :as component]
            [privat-manager.core :refer [app-db web-handler]]
            [privat-manager.server]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [privat-manager.server :as server]))


(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test")

(set-init (fn [_] (server/system {:host "localhost" :port 3000 :app-atom app-db :routes (web-handler app-db)})))

