(ns privat-manager.privat.util
  (:require [privat-manager.manager.api :as manager.api]
            [clj-time.format :as time.format]
            [clojure.walk :as walk]
            [clojure.string :as str])
  (:import java.util.Locale))

(declare wrap-transfer my-format get-supplier-by-edrpou get-uuid-by-key get-customer-by-edrpou check-payment-purpose check-receipt-purpose parse-statement)


(defrecord BankAccount [name edrpou bank-mfo-number bank-account-number])
;; debit and credit are BankAccount records

(defprotocol BankStatementType
  (get-type [this])
  (get-purpose [this]))

(defprotocol Taxable
  (tax-uuid [this db]))

(defprotocol Manager
  (parent-url [this db])
  (render->manager [this db]))

(defprotocol Accountable
  (credit-uuid [this db] "credit uuid")
  (debit-uuid [this db] "debit uuid"))

(defn wrap-payment [{:keys [amount credit debit tax date payee refp purpose extra]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "Payee" payee
                 "CreditAccount" (name credit)
                 "Notes" (str "Reference: " refp)
                 "BankClearStatus" "Cleared"
                 "Lines" [{"Account" (name debit)
                           "Description" purpose
                           "Amount" my-amount}]}]
    (cond-> payment
      tax (assoc-in ["Lines" 0 "TaxCode"] tax)
      extra (update "Notes" str " " extra))))

(defn wrap-receipt [{:keys [amount credit debit tax date payer refp purpose]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "Payer" payer
                 "DebitAccount" (name credit)
                 "Notes" (str "Reference: " refp)
                 "BankClearStatus" "Cleared"
                 "Lines" [{"DebitAccount" (name debit)
                           "Description" purpose
                           "Amount" my-amount}]}]
   (cond-> payment
     tax (assoc-in ["Lines" 0 "TaxCode"] tax))))

(defn wrap-transfer [{:keys [amount credit debit tax date payer refp]}]
  (let [my-amount (my-format "%.2f" (if (< 0  amount) amount (- amount)) "en-US")
        payment {"Date" (time.format/unparse (time.format/formatters :date) date)
                 "CreditAccount" credit
                 "DebitAccount" debit
                 "Notes" (str "Reference: " refp)
                 "DebitClearStatus" "Cleared"
                 "CreditClearStatus" "Cleared"
                 "CreditAmount" my-amount}]
    payment))

(defrecord SalaryPayment [debit credit date purpose refp]
  Accountable
  Manager
  (render->manager [this db] (wrap-payment (assoc this
                                            :credit (credit-uuid this db)
                                            :amount (- (:amount this))
                                            :debit (debit-uuid this db)
                                            :payee "Зарплата")))

  (debit-uuid [this db] "")
  (credit-uuid [this db] (:bank-account-uuid @db)))

(defrecord SupplierPayment [debit credit date purpose refp]
  Accountable
  Taxable
  Manager
  (render->manager [this db]
    (wrap-payment (assoc this
                         :credit (credit-uuid this db)
                         :amount (- (get this :amount))
                         :tax (tax-uuid this db)
                         :extra (get-in this [:credit :name])
                         :payee
                         (or
                          (->
                           (get-in this [:credit :edrpou])
                           (get-supplier-by-edrpou db)
                           val
                           (get :Name))
                          (get-in this [:credit :name]))
                         :debit (debit-uuid this db))))

  (tax-uuid [this db] (get @db :tax-uuid))

  (credit-uuid [this db]
    (let [edrpou (get-in this [:credit :edrpou])]
      (key (get-supplier-by-edrpou edrpou db))))

  (debit-uuid [this db] (:bank-account-uuid @db)))

(defrecord OperationalExpensesPayment [debit credit date purpose refp]
 Accountable
 Manager
 (render->manager [this db]
   (wrap-payment (assoc this
                        :credit (credit-uuid this db)
                        :amount (- (get this :amount))
                        :payee "Приватбанк"
                        ;; :tax (tax-uuid this db)
                        :debit (debit-uuid this db))))

 (credit-uuid [this db] (:bank-account-uuid @db))

 (debit-uuid [this db] (get-uuid-by-key :operational-expences-bank db)))

(defrecord AgentIncome [debit credit date purpose refp]
  Accountable
  Taxable
  Manager
  (render->manager [this db]
    (wrap-receipt (assoc this
                         :credit (credit-uuid this db)
                         :amount (get this :amount)
                         :payee (get-in @db [:debit :name])
                         :tax (tax-uuid this db)
                         :debit (debit-uuid this db))))

  (credit-uuid [this db] (:bank-account-uuid @db))

  (tax-uuid [this db] (get @db :tax-uuid))

  (debit-uuid [this db] (get-uuid-by-key :agent-income db)))

(defrecord PhoneExpensesPayment [debit credit date purpose refp]
  Accountable
  Taxable
  Manager
  (render->manager [this db]
    (wrap-payment (assoc this
                         :credit (credit-uuid this db)
                         :amount (- (get this :amount))
                         :tax (tax-uuid this db)
                         :payee "Астелит"
                         :debit (debit-uuid this db))))

  (credit-uuid [this db] (:bank-account-uuid @db))

  (tax-uuid [this db] (get @db :tax-uuid))

  (debit-uuid [this db] (get-uuid-by-key :phone db)))

(defrecord TaxExpenses [debit credit date purpose refp]
  Accountable
  Manager
  (render->manager [this db] (wrap-payment (assoc this
                                                  :credit (credit-uuid this db)
                                                  :amount (- (get this :amount))
                                                  :payee "Налоговая"
                                                  :debit (debit-uuid this db))))

  (credit-uuid [this db] (:bank-account-uuid @db))
  (debit-uuid [this db] (get-uuid-by-key :taxes db)))

(defrecord CustomerReceipt [debit credit date purpose refp]
 Accountable
 Taxable
 Manager
 (render->manager [this db]
   (wrap-receipt (assoc this
                        :credit (credit-uuid this db)
                        :amount (- (get this :amount))
                        :tax (tax-uuid this db)
                        :payer (or
                                (-> (get-in this [:debit :edrpou])
                                    (get-customer-by-edrpou db)
                                    val
                                    (get :Name))
                                (get-in this [:debit :name]))
                        :debit (debit-uuid this db))))

 (tax-uuid [this db] (get @db :tax-uuid))

 (credit-uuid [this db]
   (let [edrpou (get-in this [:debit :edrpou])]
    (key (get-customer-by-edrpou edrpou db))))

 (debit-uuid [this db] (:bank-account-uuid @db)))

(defrecord FromBankTransfer [debit credit date purpose refp]
  Accountable
  Manager
  (render->manager [this db]
    (wrap-transfer (assoc this
                          :credit (credit-uuid this db)
                          :amount (- (get this :amount))
                          :debit (debit-uuid this db))))

  (debit-uuid [this db] (:bank-account-uuid @db))

  (credit-uuid [this db] (:cash-account-uuid @db)))

(defrecord ToBankTransfer [debit credit date purpose refp]
  Accountable
  Manager
  (render->manager [this db]
    (wrap-transfer (assoc this
                          :credit (credit-uuid this db)
                          :amount (- (get this :amount))
                          :debit (debit-uuid this db))))

  (debit-uuid [this db] (:cash-account-uuid @db))

  (credit-uuid [this db] (:bank-account-uuid @db)))

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


(defrecord BankStatement [debit credit amount purpose date refp]
  BankStatementType
  (get-type [this] (get-statement-type this))
  (get-purpose [this] (case (get-type this)
                        :payment (check-payment-purpose (:purpose this))
                        :receipt (check-receipt-purpose (:purpose this)))))

(defn make-bank-statement [m]
  (let [statement (parse-statement m)
        {:keys [amount purpose debit credit date refp]} statement]
    (->BankStatement (map->BankAccount debit) (map->BankAccount credit) amount purpose date refp)))

(defn make-manager-statement [bank-statement]
  (case (get-type bank-statement)
    :payment (case (get-purpose bank-statement)
               :operational-expences-bank (map->OperationalExpensesPayment bank-statement)
               :salary (map->SalaryPayment bank-statement)
               :phone (map->PhoneExpensesPayment bank-statement)
               :transfer (map->FromBankTransfer bank-statement)
               :taxes (map->TaxExpenses bank-statement)
               :supplier (map->SupplierPayment bank-statement))
    :receipt (case (get-purpose bank-statement)
               :transfer (map->ToBankTransfer bank-statement)
               :agent-income (map->AgentIncome bank-statement)
               :customer (map->CustomerReceipt bank-statement))))

(def transform->manager (comp make-manager-statement make-bank-statement))


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

(defn category-by-purpose
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
   {:amount (Float. (get-in statement ["amount" "@amt"]))
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

(defmulti make-manager-statement2 (fn [m _] (select-keys m [:payment :receipt])))

(defmethod make-manager-statement2
  {:payment :operational-expences-bank}
  [statement db]
  (assoc statement
         :payee "Приватбанк"
         :amount (- (get statement :amount))
         :credit (:bank-account-uuid @db)
         :debit (get-uuid-by-key :operational-expences-bank db)))

(defmethod make-manager-statement2
  {:payment :supplier}
  [statement db]
  (assoc statement
         :payee (get-in statement [:credit :name])
         :tax (get @db :tax-uuid)
         :amount (- (get statement :amount))
         :credit (let [edrpou (get-in statement [:credit :edrpou])]
                   (key (get-supplier-by-edrpou edrpou db)))
         :debit (:bank-account-uuid @db)))

(defmethod make-manager-statement2
  {:receipt :customer}
  [statement db]
  (assoc statement
         :payer (get-in statement [:debit :name])
         :tax (get @db :tax-uuid)
         :amount (get statement :amount)
         :debit (let [edrpou (get-in statement [:debit :edrpou])]
                   (key (get-customer-by-edrpou edrpou db)))
         :credit (:bank-account-uuid @db)))

(defmethod make-manager-statement2
  {:receipt :agent-income}
  [statement db]
  (assoc statement
         :debit (get-uuid-by-key :agent-income)
         :credit (:bank-account-uuid @db)
         :payer (get-in statement [:debit :name])))

(defmulti render-manager-statement (fn [m] (-> m
                                                (select-keys [:payment :receipt])
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
               "BankClearStatus" "Cleared"
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
                  category-by-purpose
                  (append-uuid db))
        category-key (statement-account state)]
    (manager.api/api-post2! category-key (transform-statement state) db))) 
