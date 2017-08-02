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
  (with-redirect
    (settings/load-account! account app-db)
    "/settings"))

(defn with-redirect [f to]
  (do
    f
    (cond-> (ring.util.response/redirect to)
      (:flash f) (assoc :flash (:flash f))))) 

(defn web-handler [app-db]
  (compojure/routes
   (GET "/" [] (with-redirect identity "/settings"))
   (context "/rests" []
        (GET "/" [] (templ/template app-db [:h1 "Остатки"] (rests/index @app-db)))
        (POST "/" [stdate endate] (with-redirect (rests/fetch! app-db stdate endate) "/rests")))
   (context "/customers" []
        (GET "/" [] (templ/template app-db (customers/index @app-db)))
        (POST "/" [] (with-redirect (customers/fetch! app-db) "/customers"))
        (GET "/:uuid" [uuid] (templ/template app-db (customers/single uuid @app-db))) 
        (POST "/:uuid" [uuid edrpou] (templ/template app-db (customers/update! uuid {:edrpou edrpou} app-db)))) 
   (context "/suppliers" []
        (GET "/" [] (templ/template app-db (suppliers/index @app-db)))
        (GET "/:uuid" [uuid] (templ/template app-db (suppliers/single uuid app-db))) 
        (POST "/:uuid" [uuid edrpou] (templ/template app-db (suppliers/update! uuid {:edrpou edrpou} app-db))) 
        (POST "/" [] (with-redirect (suppliers/fetch! app-db) "/suppliers")))
   (context "/settings" []
        (GET "/" [] (templ/template app-db (settings/index app-db)))
        (POST "/" [account] (load-account! account app-db))
        (GET "/load" [data] (with-redirect (config/load-cached-db (keyword data) app-db) "/settings"))
        (GET "/save" [data] (with-redirect (config/save-cached-db (keyword data) app-db) "/settings")))
   (context "/auth" []
        (POST "/login" [] (with-redirect (auth/login! app-db) "/settings"))
        (GET "/login/otp" [id] (templ/template app-db (privat.auth/send-otp2! id app-db)))
        (POST "/login/otp" [otp] (with-redirect (privat.auth/check-otp2! otp app-db)))
        (GET "/logout" [] (with-redirect (privat.auth/logout! app-db) "/settings")))
   (context "/statements" []
        (GET "/" {flash :flash} (templ/template app-db flash (mstatement/index @app-db)))
        (POST "/" [stdate endate] (with-redirect (mstatement/fetch! app-db stdate endate) "/statements"))
        (POST "/:id" [id :<< as-int] (templ/template app-db (mstatement/post! id @app-db)))
        (GET "/:id" [id :<< as-int] (templ/template app-db (mstatement/single id @app-db))))))

(defn -main [& args]
  (settings/load-account! (first args) app-db)
  (component/start
    (server/system {:host "0.0.0.0" :port 3000 :app-atom app-db :routes (web-handler app-db)})))

