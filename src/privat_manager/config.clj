(ns privat-manager.config)

;; (defn load-uuids [uuids]
;;   (let [bid (:business-id @uuids)]
;;     (swap! uuids assoc :uuids
;;       (read-string (slurp (str "resources/" bid ".edn"))))))

(defn load-uuids2 [settings]
  (let [bid (get-in @settings [:manager :business-id])]
    (swap! settings assoc-in [:manager :uuids]
           (read-string (slurp (str "resources/" bid ".edn"))))))

(defn save-uuids [uuids]
  (let [bid (:business-id @uuids)
        uuid (:uuids @uuids)]
    (spit (str "resources/" bid ".edn") uuid)))

(defn save-settings [settings]
  (let [bid (:business-id @settings)]
    (spit (str "resources/" bid ".edn") @settings)))

(defn load-settings [bid settings]
  (do
   (reset! settings
          (read-string
           (slurp (str "resources/" bid ".edn"))))
   (load-uuids2 settings)))

(defn save-cached-db [k db]
  (let [bid (get-in @db [:business-id])
        file-name (str "resources/" bid "-" (name k) ".edn")]
    (spit file-name (get-in @db [:manager :db k]))))

(defn load-cached-db [k db]
  (let [bid (get-in @db [:business-id])
        file-name (str "resources/" bid "-" (name k) ".edn")]
    (try
     (swap! db assoc-in [:manager :db k]
            (read-string
             (slurp file-name)))
     (catch Exception e (str (.getMessage e))))))
