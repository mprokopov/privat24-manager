(ns privat-manager.privat.util
  (:require [privat-manager.manager.api :as manager.api]
            [clj-time.format :as time.format]
            [clojure.walk :as walk]
            [clojure.string :as str])
  (:import java.util.Locale))

(declare wrap-transfer my-format get-supplier-by-edrpou get-uuid-by-key get-customer-by-edrpou check-payment-purpose check-receipt-purpose parse-statement)


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


(def custom-formatter (time.format/formatter "YYYYMMdd'T'HH:mm:ss"))

(defn parse-account
  "returns map of credit or debit part of the statement"
  [m]
  (let [account (get m "account")
        customer (get account "customer")]
    {:name (get account "@name")
     :edrpou (get customer "@crf")
     :bank-mfo-number (get-in customer ["bank" "@code"])
     :bank-account-number (get account "@number")}))

(defn get-statement-type
  "return :receipt or :payment depending on amount"
  [{amount :amount}]
  (if (> amount 0)
    :receipt
    :payment))


(defn check-payment-purpose
  "parses payment string and returns key, by default is payment from supplier"
  [s]
  (condp re-matches s
    #"Комiсiя за викон.*" :operational-expences-bank
    #"Комiсiя за обслуговування рахунку .*" :operational-expences-bank
    #"Еквайрингова комiсiя.*" :operational-expences-bank
    #"Ком\?с\?я РКО.*" :operational-expences-bank
    #"Комiсiя за внесення коштiв.*" :operational-expences-bank
    #"оплата за телекомунiкацiйнi послуги .*" :phone
    #"сплата за телекомунiкацiйнi послуги .*" :phone
    #"Выдача наличных средств.*" :transfer
    #"Видача готiвки.*" :transfer
    #"зарплата за .*" :salary
    #".*Перевод на свою карту.*" :transfer
    #"заробiтна плата.*" :salary
    #"Заробiтна плата.*" :salary
    #"Зарплата за.*" :salary
    #"ЗАРОБIТНА ПЛАТА.*" :salary
    #".*35643866\;.*" :taxes
    #".*Єдиний податок.*" :taxes
    #".*3053511633\;.*" :taxes
    #"Погашення комiсiї за .*" :operational-expences-bank
    :supplier))

(defn check-receipt-purpose
  "parses receipt string and returns receipt key"
  [s]
  (condp re-matches s
    #"Агентська виногорода.*" :agent-income
    #"Надходження торговельної виручки.*" :transfer
    :customer))


(defn get-uuid-by-key
  "returns uuid from uuids hashmap by key"
  [k db]
  (get-in db [:uuids k]))


(defn get-supplier-by-edrpou
  "find supplier by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (manager.api/edrpou-by-supplier2 (key %) db)) (get-in db [:db :suppliers]))))


(defn get-customer-by-edrpou
  "find customer by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (manager.api/edrpou-by-customer2 (key %) db)) (get-in db [:db :customers]))))


(defn assoc-transaction-type
  "assoc parsed statement with category from parsed purpose"
  [parsed-statement]
  (let [statement-type (get-statement-type parsed-statement)]
    (condp = statement-type
      :receipt (assoc parsed-statement :receipt (check-receipt-purpose (:purpose parsed-statement)))
      :payment (assoc parsed-statement :payment (check-payment-purpose (:purpose parsed-statement)))
      nil)))

(defn parse-rest
  "parses privatbant rests"
  [statement]
  (let [statement (walk/stringify-keys statement)]
      {:inrest (Float/parseFloat (get statement "inrest"))
       :outrest (Float/parseFloat (get statement "outrest"))
       :debit (Float/parseFloat (get statement "debet"))
       :credit (Float/parseFloat (get statement "credit"))
       :date (time.format/parse custom-formatter (get-in statement ["date" "@id"]))})) ;; @customerdate
  
(defn parse-statement
  "parses privatbank statement and returns map for further processing"
  [statement]
  (let [statement (walk/stringify-keys statement)]
   {:amount (Float/parseFloat (get-in statement ["amount" "@amt"]))
    :refp (get-in statement ["info" "@refp"])
    :date (time.format/parse custom-formatter (get-in statement ["info" "@postdate"])) ;; @customerdate
    :purpose (get statement "purpose")
    :debit (parse-account (get-in statement ["debet"]))
    :credit (parse-account (get-in statement ["credit"]))})) 

;; (def transform->manager2 (comp assoc-transaction-type parse-statement))

(defn my-format [fmt n & [locale]]
  (let [locale (if locale (Locale. locale)
                   (Locale/getDefault))]
    (String/format locale fmt (into-array Object [n]))))

(defmulti make-manager-statement (fn [m _] (select-keys m [:payment :receipt])))

(defmethod make-manager-statement
  {:payment :operational-expences-bank}
  [statement {db :manager}]
  (assoc statement
         :payee "Приватбанк"
         :recognized true
         :comment "оплата банковских расходов"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid (get-uuid-by-key :operational-expences-bank db)))

(defmethod make-manager-statement
  {:payment :phone}
  [statement {db :manager}]
  (assoc statement
         :payee "Астелит"
         :recognized true
         :comment "оплата расходов связи"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :tax (get db :tax-uuid)
         :credit-uuid (get-uuid-by-key :phone db)))


(defmethod make-manager-statement
  {:payment :taxes}
  [statement {db :manager}]
  (assoc statement
         :payee "Налоговая"
         :recognized true
         :comment "оплата налогов"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid (get-uuid-by-key :taxes db)))

(defmethod make-manager-statement
  {:payment :salary}
  [statement {db :manager}]
  (assoc statement
         :payee "Зарплата"
         :recognized true
         :comment "выплата зарплаты"
         :amount (Math/abs (get statement :amount))
         :debit-uuid (:bank-account-uuid db)
         :credit-uuid ""))

(defmethod make-manager-statement
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

(defmethod make-manager-statement
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

(defmethod make-manager-statement
  {:payment :supplier}
  [statement {db :manager}]
  (let [edrpou (get-in statement [:credit :edrpou])
        supplier (get-supplier-by-edrpou edrpou db)
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

(defmethod make-manager-statement
  {:receipt :customer}
  [statement {db :manager}]
  (let [edrpou (get-in statement [:debit :edrpou])
        customer (get-customer-by-edrpou edrpou db)
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

(defmethod make-manager-statement
  {:receipt :agent-income}
  [statement {db :manager}]
  (assoc statement
         :debit-uuid (get-uuid-by-key :agent-income db)
         :credit-uuid (:bank-account-uuid db)
         :tax (get db :tax-uuid)
         :comment "агентский доход"
         :recognized true
         :payer (get-in statement [:debit :name])))

(defmulti render-manager-statement (fn [m] (-> m
                                                (select-keys manager.api/statement-types)  ; :payment, :receipt, :transfer
                                                first
                                                key)))

(defmethod render-manager-statement :payment [m]
  (wrap-payment m))

(defmethod render-manager-statement :receipt [m]
  (wrap-receipt m))

(defmethod render-manager-statement :transfer [m]
  (wrap-transfer m))

(defmulti get-category-key
  (fn [x] (-> (select-keys x [:payment :receipt :transfer])
             first
             key)))

(defmethod get-category-key :payment [_] :payments)

(defmethod get-category-key :receipt [_] :receipts)

(defmethod get-category-key :transfer [_] :transfers)

(defn statement-account
  "returns key for category depending on statement type
  :payments, :receipts or :transfers
  it's needed to find category uuid when doing API post to Manager"
  [statement]
  (cond
    (:receipt statement) (if (= :transfer (:receipt statement)) :transfers :receipts)
    (:payment statement) (if (= :transfer (:payment statement)) :transfers :payments)))


(defn post2
  "parses initial bank statement and makes POST to Manager API
  db should contain {:login '' :password ''}"
  [statement manager-db]
  (let [category-key (get-category-key statement)]
    (manager.api/api-post2! category-key (render-manager-statement statement) manager-db))) 

;; (defn get-statement-by-index [x db]
;;   (let [statements (get-in @db [:manager :db :mstatements])]
;;     (nth statements x)))


(defn make-statements-list [db]
  (get-in db [:manager :db :statements]))

(defn format-floats [m]
  (let [f (fn [acc mkey mval] (assoc acc mkey (if (float? mval) (format "%.2f" mval) mval)))]
    (reduce-kv f {} m)))

(defn privat->manager [app-db] (comp (map parse-statement)
                                  (map assoc-transaction-type)
                                  (map #(make-manager-statement % app-db))))
