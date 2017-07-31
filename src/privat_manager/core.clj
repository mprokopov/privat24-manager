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
            [privat-manager.template :refer [x-panel sidebar-menu] :as templ]
            [compojure.core :refer [defroutes GET POST context] :as compojure]
            [compojure.coercions :refer [as-int as-uuid]]
            [clj-time.coerce :as time.coerce]
            ;; [ring.middleware.resource :refer [wrap-resource]]
            ;; [ring.middleware.params :refer [wrap-params]]
            ;; [ring.middleware.not-modified :refer [wrap-not-modified]]
            ;; [ring.middleware.content-type :refer [wrap-content-type]]
            ;; [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            ;; [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]
            [clojure.string :as str])
  (:gen-class))

(declare with-redirect)

(def app-db (atom {:business-id ""
                   :privat nil
                   :manager nil}))


(defn template [& body]
  (templ/template app-db body))


(defn load-account! [account app-db]
  (with-redirect
    (settings/load-account! account app-db)
    "/settings"))


(defn settings-index [app-db]
  (template (settings/index app-db)))


(defn fetch-statements! [app-db stdate endate]
  (with-redirect
    (mstatement/fetch! app-db stdate endate)
    "/statements"))


(defn statements-index [app-db]
  (template [:h1 "Выписки"] (mstatement/index @app-db)))

(defn single-statement [index]
  (template (mstatement/single index @app-db)))

(defn post-statement! [index app-db]
  (template (mstatement/post! index @app-db)))

(defn single-customer [uuid app-db]
   (template (customers/single uuid @app-db)))

(defn single-supplier [uuid app-db]
  (template (suppliers/single uuid @app-db)))

(defn update-customer! [uuid m app-db]
  (template (customers/update! uuid m app-db)))

(defn update-supplier! [uuid m app-db]
  (template (suppliers/update! uuid m app-db)))

(defn login! [app-db]
 (with-redirect
  (do
    (privat.auth/auth app-db)
    (-> (privat.auth/auth-p24 app-db)
        (get-in [:privat :session])
        (update :expires #(time.coerce/from-long (* % 1000)))
        utils/map-to-html-list))
  "/settings"))

(defn with-redirect [f to]
  (do
    f
    (ring.util.response/redirect to))) 

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
        (POST "/:uuid" [uuid edrpou] (update-customer! uuid {:edrpou edrpou} app-db))) 
   (context "/suppliers" []
        (GET "/" [] (template (suppliers/index @app-db)))
        (GET "/:uuid" [uuid] (single-supplier uuid app-db)) 
        (POST "/:uuid" [uuid edrpou] (update-supplier! uuid {:edrpou edrpou} app-db)) 
        (POST "/" [] (with-redirect (suppliers/fetch! app-db) "/suppliers")))
   (context "/settings" []
        (GET "/" [] (settings-index app-db))
        (POST "/" [account] (load-account! account app-db))
        (GET "/load" [data] (with-redirect (config/load-cached-db (keyword data) app-db) "/settings"))
        (GET "/save" [data] (with-redirect (config/save-cached-db (keyword data) app-db) "/settings")))
   (context "/auth" []
        (POST "/login" [] (login! app-db))
        (GET "/login/otp" [id] (templ/template app-db (privat.auth/send-otp2! id app-db)))
        (POST "/login/otp" [otp] (with-redirect (privat.auth/check-otp2! otp app-db)))
        (GET "/logout" [] (with-redirect (privat.auth/logout! app-db) "/settings")))
   (context "/statements" []
        (GET "/" [] (statements-index app-db))
        (POST "/" [stdate endate] (fetch-statements! app-db stdate endate))
        (POST "/:id" [id :<< as-int] (post-statement! id app-db))
        (GET "/:id" [id :<< as-int] (single-statement id)))))


;; (def sys (server/system {:host "locahost" :port 3000 :app-atom app-db :routes (web-handler app-db)}))

;; (defn start-dev []
;;   (alter-var-root #'sys component/start))

;; (defn stop-dev []
;;   (alter-var-root #'sys component/stop))

;; (defn restart []
;;   (stop-dev)
;;   (start-dev))

(defn -main [& args]
  (settings/load-account! (first args) app-db)
  (component/start
    (server/system {:host "0.0.0.0" :port 3000 :app-atom app-db :routes (web-handler app-db)})))
  ;(start-dev))
