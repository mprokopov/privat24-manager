(ns privat-manager.manager.api
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]))


(def api (str "http://manager.it-premium.com.ua:8080/api/"))


(defn category-link2 [k settings & index?]
  (let [{bid :business-id} @settings
        cat-uuid (get-in @settings [:uuids k])]
    (str api bid "/" cat-uuid
         (when index? "/index.json"))))

(defn item-link2 [uuid settings]
  (let [{bid :business-id} @settings]
    (str api bid "/" uuid ".json")))

;; (defn category-link [category] (str api business-id "/" category)) 


(defn get-index2! [k settings]
  (let [{:keys [login password]} @settings
        request (client/get (category-link2 k settings true)
                            {:as :json
                             ;; :debug true
                             :basic-auth [login password]})
        body (get request :body)]
    (dorun (map #(swap! settings assoc-in [:db k (keyword %)] nil) body))))

    

(defn fetch-uuid-item! [uuid settings]
  (let [{login :login password :password} @settings
        request (client/get (item-link2 uuid settings)
                            {:as :json
                             :basic-auth [login password]})
        body (get request :body)]
    body))

(defn fetch-uuid-item2! [uuid settings]
  (let [{login :login password :password} @settings]
     (client/get (item-link2 uuid settings)
                 {:as :json
                  :async? true
                  :basic-auth [login password]}
                 (fn [response] (get response :body))
                 (fn [raise] (.getMessage raise)))))

(defn api-update!
  "update map"
  [uuid m db]
  (let [{login :login password :password} @db]
   (client/put (item-link2 uuid db)
               {:basic-auth [login password]
                :body (cheshire.core/generate-string m)})))

(defn api-post2!
  "post map to manager API under key k"
  [k m db]
  (let [{:keys [login password]} @db]
   (client/post (category-link2 k db)
                {:accept :json
                 :content-type :json
                 :basic-auth [login password]
                 :body (cheshire/generate-string m)})))

(defn api-destroy! [uuid db]
  (let [{:keys [login password]} @db]
    (client/delete (item-link2 uuid db)
                   {:basic-auth [login password]})))


(defn populate-db2!
  "asyncronous item fetch for customers, suppliers DB"
  [k app-db]
  (let [db (get-in @app-db [:db k])
        {login :login password :password} @app-db
        f (fn [[item _]] (client/get (item-link2 (name item) app-db)
                                    {:as :json
                                     :async? true
                                     :basic-auth [login password]}
                                    (fn [response] (swap! app-db assoc-in [:db k (keyword item)] (get response :body)))
                                    ;(fn [response] (print response "item " item))
                                    (fn [raise] (.getMessage raise))))]
    (run! f db)))

(defn populate-db!
  "fetch item for every DB uuid and update entry"
  [k settings]
  (let [db (get-in @settings [:db k])
        db2 (reduce #(assoc %1 (-> %2 key keyword) (fetch-uuid-item! (-> %2 key name) settings)) {} db)]
    (swap! settings assoc-in [:db k] db2)
    "ok"))

;; (defn edrpou-by-customer [customer]
;;   (get-in customer [:CustomFields (-> uuids :customer-edrpou keyword)]))

;; (defn edrpou-by-supplier [customer]
;;   (get-in customer [:CustomFields (-> uuids :supplier-edrpou keyword)]))


(defn edrpou-by-customer2 [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in @manager-db [:uuids :customer-edrpou]))] 
    (get-in @manager-db [:db :customers (keyword uuid) :CustomFields custom-edrpou-uuid])))

(defn edrpou-by-supplier2 [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in @manager-db [:uuids :supplier-edrpou]))] 
    (get-in @manager-db [:db :suppliers (keyword uuid) :CustomFields custom-edrpou-uuid])))
