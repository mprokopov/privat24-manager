(ns user
  (:require [com.stuartsierra.component.user-helpers :refer [dev go reset]])) ;;[com.stuartsierra.component :as component]
            ;; [privat-manager.server :as server]
            ;; [privat-manager.core]
            ;; [clojure.tools.namespace.repl :refer (refresh)]))


;; (def sys nil)

;; ;; (def app-db (atom nil))

;; (defn init []
;;   (alter-var-root #'sys
;;                   (constantly (server/system {:host "localhost"
;;                                               :port 8080
;;                                               :routes (privat-manager.core/web-handler privat-manager.core/app-db)
;;                                               :app-atom privat-manager.core/app-db}))))

;; (defn start []
;;   (alter-var-root #'sys component/start))

;; (defn stop []
;;   (alter-var-root #'sys
;;                   (fn [s] (when s (component/stop s)))))

;; (defn go []
;;   (init)
;;   (start))

;; (defn reset []
;;   (stop)
;;   (refresh))
;;   ;(refresh :after 'user/go))
