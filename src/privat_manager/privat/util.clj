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
                 "CreditAccount" (when credit-uuid (name credit-uuid))
                 "Notes" (str "Reference: " refp)
                 "BankClearStatus" "Cleared"
                 "Lines" [{"Account" (when debit-uuid (name debit-uuid))
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
                 "Lines" [{"DebitAccount" (when debit-uuid (name debit-uuid))
                           "Description" purpose
                           "Amount" my-amount}]}]
   (cond-> payment
     tax (assoc-in ["Lines" 0 "TaxCode"] tax))))

(defn wrap-transfer [{:keys [amount credit-uuid debit-uuid tax date refp]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "CreditAccount" credit-uuid
                 "DebitAccount" debit-uuid
                 "Notes" (str "Reference: " refp)
                 "DebitClearStatus" "Cleared"
                 "CreditClearStatus" "Cleared"
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
  (get-in @db [:uuids k]))


(defn get-supplier-by-edrpou
  "find supplier by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (manager.api/edrpou-by-supplier2 (key %) db)) (get-in @db [:db :suppliers]))))


(defn get-customer-by-edrpou
  "find customer by custom code edrpou"
  [edrpou db]
  (first
   (filter #(= edrpou (manager.api/edrpou-by-customer2 (key %) db)) (get-in @db [:db :customers]))))


(defn append-uuid
  "appends debit-uuid and credit-uuid to parsed statement depending on statement type.
  It should be [:receipt :customer] or [:receipt :transfer] or [:payment :operational-expences-bank]"
  [parsed-statement db]
  (condp #(contains? %2 %1) parsed-statement
    :receipt (condp = (:receipt parsed-statement)
               :transfer (assoc parsed-statement
                                :recognized true
                                :credit-uuid (:bank-account-uuid @db)
                                :debit-uuid (:cash-account-uuid @db))
               :agent-income (assoc parsed-statement
                                    :recognized true
                                    :payer (get-in parsed-statement [:debit :name])
                                    :debit-uuid (:bank-account-uuid @db))
               :customer (let [{{edrpou :edrpou debitor-name :name} :debit} parsed-statement
                               customer (get-customer-by-edrpou edrpou db)
                               uuid (if customer (key customer) "")
                               n (if customer (get (val customer) :Name) debitor-name)] 
                           (assoc parsed-statement
                                  :payer n
                                  :recognized (when customer true)
                                  :tax-code (get @db :tax-uuid)
                                  :credit-uuid uuid
                                  :debit-uuid (:bank-account-uuid @db))))
    :payment (condp = (:payment parsed-statement)
               :operational-expences-bank (assoc parsed-statement
                                                 :payee "Приватбанк"
                                                 :recognized true
                                                 :credit-uuid (get-uuid-by-key :operational-expences-bank db)
                                                 :debit-uuid (:bank-account-uuid @db))
               :taxes (assoc parsed-statement
                             :payee "Налоговая"
                             :recognized true
                             :credit-uuid (get-uuid-by-key :taxes db)
                             :debit-uuid (:bank-account-uuid @db))
               :salary (assoc parsed-statement
                              :payee "Зарплата"
                              :recognized true
                              :debit-uuid (:bank-account-uuid @db))
               :transfer (assoc parsed-statement
                                :payee "Трансфер"
                                :recognized true
                                :debit-uuid (:bank-account-uuid @db)
                                :credit-uuid (:cash-account-uuid @db))
               :phone (assoc parsed-statement
                             :payee "Астелит"
                             :recognized true
                             :credit-uuid (get-uuid-by-key :phone db)
                             :debit-uuid (:bank-account-uuid @db))
               :supplier (let [{{edrpou :edrpou} :credit} parsed-statement
                               supplier (get-supplier-by-edrpou edrpou db)
                               uuid (if supplier (key supplier) "")
                               n (if supplier (get (val supplier) :Name) "")]
                           (assoc parsed-statement
                                  :payee n
                                  :tax-code (get @db :tax-uuid)
                                  :recognized (when supplier true)
                                  :credit-uuid uuid
                                  :debit-uuid (:bank-account-uuid @db)))
               nil)
    nil))

(defn assoc-transaction-type
  "assoc parsed statement with category from parsed purpose"
  [parsed-statement]
  (let [statement-type (get-statement-type parsed-statement)]
    (condp = statement-type
      :receipt (assoc parsed-statement :receipt (check-receipt-purpose (:purpose parsed-statement)))
      :payment (assoc parsed-statement :payment (check-payment-purpose (:purpose parsed-statement)))
      nil)))

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

(def transform->manager2 (comp category-by-purpose parse-statement))

(defn my-format [fmt n & [locale]]
  (let [locale (if locale (Locale. locale)
                   (Locale/getDefault))]
    (String/format locale fmt (into-array Object [n]))))

(defmulti make-manager-statement (fn [m _] (select-keys m [:payment :receipt])))

(defmethod make-manager-statement
  {:payment :operational-expences-bank}
  [statement db]
  (assoc statement
         :payee "Приватбанк"
         :recognized true
         :comment "оплата банковских расходов"
         :amount (Math/abs (get statement :amount))
         :credit-uuid (:bank-account-uuid @db)
         :debit-uuid (get-uuid-by-key :operational-expences-bank db)))

(defmethod make-manager-statement
  {:payment :phone}
  [statement db]
  (assoc statement
         :payee "Астелит"
         :recognized true
         :comment "оплата расходов связи"
         :amount (Math/abs (get statement :amount))
         :credit-uuid (:bank-account-uuid @db)
         :tax (get @db :tax-uuid)
         :debit-uuid (get-uuid-by-key :phone db)))


(defmethod make-manager-statement
  {:payment :taxes}
  [statement db]
  (assoc statement
         :payee "Налоговая"
         :recognized true
         :comment "оплата налогов"
         :amount (Math/abs (get statement :amount))
         :credit-uuid (:bank-account-uuid @db)
         :debit-uuid (get-uuid-by-key :taxes db)))

(defmethod make-manager-statement
  {:payment :salary}
  [statement db]
  (assoc statement
         :payee "Зарплата"
         :recognized true
         :comment "выплата зарплаты"
         :amount (Math/abs (get statement :amount))
         :credit-uuid (:bank-account-uuid @db)
         :debit-uuid ""))

(defmethod make-manager-statement
  {:payment :transfer}
  [statement db]
  (-> statement
      (dissoc :payment)
      (assoc
       :transfer :payment
       :recognized true
       :comment "снятие кеша"
       :amount (Math/abs (get statement :amount))
       :credit-uuid (:bank-account-uuid @db)
       :debit-uuid (:cash-account-uuid @db)))) 

(defmethod make-manager-statement
  {:receipt :transfer}
  [statement db]
  (-> statement
      (dissoc :receipt)
      (assoc
       :transfer :receipt
       :recognized true
       :comment "пополнение счета"
       :amount (get statement :amount)
       :debit-uuid (:bank-account-uuid @db)
       :credit-uuid (:cash-account-uuid @db)))) 

(defmethod make-manager-statement
  {:payment :supplier}
  [statement db]
  (let [edrpou (get-in statement [:credit :edrpou])
        supplier (get-supplier-by-edrpou edrpou db)
        supplier-name1 (get-in statement [:credit :name])
        supplier-name2 (when supplier (get (val supplier) :Name))]
   (assoc statement
          :payee (or supplier-name2 supplier-name1) 
          :tax (get @db :tax-uuid)
          :recognized supplier-name2
          :comment "оплата поставщику"
          :extra (get-in statement [:credit :name])
          :amount (Math/abs (get statement :amount))
          :credit-uuid (key supplier)
          :debit-uuid (:bank-account-uuid @db))))

(defmethod make-manager-statement
  {:receipt :customer}
  [statement db]
  (let [edrpou (get-in statement [:debit :edrpou])
        customer (get-customer-by-edrpou edrpou db)
        customer-name1 (when customer (-> customer val :Name))
        customer-name2 (get-in statement [:debit :name])]
   (assoc statement
          :payer (or customer-name1 customer-name2)
          :recognized customer-name1
          :tax (get @db :tax-uuid)
          :comment "оплата покупателя"
          :amount (get statement :amount)
          :debit-uuid (when customer (key customer))
          :credit-uuid (get @db :bank-account-uuid))))

(defmethod make-manager-statement
  {:receipt :agent-income}
  [statement db]
  (assoc statement
         :debit (get-uuid-by-key :agent-income db)
         :credit-uuid (:bank-account-uuid @db)
         :comment "агентский доход"
         :recognized true
         :payer (get-in statement [:debit :name])))

(defmulti render-manager-statement (fn [m] (-> m
                                                (select-keys [:payment :receipt :transfer])
                                                first
                                                key)))

(defmethod render-manager-statement :payment [m]
  (wrap-payment m))

(defmethod render-manager-statement :receipt [m]
  (wrap-receipt m))

(defmethod render-manager-statement :transfer [m]
  (wrap-transfer m))

(defn transform-statement
  "transforms parsed hashmap for Manager suitable format"
  [statement]
  (let [{:keys [amount payee payer purpose refp credit debit date credit-uuid debit-uuid tax-code]} statement
        amount-localized (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        lines {"Description" purpose
               "Amount" amount-localized
               "Account" credit-uuid}
        state {"Date" (time.format/unparse (time.format/formatters :date) date)
               "Notes" (str "Reference: " refp)

               "Lines" [(if tax-code (assoc lines "TaxCode" tax-code) lines)]}]
    (condp #(contains? %2 %1) statement
      :receipt
      (if (= :transfer (:receipt statement))
        (-> state
            (dissoc "Lines")
            (assoc "DebitAccount" credit-uuid
                   "CreditAccount" debit-uuid
                   "CreditAmount" amount-localized
                   "Description" purpose
                   "DebitClearStatus" "Cleared"
                   "CreditClearStatus" "Cleared"))
        (assoc state "Payer" payer "DebitAccount" debit-uuid))
      :payment (if (= :transfer (:payment statement))
                 (-> state
                     (dissoc "Lines")
                     (assoc "DebitAccount" credit-uuid
                            "CreditAccount" debit-uuid
                            "CreditAmount" amount-localized
                            "Description" purpose
                            "DebitClearStatus" "Cleared"
                            "CreditClearStatus" "Cleared"))
                 (assoc state "Payee" payee "CreditAccount" debit-uuid)))))

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


(defn post
  "parses initial bank statement and makes POST to Manager API
  db should contain {:login '' :password ''}"
  [statement db]
  (let [state (-> statement
                  parse-statement
                  assoc-transaction-type
                  (append-uuid db))
        category-key (statement-account state)]
    (manager.api/api-post2! category-key (transform-statement state) db))) 

(defn post2
  "parses initial bank statement and makes POST to Manager API
  db should contain {:login '' :password ''}"
  [statement db]
  (let [state (-> statement
                  transform->manager2
                  (make-manager-statement db))
        category-key (get-category-key state)]
    (manager.api/api-post2! category-key (render-manager-statement state) db))) 

(defn get-statement-by-index [x db]
  (let [statements (get-in @db [:db :statements])]
    (nth statements x)))


(defn make-statements-list [db]
  (-> (get-in @db [:db :statements])))
