(ns privat-manager.privat.util
  (:require [privat-manager.manager.api :as manager.api]
            [privat-manager.privat.parser :as privat.parser]
            [privat-manager.utils :refer [custom-formatter my-format]]
            [clj-time.format :as time.format]
            [clojure.string :as str]))


;; (defn uuid-by-key
;;   "returns uuid from uuids hashmap by key"
;;   [k db]
;;   (get-in db [:uuids k]))

;; (defmulti make-manager-statement (fn [m _] (select-keys m [:payment :receipt])))

;; (defmethod make-manager-statement
;;   {:payment :operational-expences-bank}
;;   [statement {db :manager}]
;;   (assoc statement
;;          :payee "Приватбанк"
;;          :recognized true
;;          :comment "оплата банковских расходов"
;;          :amount (Math/abs (get statement :amount))
;;          :debit-uuid (:bank-account-uuid db)
;;          :credit-uuid (uuid-by-key :operational-expences-bank db)))

;; (defmethod make-manager-statement
;;   {:payment :phone}
;;   [statement {db :manager}]
;;   (assoc statement
;;          :payee "Астелит"
;;          :recognized true
;;          :comment "оплата расходов связи"
;;          :amount (Math/abs (get statement :amount))
;;          :debit-uuid (:bank-account-uuid db)
;;          :tax (get db :tax-uuid)
;;          :credit-uuid (uuid-by-key :phone db)))


;; (defmethod make-manager-statement
;;   {:payment :taxes}
;;   [statement {db :manager}]
;;   (assoc statement
;;          :payee "Налоговая"
;;          :recognized true
;;          :comment "оплата налогов"
;;          :amount (Math/abs (get statement :amount))
;;          :debit-uuid (:bank-account-uuid db)
;;          :credit-uuid (uuid-by-key :taxes db)))

;; (defmethod make-manager-statement
;;   {:payment :salary}
;;   [statement {db :manager}]
;;   (assoc statement
;;          :payee "Зарплата"
;;          :recognized true
;;          :comment "выплата зарплаты"
;;          :amount (Math/abs (get statement :amount))
;;          :debit-uuid (:bank-account-uuid db)
;;          :credit-uuid ""))

;; (defmethod make-manager-statement
;;   {:payment :transfer}
;;   [statement {db :manager}]
;;   (-> statement
;;       (dissoc :payment)
;;       (assoc
;;        :transfer :payment
;;        :recognized true
;;        :comment "снятие кеша"
;;        :amount (Math/abs (get statement :amount))
;;        :credit-uuid (:bank-account-uuid db)
;;        :debit-uuid (:cash-account-uuid db)))) 

;; (defmethod make-manager-statement
;;   {:receipt :transfer}
;;   [statement {db :manager}]
;;   (-> statement
;;       (dissoc :receipt)
;;       (assoc
;;        :transfer :receipt
;;        :recognized true
;;        :comment "пополнение счета"
;;        :amount (get statement :amount)
;;        :debit-uuid (:bank-account-uuid db)
;;        :credit-uuid (:cash-account-uuid db)))) 

;; (defmethod make-manager-statement
;;   {:payment :supplier}
;;   [statement {db :manager}]
;;   (let [edrpou (get-in statement [:credit :edrpou])
;;         supplier (manager.api/supplier-by-edrpou edrpou db)
;;         supplier-name1 (get-in statement [:credit :name])
;;         supplier-name2 (when supplier (get (val supplier) :Name))]
;;    (assoc statement
;;           :payee (or supplier-name2 supplier-name1) 
;;           :tax (get db :tax-uuid)
;;           :recognized supplier-name2
;;           :comment "оплата поставщику"
;;           :extra (get-in statement [:credit :name])
;;           :amount (Math/abs (get statement :amount))
;;           :credit-uuid (when supplier (key supplier))
;;           :debit-uuid (:bank-account-uuid db))))

;; (defmethod make-manager-statement
;;   {:receipt :customer}
;;   [statement {db :manager}]
;;   (let [edrpou (get-in statement [:debit :edrpou])
;;         customer (manager.api/customer-by-edrpou edrpou db)
;;         customer-name1 (when customer (-> customer val :Name))
;;         customer-name2 (get-in statement [:debit :name])]
;;    (assoc statement
;;           :payer (or customer-name1 customer-name2)
;;           :recognized customer-name1
;;           :tax (get db :tax-uuid)
;;           :comment "оплата покупателя"
;;           :amount (get statement :amount)
;;           :debit-uuid (when customer (key customer))
;;           :credit-uuid (get db :bank-account-uuid))))

;; (defmethod make-manager-statement
;;   {:receipt :agent-income}
;;   [statement {db :manager}]
;;   (assoc statement
;;          :debit-uuid (uuid-by-key :agent-income db)
;;          :credit-uuid (:bank-account-uuid db)
;;          :tax (get db :tax-uuid)
;;          :comment "агентский доход"
;;          :recognized true
;;          :payer (get-in statement [:debit :name])))

(defmulti render-manager-statement (fn [m] (-> m
                                                (select-keys manager.api/statement-types)  ; :payment, :receipt, :transfer
                                                first
                                                key)))

(defn wrap-payment [{:keys [amount credit-uuid debit-uuid tax date payee refp purpose extra]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "Payee" payee
                 "CreditAccount" (when debit-uuid (name debit-uuid))
                 "Notes" (str "Reference: " refp)
                 "BankClearStatus" "Cleared"
                 "Lines" [{"Account" (when credit-uuid (name credit-uuid))
                           "Description" purpose
                           "Amount" my-amount}]}]
    (cond-> payment
      tax (assoc-in ["Lines" 0 "TaxCode"] tax)
      extra (update "Notes" str " " extra))))

(defn wrap-receipt [{:keys [amount credit-uuid debit-uuid tax date payer refp purpose]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "Payer" payer
                 "DebitAccount" (when credit-uuid (name credit-uuid))
                 "Notes" (str "Reference: " refp)
                 "BankClearStatus" "Cleared"
                 "Lines" [{"Account" (when debit-uuid (name debit-uuid))
                           "Description" purpose
                           "Amount" my-amount}]}]
   (cond-> payment
     tax (assoc-in ["Lines" 0 "TaxCode"] tax))))

(defn wrap-transfer [{:keys [amount credit-uuid debit-uuid tax date refp]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        my-date (time.format/unparse (time.format/formatters :date) date)
        payment {"Date" my-date
                 "CreditAccount" credit-uuid
                 "DebitAccount" debit-uuid
                 "Notes" (str "Reference: " refp)
                 "DebitClearStatus" "Cleared"
                 "CreditClearStatus" "Cleared"
                 "CreditClearDate" my-date
                 "CreditAmount" my-amount}]
    payment))

(defmethod render-manager-statement :payment [m]
  (wrap-payment m))

(defmethod render-manager-statement :receipt [m]
  (wrap-receipt m))

(defmethod render-manager-statement :transfer [m]
  (wrap-transfer m))


;; (defn statement-account
;;   "returns key for category depending on statement type
;;   :payments, :receipts or :transfers
;;   it's needed to find category uuid when doing API post to Manager"
;;   [statement]
;;   (cond
;;     (:receipt statement) (if (= :transfer (:receipt statement)) :transfers :receipts)
;;     (:payment statement) (if (= :transfer (:payment statement)) :transfers :payments)))


;; (defn create-manager-statement
;;   "parses initial bank statement and makes POST to Manager API
;;   db should contain {:login '' :password ''}"
;;   [statement manager-db]
;;   (let [category-key (manager.api/category-key statement)]
;;     (manager.api/create-item category-key (render-manager-statement statement) manager-db))) 


;; (defn privat->manager [app-db] (comp (map privat.parser/parse-statement)
;;                                   (map privat.parser/assoc-transaction-type)
;;                                   (map #(make-manager-statement % app-db))))
