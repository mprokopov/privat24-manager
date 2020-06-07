(ns privat-manager.privat.util-test
  (:require [privat-manager.privat.util :refer :all :as putil]
            [privat-manager.privat.parser :refer :all]
            [cheshire.core :as cheshire]
            [clojure.test :refer :all]
            [privat-manager.manager.api :as mapi]
            [clojure.walk :as walk]
            [privat-manager.config :as config]))

;; (def statement1 (walk/keywordize-keys {"@n" "0", "info" {"@export" "Crrp", "@refp" "JBKLH5AO03EQGW", "@number" "605", "@doctype" "p", "@state" "r", "@fldc" "C", "@shorttype" "C", "@postdate" "20170510T12:29:00", "@ref" "JBKLH5AO03EQGW.P", "@refn" "P", "@customerdate" "20170510T00:00:00", "@flinfo" "r"}, "amount" {"@amt" "400.00", "@ccy" "UAH"}, "debet" {"account" {"@name" "БОСНА ЕЛ ДЖI,ТО", "@number" "26000284337001", "customer" {"@crf" "30525505", "bank" {"@code" "321842", "city" "Київ", "#text" "КИЇВСЬКЕ ГРУ ПАТ КБ\"ПРИВАТБАНК\",М.КИЇВ"}}}}, "credit" {"account" {"@name" "АЙ ТI СЕРВIС ПП", "@number" "26002052712918", "customer" {"@crf" "35643866", "bank" {"@code" "300711", "city" "Київ", "#text" "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}}, "purpose" "Iнформацiйно-техн.супровiд за квiтень 2017р згiдно Рах.NББ20 вiд 30.04.2017р У сумi 333.33 грн., ПДВ - 20 % 66.67 грн.", "row" [{"@key" "TRAN_ID", "@level" "1", "#text" "1028799345"} {"@key" "BPL_A_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "BPL_C_BAL", "@level" "1", "#text" "0.00"} {"@key" "BPL_B_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "ACC", "@level" "1", "#text" "26002052712918"} {"@key" "BNK", "@level" "1", "#text" "PB"} {"@key" "BPL_DLR", "@level" "1", "#text" "244049801"} {"@key" "UNAM_PR", "@level" "1", "#text" "Payment Monitor"} {"@key" "BPL_MVTS", "@level" "1"} {"@key" "BPL_SUM_E", "@level" "1", "#text" "400.00"} {"@key" "BPL_APP", "@level" "1", "#text" "KB"} {"@key" "BPL_PR_CLS", "@level" "1", "#text" "P"} {"@key" "BPL_MVT", "@level" "1"}]}))

;; (def statement2 (walk/keywordize-keys {"@n" "4", "info" {"@export" "Crrp", "@refp" "JBKLH5AO03F5F3", "@number" "279", "@doctype" "p", "@state" "r", "@fldc" "C", "@shorttype" "D", "@postdate" "20170510T13:14:00", "@ref" "JBKLH5AO03F5F3.1", "@refn" "1", "@customerdate" "20170510T00:00:00", "@flinfo" "r"}, "amount" {"@amt" "-96.00", "@ccy" "UAH"}, "debet" {"account" {"@name" "АЙ ТI СЕРВIС ПП", "@number" "26002052712918", "customer" {"@crf" "35643866", "bank" {"@code" "300711", "city" "Київ", "#text" "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}}, "credit" {"account" {"@name" "ТОВ \"Кубiк Телеком\"", "@number" "26003174715", "customer" {"@crf" "36386774", "bank" {"@code" "380805", "city" "Київ", "#text" "АТ \"РАЙФФАЙЗЕН БАНК АВАЛЬ\" У М. КИЄВI"}}}}, "purpose" "абонплата за обслуговування в травнi 2017 р. згiдно р/ф N 400 вiд 01.05.2017р.ПДВ-16,00грн", "row" [{"@key" "TRAN_ID", "@level" "1", "#text" "1028875802"} {"@key" "BPL_A_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "BPL_B_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "ACC", "@level" "1", "#text" "26002052712918"} {"@key" "BNK", "@level" "1", "#text" "PB"} {"@key" "BPL_DLR", "@level" "1", "#text" "244066318"} {"@key" "BPL_D_BAL", "@level" "1", "#text" "0.00"} {"@key" "BPL_MVTS", "@level" "1"} {"@key" "BPL_SUM_E", "@level" "1", "#text" "96.00"} {"@key" "BPL_APP", "@level" "1", "#text" "KB"} {"@key" "BPL_PR_CLS", "@level" "1", "#text" "P"} {"@key" "BPL_MVT", "@level" "1"}]}))

(def app-db (atom nil))

(def statements (-> (slurp "test/privat_manager/input.json")
                    (cheshire/parse-string true)))

(def cleared-statements
  (->  statements
       privat-manager.privat.api/unpack-statements))

(def sample-statement (-> cleared-statements
                          first
                          privat-manager.privat.parser/parse-statement))

(deftest test-parse-statement
  (is (= (:refp sample-statement)
         :JBKLJ82O2D4EXO)))

(deftest test-amount-statement
  (is (= (:amount sample-statement)
         1050.00)))

(deftest test-purpose-statement
  (is (= (:purpose sample-statement)
         "За консультування з питань iнформатизацiї зг.рах.Nб/н вiд 31.07.2019р. Без ПДВ")))

;; (deftest test-date-statement
;;   (is (= (:date sample-statement)
;;          (.DateTime parse "02.08.2019 10:30:00"))))

(deftest test-transaction-type-statement
  (is (= (:transaction-type sample-statement)
         "C")))

;; (deftest test-parse-statement
;;   (is (=
;;           {:amount 400.0,
;;            :refp "JBKLH5AO03EQGW",
;;            :purpose
;;            "Iнформацiйно-техн.супровiд за квiтень 2017р згiдно Рах.NББ20 вiд 30.04.2017р У сумi 333.33 грн., ПДВ - 20 % 66.67 грн.",
;;            :debit
;;            {:name "БОСНА ЕЛ ДЖI,ТО",
;;              :edrpou "30525505",
;;              :bank-mfo-number "321842",
;;              :bank-account-number "26000284337001"},
;;            :credit
;;            {:name "АЙ ТI СЕРВIС ПП",
;;              :edrpou "35643866",
;;              :bank-mfo-number "300711",
;;              :bank-account-number "26002052712918"}}
;;           (select-keys (parse-statement statement1) [:amount :refp :debit :credit :purpose])))
  ;; (is (= (select-keys (parse-statement statement2) [:amount :refp :debit :credit :purpose])
  ;;        {:amount -96.0,
  ;;         :refp "JBKLH5AO03F5F3",
  ;;         :debit
  ;;         {:name "АЙ ТI СЕРВIС ПП",
  ;;          :edrpou "35643866",
  ;;          :bank-mfo-number "300711",
  ;;          :bank-account-number "26002052712918"},
  ;;         :credit
  ;;         {:name "ТОВ \"Кубiк Телеком\"",
  ;;          :edrpou "36386774",
  ;;          :bank-mfo-number "380805",
  ;;          :bank-account-number "26003174715"},
  ;;         :purpose
  ;;         "абонплата за обслуговування в травнi 2017 р. згiдно р/ф N 400 вiд 01.05.2017р.ПДВ-16,00грн"})))

(deftest test-assoc-transaction-type
  (is (= (select-keys  (assoc-transaction-type sample-statement) [:receipt :payment])
         {:receipt :customer})))

;; (is (= (select-keys  (assoc-transaction-type (parse-statement statement2)) [:receipt :payment])
;;        {:payment :supplier})))


;; (deftest test-category-key
;;   (is (= (mapi/category-key (-> sample-statement parse-statement assoc-transaction-type))

;;          :bank-receipts))
;;   (is (= (mapi/category-key (-> sample-statement parse-statement assoc-transaction-type))
;;          :bank-payments)))


;; (deftest test-check-purpose
;;   (is (= (payment-purpose (-> statement1 parse-statement :purpose))
;;          :supplier))
;;   (is (= (receipt-purpose (-> statement2 parse-statement :purpose))
;;          :customer)))


;; (config/load-settings! "itservice" app-db)
;; (config/load-cached-db :customers app-db)
;; (config/load-cached-db :suppliers app-db)

;; (deftest test-make-statement
;;   (is
;;     (= (-> statement1
;;           parse-statement
;;           assoc-transaction-type
;;           (mapi/statement @app-db)
;;           (select-keys [:amount :receipt :comment]))
;;       {
;;         :amount 400.0
;;         :receipt :customer
;;         :comment "оплата покупателя"}))
;;   (is
;;     (= (-> statement2
;;           parse-statement
;;           assoc-transaction-type
;;           (mapi/statement @app-db)
;;           (select-keys [:amount :payment :comment]))
;;       {
;;         :amount 96.0
;;         :payment :supplier
;;         :comment "оплата поставщику"})))
;;         ;:payer "Босна Ел Джи"}))


;; (deftest test-render-manager
;;   (is
;;    (= (-> sample-statement
;;           parse-statement
;;           assoc-transaction-type
;;           (mapi/statement @app-db)
;;           render-manager-statement)
;;       {"BankAccount" "af5a0cb8-5e91-47be-92ac-52974ba7dbc4",
;;        "Date" "2017-05-10",
;;        "Lines"
;;        [{"Amount" "96.00",
;;          "Account" "daf5b193-b1ad-4a87-91af-703e9386ad16",
;;          "Description"
;;          "абонплата за обслуговування в травнi 2017 р. згiдно р/ф N 400 вiд 01.05.2017р.ПДВ-16,00грн",
;;          "TaxCode" "5f1d5dfa-51f4-4b9f-aaa0-bd7b3db3de34"}],
;;        "Payee" "Кубик Телеком",
;;        "BankClearStatus" "Cleared",
;;        "Notes" "Reference: JBKLH5AO03F5F3 ТОВ \"Кубiк Телеком\""}))
;;   (is
;;    (= (-> sample-statement
;;           parse-statement
;;           assoc-transaction-type
;;           (mapi/statement @app-db)
;;           render-manager-statement)
;;       {"Payer" "Босна LG",
;;        "Date" "2017-05-10",
;;        "Lines"
;;        [{"Amount" "400.00",
;;          "Account" "e6b297f3-61f2-49b8-955d-45618c40c16b",
;;          "TaxCode" "5f1d5dfa-51f4-4b9f-aaa0-bd7b3db3de34",
;;          "Description",
;;          "Iнформацiйно-техн.супровiд за квiтень 2017р згiдно Рах.NББ20 вiд 30.04.2017р У сумi 333.33 грн., ПДВ - 20 % 66.67 грн."}],
;;        "BankClearStatus" "Cleared",
;;        "BankAccount" "af5a0cb8-5e91-47be-92ac-52974ba7dbc4",
;;        "Notes" "Reference: JBKLH5AO03EQGW"})))
