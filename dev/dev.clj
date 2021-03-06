(ns dev
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all clear]]
            [com.stuartsierra.component :as component]
            ;[privat-manager.core] 
            [privat-manager.server]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [privat-manager.server :as server]))

(defonce app-db (atom nil))

(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test")
;; (clojure.tools.namespace.repl/set-refresh-dirs "src" "test")

(set-init (fn [_] (server/system { :host "localhost"
                                  :port 3000
                                  :app-atom app-db
                                  :env :dev})))

;; (def auth (privat-manager.settings/get-api-auth @app-db))
;; (-> @app-db :manager :db)
