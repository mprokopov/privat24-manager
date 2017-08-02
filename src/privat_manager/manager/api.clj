(ns privat-manager.manager.api
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]))


(def api (str "http://manager.it-premium.com.ua:8080/api/"))

(def statement-types #{:receipt :transfer :payment})

(defn category-link2 [k settings & index?]
  (let [{bid :business-id} @settings
        cat-uuid (get-in @settings [:uuids k])]
    (str api bid "/" cat-uuid
         (when index? "/index.json"))))

(defn category-link [k settings & index?]
  (let [{bid :business-id} settings
        cat-uuid (get-in settings [:uuids k])]
    (str api bid "/" cat-uuid
         (when index? "/index.json"))))

(defn item-link2 [uuid settings]
  (let [{bid :business-id} @settings]
    (str api bid "/" uuid ".json")))

(defn item-link [uuid app-db]
  (let [{{bid :business-id} :manager} app-db]
    (str api bid "/" uuid ".json")))

(defn get-index2! [k app-db]
  (let [{{:keys [login password] :as manager} :manager} @app-db
        f (fn [item] (swap! app-db assoc-in [:manager :db k (keyword item)] nil))
        {body :body} (client/get (category-link k manager true)
                                 { ;; :debug true
                                  :as :json
                                  :basic-auth [login password]})]
    (dorun (map f body))))

(defn fetch-uuid-item! [uuid app-db]
  (let [{{login :login password :password} :manager} @app-db
        {body :body} (client/get (item-link uuid @app-db)
                                 {:as :json
                                  :basic-auth [login password]})]
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
  [uuid m app-db]
  (let [{{login :login password :password} :manager} @app-db]
   (client/put (item-link uuid @app-db)
               {:basic-auth [login password]
                :body (cheshire.core/generate-string m)})))

(defn api-post2!
  "post map to manager API under key k"
  [k m manager-db]
  (let [{:keys [login password]} manager-db]
   (client/post (category-link k manager-db)
                {:accept :json
                 :content-type :json
                 :basic-auth [login password]
                 :body (cheshire/generate-string m)})))

(defn api-destroy! [uuid db]
  (let [{:keys [login password]} (:manager @db)]
    (client/delete (item-link uuid db)
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

(defn populate-db3!
  "fetch item for every DB uuid and update entry"
  [k settings]
  (let [db (get-in @settings [:manager :db k])
        db2 (reduce #(assoc %1 (-> %2 key keyword) (fetch-uuid-item! (-> %2 key name) settings)) {} db)]
    (swap! settings assoc-in [:manager :db k] db2)
    "ok"))


;; TODO improve with connection-pool https://github.com/dakrone/clj-http#persistent-connections
(defn populate-db!
  "fetch item for every DB uuid and update entry"
  [k app-db]
  (let [db (get-in @app-db [:manager :db k])
        f (fn [[k v]] {k (fetch-uuid-item! (name k) app-db)})]
    (swap! app-db assoc-in [:manager :db k]
     (into {} (pmap f db)))))

;; (defn edrpou-by-customer [customer]
;;   (get-in customer [:CustomFields (-> uuids :customer-edrpou keyword)]))

;; (defn edrpou-by-supplier [customer]
;;   (get-in customer [:CustomFields (-> uuids :supplier-edrpou keyword)]))


(defn edrpou-by-customer2 [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in manager-db [:uuids :customer-edrpou]))] 
    (get-in manager-db [:db :customers (keyword uuid) :CustomFields custom-edrpou-uuid])))

(defn edrpou-by-supplier2 [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in manager-db [:uuids :supplier-edrpou]))] 
    (get-in manager-db [:db :suppliers (keyword uuid) :CustomFields custom-edrpou-uuid])))

;; (defmulti statement-link [] (fn [statement _] (select-keys [:payment :transfer])))

