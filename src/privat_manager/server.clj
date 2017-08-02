(ns privat-manager.server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord Handler [routes]
  component/Lifecycle
  (start [component]
    (assoc component :handler (-> routes
                                  (wrap-defaults (update-in site-defaults [:security] dissoc :anti-forgery))
                                  (wrap-params)
                                  (wrap-content-type)
                                  (wrap-resource "public")
                                  (wrap-resource "public/vendor/gentelella")
                                  (wrap-not-modified))))
  (stop [component]
    (dissoc component :handler)))

(defrecord Webserver [app-atom handler host port server]
  component/Lifecycle
  (start [component]
    (println "Start web server")
    (assoc component :server
           (run-jetty (:handler handler)
                      {:port port :join? false})))
  (stop [component]
    (println "Stop web server")
    (.stop server)
    (assoc component :server nil)))


(defn system [config-options]
  (let [{:keys [host port app-atom routes]} config-options]
    (component/system-map
     :routes-handler (map->Handler {:routes routes})
     :app (component/using
           (map->Webserver {:host host
                            :port port
                            :app-atom app-atom}) {:handler :routes-handler}))))
