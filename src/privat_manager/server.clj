(ns privat-manager.server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord Webserver [app-atom routes host port server]
  component/Lifecycle
  (start [component]
    (println "Start web server")
    (assoc component :server
           (run-jetty (-> routes
                          (wrap-defaults (update-in site-defaults [:security] dissoc :anti-forgery))
                          (wrap-params)
                          (wrap-content-type)
                          (wrap-resource "public")
                          (wrap-resource "public/vendor/gentelella")
                          (wrap-not-modified))
                      {:port port :join? false})))
  (stop [component]
    (println "Stop web server")
    (.stop server)
    (assoc component :server nil)))

(defn system [config-options]
  (let [{:keys [host port app-atom routes]} config-options]
    (component/system-map
     :app (map->Webserver {:host host
                           :port port
                           :app-atom app-atom
                           :routes routes}))))
