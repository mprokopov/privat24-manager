(ns privat-manager.config
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]))

(def config-resource "resources/settings")

(def manager-endpoint (env :manager-endpoint))

(defn matched-config-name [item]
  (let [n (.getName item)
        [[_ found] _] (re-seq #"(.*).conf.edn$" n)]
   found))

(defn read-config []
  (let [config-directory (file-seq (io/file config-resource))
        filtered (filter #(re-matches #".*conf.edn$" (.getName %)) config-directory)]
     (set (map matched-config-name filtered))))

;; (def config-set (read-config))
(def config-set (atom (read-config)))

(defn load-edn [res]
  (let [res-file (io/file res)]
    (if (.exists res-file)
      (clojure.edn/read-string (slurp res-file))
      {})))

(defn load-edn-resourse [res]
  (let [loaded (load-edn res)]
    (when (seq loaded) (log/info "Successfully loaded " res))
    loaded))

(def payment-purposes (atom (load-edn-resourse "resources/settings/payment-purposes.edn")))

(def receipt-purposes (atom (load-edn-resourse "resources/settings/receipt-purposes.edn")))

(defn load-uuids2 [settings]
  (let [bid (get-in @settings [:manager :business-id])]
    (swap! settings assoc-in [:manager :uuids]
           (read-string (slurp (str "resources/settings/" bid ".edn"))))))

(defn save-uuids [uuids]
  (let [bid (:business-id @uuids)
        uuid (:uuids @uuids)]
    (spit (str "resources/settings/" bid ".edn") uuid)))

(defn save-settings [settings]
  (let [bid (:business-id @settings)]
    (spit (str "resources/settings/" bid ".edn") @settings)))

(defn load-settings! [business-id settings]
  (do
    (reset! settings
            (read-string
             (slurp (str "resources/settings/" business-id ".conf.edn"))))
   (load-uuids2 settings)))

(defn save-cached-db [business-id db]
  (let [k (if (keyword? business-id) business-id (keyword business-id))
        bid (get-in @db [:business-id])
        file-name (str "resources/cache/" bid "-" (name k) ".edn")]
    (spit file-name (get-in @db [:manager :db k]))))

(defn load-cached-db [k db]
  (let [bid (get-in @db [:business-id])
        file-name (str "resources/cache/" bid "-" (name k) ".edn")]
    (try
     (swap! db assoc-in [:manager :db k]
            (read-string
             (slurp file-name)))
     {:flash { :success "Кеш успешно загружен" }}
     (catch Exception e (str (.getMessage e))))))
