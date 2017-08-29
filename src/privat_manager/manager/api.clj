(ns privat-manager.manager.api
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]))


(def api (str "http://manager.it-premium.com.ua:8080/api/"))

(def statement-types #{:receipt :transfer :payment})

(defn category-key [m]
  (case (-> (select-keys m statement-types)
            first
            key)
    :receipt :receipts
    :payment :payments
    :transfer :transfers))

(defn category
  "constructor for category"
  [k settings & index?]
  (let [{bid :business-id} settings
        cat-uuid (get-in settings [:uuids k])]
    (str api bid "/" cat-uuid
         (when index? "/index.json"))))

(defn item [uuid app-db]
  (let [{{bid :business-id} :manager} app-db]
    (str api bid "/" uuid ".json")))

(defn get-category [k app-db]
  (let [{{:keys [login password] :as manager} :manager} @app-db
        f (fn [item] (swap! app-db assoc-in [:manager :db k (keyword item)] nil))
        {body :body} (client/get (category k manager true)
                                 { ;; :debug true
                                  :as :json
                                  :basic-auth [login password]})]
    (dorun (map f body))))

(defn get-item [uuid app-db]
  (let [{{login :login password :password} :manager} @app-db
        {body :body} (client/get (item uuid @app-db)
                                 {:as :json
                                  :basic-auth [login password]})]
    body))


(defn update-item
  "update map"
  [uuid m app-db]
  (let [{{login :login password :password} :manager} @app-db]
   (client/put (item uuid @app-db)
               {:basic-auth [login password]
                :body (cheshire.core/generate-string m)})))

(defn create-item
  "post map to manager API under key k"
  [k m manager-db]
  (let [{:keys [login password]} manager-db]
   (client/post (category k manager-db)
                {:accept :json
                 :content-type :json
                 :basic-auth [login password]
                 :body (cheshire/generate-string m)})))

(defn delete-item [uuid db]
  (let [{:keys [login password]} (:manager @db)]
    (client/delete (item uuid db)
                   {:basic-auth [login password]})))




;; TODO improve with connection-pool https://github.com/dakrone/clj-http#persistent-connections
(defn populate-category
  "fetch item for every DB uuid and update entry"
  [k app-db]
  (let [db (get-in @app-db [:manager :db k])
        f (fn [[k v]] {k (get-item (name k) app-db)})]
    (swap! app-db assoc-in [:manager :db k]
      ;(client/with-connection-pool {:timeout 5 :threads 4 :insecure? false :default-per-route 10} 
        (into {} (pmap f db)))))


(defn customer-edrpou [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in manager-db [:uuids :customer-edrpou]))] 
    (get-in manager-db [:db :customers (keyword uuid) :CustomFields custom-edrpou-uuid])))

(defn supplier-edrpou [uuid manager-db]
  (let [custom-edrpou-uuid (keyword (get-in manager-db [:uuids :supplier-edrpou]))] 
    (get-in manager-db [:db :suppliers (keyword uuid) :CustomFields custom-edrpou-uuid])))

(defn supplier-by-edrpou
  "find supplier by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (supplier-edrpou (key %) db)) (get-in db [:db :suppliers]))))


(defn customer-by-edrpou
  "find customer by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (customer-edrpou (key %) db)) (get-in db [:db :customers]))))

(defn uuid-by-key
  "returns uuid from uuids hashmap by key"
  [k db]
  (get-in db [:uuids k]))

(defmulti statement (fn [m _] (select-keys m [:payment :receipt])))

(defmethod statement
  {:payment :operational-expences-bank}
  [statement {db :manager}]
  (assoc statement
         :payee "Приватбанк"
         :recognized true
         :comment "оплата банковских расходов"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid (uuid-by-key :operational-expences-bank db)))

(defmethod statement
  {:payment :phone}
  [statement {db :manager}]
  (assoc statement
         :payee "Астелит"
         :recognized true
         :comment "оплата расходов связи"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :tax (get db :tax-uuid)
         :credit-uuid (uuid-by-key :phone db)))


(defmethod statement
  {:payment :taxes}
  [statement {db :manager}]
  (assoc statement
         :payee "Налоговая"
         :recognized true
         :comment "оплата налогов"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid (uuid-by-key :taxes db)))

(defmethod statement
  {:payment :salary}
  [statement {db :manager}]
  (assoc statement
         :payee "Зарплата"
         :recognized true
         :comment "выплата зарплаты"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid ""))

(defmethod statement
  {:payment :transfer}
  [statement {db :manager}]
  (-> statement
      (dissoc :payment)
      (assoc
       :transfer :payment
       :recognized true
       :comment "снятие кеша"
       :amount (Math/abs (get statement :amount))
       :credit-uuid (:bank-account-uuid db)
       :debit-uuid (:cash-account-uuid db)))) 

(defmethod statement
  {:receipt :transfer}
  [statement {db :manager}]
  (-> statement
      (dissoc :receipt)
      (assoc
       :transfer :receipt
       :recognized true
       :comment "пополнение счета"
       :amount (get statement :amount)
       :debit-uuid (:bank-account-uuid db)
       :credit-uuid (:cash-account-uuid db)))) 

(defmethod statement
  {:payment :supplier}
  [statement {db :manager}]
  (let [edrpou (get-in statement [:credit :edrpou])
        supplier (supplier-by-edrpou edrpou db)
        supplier-name1 (get-in statement [:credit :name])
        supplier-name2 (when supplier (get (val supplier) :Name))]
   (assoc statement
          :payee (or supplier-name2 supplier-name1) 
          :tax (get db :tax-uuid)
          :recognized supplier-name2
          :comment "оплата поставщику"
          :extra (get-in statement [:credit :name])
          :amount (Math/abs (get statement :amount))
          :credit-uuid (when supplier (key supplier))
          :debit-uuid (:bank-account-uuid db))))

(defmethod statement
  {:receipt :customer}
  [statement {db :manager}]
  (let [edrpou (get-in statement [:debit :edrpou])
        customer (customer-by-edrpou edrpou db)
        customer-name1 (when customer (-> customer val :Name))
        customer-name2 (get-in statement [:debit :name])]
   (assoc statement
          :payer (or customer-name1 customer-name2)
          :recognized customer-name1
          :tax (get db :tax-uuid)
          :comment "оплата покупателя"
          :amount (get statement :amount)
          :debit-uuid (when customer (key customer))
          :credit-uuid (get db :bank-account-uuid))))

(defmethod statement
  {:receipt :agent-income}
  [statement {db :manager}]
  (assoc statement
         :debit-uuid (uuid-by-key :agent-income db)
         :credit-uuid (:bank-account-uuid db)
         :tax (get db :tax-uuid)
         :comment "агентский доход"
         :recognized true
         :payer (get-in statement [:debit :name])))

;; ========= ASYNC ==========
;; (defn fetch-uuid-item2! [uuid settings]
;;   (let [{login :login password :password} @settings]
;;      (client/get (item-link2 uuid settings)
;;                  {:as :json
;;                   :async? true
;;                   :basic-auth [login password]}
;;                  (fn [response] (get response :body))
;;                  (fn [raise] (.getMessage raise)))))

;; (defn populate-db2!
;;   "asyncronous item fetch for customers, suppliers DB"
;;   [k app-db]
;;   (let [db (get-in @app-db [:db k])
;;         {login :login password :password} @app-db
;;         f (fn [[item _]] (client/get (item-link2 (name item) app-db)
;;                                     {:as :json
;;                                      :async? true
;;                                      :basic-auth [login password]}
;;                                     (fn [response] (swap! app-db assoc-in [:db k (keyword item)] (get response :body)))
;;                                     ;(fn [response] (print response "item " item))
;;                                     (fn [raise] (.getMessage raise))))]
;;     (run! f db)))
