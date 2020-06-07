(ns privat-manager.privat.parser
  (:require [clojure.walk :as walk]
            [privat-manager.utils :refer [custom-formatter custom-datetime-formatter]]
            [clj-time.format :as time.format]
            [privat-manager.spec :as spec]
            [clojure.spec.alpha :as s]
            [privat-manager.config :as config]))

(defn privat-rest
  "parses privatbant rests"
  [statement]
  (let [acc (-> statement keys first)
        statement (walk/stringify-keys (get statement acc))]
    {
     :acc acc
     :inrest (Float/parseFloat (get statement "balanceIn"))
     :outrest (Float/parseFloat (get statement "balanceOut"))
     :debit (Float/parseFloat (get statement "turnoverDebt"))
     :credit (Float/parseFloat (get statement "turnoverCred"))
     :date (time.format/parse custom-datetime-formatter (get-in statement ["dpd"]))
     })) ;; @customerdate

(defn statement-type
  "return :receipt or :payment depending on :transaction-type"
  [{type :transaction-type}]
  (condp = type
    "C" :receipt
    "D" :payment
    "n/a"))

(defn payment-purpose
  "parses payment string and returns key, by default is payment from supplier"
  [s]
  (reduce (fn [acc [k v]]
            (let [found (re-seq (re-pattern k) s)]
              (if found
                v
                acc)))
          :supplier config/payment-purposes))

(defn receipt-purpose
  "parses receipt string and returns receipt key"
  [s]
  (reduce (fn [acc [k v]]
            (let [found (re-seq (re-pattern k) s)]
              (if found
                v
                acc)))
          :customer config/receipt-purposes))

(defn assoc-transaction-type
  "assoc parsed statement with category from parsed purpose"
  [parsed-statement]
  (let [statement-type (statement-type parsed-statement)]
    (condp = statement-type
      :receipt (assoc parsed-statement :receipt (receipt-purpose (:purpose parsed-statement)))
      :payment (assoc parsed-statement :payment (payment-purpose (:purpose parsed-statement)))
      nil)))

(defn privat-statement-debit
  [m]
  {:pre [(s/valid? ::spec/creditor m)]}
  {:name (get m :AUT_CNTR_NAM)
   :edrpou (get m :AUT_CNTR_CRF)
   :bank-mfo-number (get m :AUT_CNTR_MFO)
   :bank-account-number (get m :AUT_CNTR_ACC)
   })

(defn privat-statement-credit
  [m]
  {:pre [(s/valid? ::spec/debitor m)]}
  {:name (get m :AUT_MY_NAM)
   :edrpou (get m :AUT_MY_CRF)
   :bank-mfo-number (get m :AUT_MY_MFO)
   :bank-account-number (get m :AUT_MY_ACC)
   })

(defn parse-statement
  "parses privatbank statement and returns map for further processing"
  [input]
  {:pre [(s/valid? ::spec/transaction input)]}
  (let [transaction_id (-> input keys first)
        statement (get input transaction_id)]
    {:amount (Float/parseFloat (get-in statement [:BPL_SUM]))
     :refp transaction_id
     :date (time.format/parse custom-datetime-formatter (get-in statement [:DATE_TIME_DAT_OD_TIM_P])) ;; @customerdate
     :purpose (get statement :BPL_OSND)
     :debit (privat-statement-debit statement)
     :credit (privat-statement-credit statement)
     :transaction-type (get statement :TRANTYPE)
     }))
