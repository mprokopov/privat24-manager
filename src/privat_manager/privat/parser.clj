(ns privat-manager.privat.parser
  (:require [clojure.walk :as walk]
            [privat-manager.utils :refer [custom-formatter]]
            [clj-time.format :as time.format]
            [privat-manager.config :as config]))

(defn privat-rest
  "parses privatbant rests"
  [statement]
  (let [statement (walk/stringify-keys statement)]
    {:inrest (Float/parseFloat (get statement "inrest"))
     :outrest (Float/parseFloat (get statement "outrest"))
     :debit (Float/parseFloat (get statement "debet"))
     :credit (Float/parseFloat (get statement "credit"))
     :date (time.format/parse custom-formatter (get-in statement ["date" "@id"]))})) ;; @customerdate

(defn statement-type
  "return :receipt or :payment depending on amount"
  [{amount :amount}]
  (if (> amount 0)
    :receipt
    :payment))

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


(defn privat-account
  "returns map of credit or debit part of the statement"
  [m]
  (let [account (get m "account")
        customer (get account "customer")]
    {:name (get account "@name")
     :edrpou (get customer "@crf")
     :bank-mfo-number (get-in customer ["bank" "@code"])
     :bank-account-number (get account "@number")}))

(defn parse-statement
  "parses privatbank statement and returns map for further processing"
  [statement]
  (let [statement (walk/stringify-keys statement)]
    {:amount (Float/parseFloat (get-in statement ["amount" "@amt"]))
     :refp (get-in statement ["info" "@refp"])
     :date (time.format/parse custom-formatter (get-in statement ["info" "@postdate"])) ;; @customerdate
     :purpose (get statement "purpose")
     :debit (privat-account (get-in statement ["debet"]))
     :credit (privat-account (get-in statement ["credit"]))})) 
