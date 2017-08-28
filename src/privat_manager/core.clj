(ns privat-manager.core
  (:require [privat-manager.privat.auth :as privat.auth]
            [privat-manager.utils :as utils]
            [privat-manager.rests :as rests]
            [privat-manager.statement :as mstatement]
            [privat-manager.customers :as customers]
            [privat-manager.suppliers :as suppliers]
            [privat-manager.settings :as settings]
            [privat-manager.config :as config]
            [privat-manager.server :as server]
            [privat-manager.auth :as auth]
            [privat-manager.template :as templ]
            [compojure.core :refer [GET POST context] :as compojure]
            [compojure.coercions :refer [as-int as-uuid]]
            [clj-time.coerce :as time.coerce]
            [com.stuartsierra.component :as component]
            [clojure.string :as str])
  (:gen-class))

(declare with-redirect)

(def app-db (atom {:business-id ""
                   :privat nil
                   :manager nil}))


(defn load-account! [account app-db]
  (settings/load-account! account app-db))

(defn -main [& args]
  (settings/load-account! (first args) app-db)
  (component/start
    (server/system {:host "0.0.0.0" :port 3000 :app-atom app-db :env :prod}))) 

