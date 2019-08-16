(ns privat-manager.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [privat-manager.core :refer :all]))

(def statement (-> (slurp "test/privat_manager/input.json")
                   (cheshire/parse-string true)))

(deftest test-parse-statement
  (is (= (->  statement privat-manager.privat.parser/parse-statement2 privat-manager.privat.api/unpack-statements)

         ))
  )

(def cleared-statement
  (->  statement
       privat-manager.privat.api/unpack-statements
       ))

(map privat-manager.privat.parser/parse-statement2 cleared-statement)
;; (def statement (walk/keywordize-keys {"@n" "0", "info" {"@export" "Crrp", "@refp" "JBKLH5AO03EQGW", "@number" "605", "@doctype" "p", "@state" "r", "@fldc" "C", "@shorttype" "C", "@postdate" "20170510T12:29:00", "@ref" "JBKLH5AO03EQGW.P", "@refn" "P", "@customerdate" "20170510T00:00:00", "@flinfo" "r"}, "amount" {"@amt" "400.00", "@ccy" "UAH"}, "debet" {"account" {"@name" "БОСНА ЕЛ ДЖI,ТО", "@number" "26000284337001", "customer" {"@crf" "30525505", "bank" {"@code" "321842", "city" "Київ", "#text" "КИЇВСЬКЕ ГРУ ПАТ КБ\"ПРИВАТБАНК\",М.КИЇВ"}}}}, "credit" {"account" {"@name" "АЙ ТI СЕРВIС ПП", "@number" "26002052712918", "customer" {"@crf" "35643866", "bank" {"@code" "300711", "city" "Київ", "#text" "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}}, "purpose" "Iнформацiйно-техн.супровiд за квiтень 2017р згiдно Рах.NББ20 вiд 30.04.2017р У сумi 333.33 грн., ПДВ - 20 % 66.67 грн.", "row" [{"@key" "TRAN_ID", "@level" "1", "#text" "1028799345"} {"@key" "BPL_A_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "BPL_C_BAL", "@level" "1", "#text" "0.00"} {"@key" "BPL_B_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "ACC", "@level" "1", "#text" "26002052712918"} {"@key" "BNK", "@level" "1", "#text" "PB"} {"@key" "BPL_DLR", "@level" "1", "#text" "244049801"} {"@key" "UNAM_PR", "@level" "1", "#text" "Payment Monitor"} {"@key" "BPL_MVTS", "@level" "1"} {"@key" "BPL_SUM_E", "@level" "1", "#text" "400.00"} {"@key" "BPL_APP", "@level" "1", "#text" "KB"} {"@key" "BPL_PR_CLS", "@level" "1", "#text" "P"} {"@key" "BPL_MVT", "@level" "1"}]}))

;; (def statement2 (walk/keywordize-keys {"@n" "4", "info" {"@export" "Crrp", "@refp" "JBKLH5AO03F5F3", "@number" "279", "@doctype" "p", "@state" "r", "@fldc" "C", "@shorttype" "D", "@postdate" "20170510T13:14:00", "@ref" "JBKLH5AO03F5F3.1", "@refn" "1", "@customerdate" "20170510T00:00:00", "@flinfo" "r"}, "amount" {"@amt" "-96.00", "@ccy" "UAH"}, "debet" {"account" {"@name" "АЙ ТI СЕРВIС ПП", "@number" "26002052712918", "customer" {"@crf" "35643866", "bank" {"@code" "300711", "city" "Київ", "#text" "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}}, "credit" {"account" {"@name" "ТОВ \"Кубiк Телеком\"", "@number" "26003174715", "customer" {"@crf" "36386774", "bank" {"@code" "380805", "city" "Київ", "#text" "АТ \"РАЙФФАЙЗЕН БАНК АВАЛЬ\" У М. КИЄВI"}}}}, "purpose" "абонплата за обслуговування в травнi 2017 р. згiдно р/ф N 400 вiд 01.05.2017р.ПДВ-16,00грн", "row" [{"@key" "TRAN_ID", "@level" "1", "#text" "1028875802"} {"@key" "BPL_A_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "BPL_B_MFO_CACC", "@level" "1", "#text" "..."} {"@key" "ACC", "@level" "1", "#text" "26002052712918"} {"@key" "BNK", "@level" "1", "#text" "PB"} {"@key" "BPL_DLR", "@level" "1", "#text" "244066318"} {"@key" "BPL_D_BAL", "@level" "1", "#text" "0.00"} {"@key" "BPL_MVTS", "@level" "1"} {"@key" "BPL_SUM_E", "@level" "1", "#text" "96.00"} {"@key" "BPL_APP", "@level" "1", "#text" "KB"} {"@key" "BPL_PR_CLS", "@level" "1", "#text" "P"} {"@key" "BPL_MVT", "@level" "1"}]}))

;; (def customer1 (walk/keywordize-keys {"account" {"@name" "БОСНА ЕЛ ДЖI,ТО", "@number" "26000284337001", "customer" {"@crf" "30525505", "bank" {"@code" "321842", "city" "Київ", "#text" "КИЇВСЬКЕ ГРУ ПАТ КБ\"ПРИВАТБАНК\",М.КИЇВ"}}}}))
;; (def customer1-acc (:account customer1))

;; (def we (:account (walk/keywordize-keys {"account" {"@name" "АЙ ТI СЕРВIС ПП", "@number" "26002052712918", "customer" {"@crf" "35643866", "bank" {"@code" "300711", "city" "Київ", "#text" "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}})))


;; (deftest test-get-code2
;;   (is (= "30525505" (get-code2 statement))))

;; (deftest test-get-date
;;   (is (= (clj-time.core/date-time 2017 05 10) (get-date statement))))
; преобразовать дату в американский формат
; DebitAccount - откуда ушли деньги
; "Payer" 
; "Lines" : [ { "Account" "Amount" "Invoice"}]
;; {:@n "11",
;;  :info
;;  {:@refp "JBKLH5AO03F5GH",
;;   :@number "275",
;;   :@shorttype "D",
;;   :@refn "1",
;;   :@doctype "p",
;;   :@flinfo "r",
;;   :@export "Crrp",
;;   :@state "r",
;;   :@postdate "20170510T13:15:00",
;;   :@ref "JBKLH5AO03F5GH.1",
;;   :@fldc "C",
;;   :@customerdate "20170510T00:00:00"},
;;  :amount {:@amt "-500.00", :@ccy "UAH"},
;;  :debet
;;  {:account
;;   {:@name "АЙ ТI СЕРВIС ПП",
;;    :@number "26002052712918",
;;    :customer
;;    {:@crf "35643866",
;;     :bank
;;     {:@code "300711",
;;      :city "Київ",
;;      :#text "ПЕЧЕРСЬКА Ф.ПАТ КБ\"ПРИВАТБАНК\", М.КИЇВ"}}}},
;;  :credit
;;  {:account
;;   {:@name "ТОВ \"лайфселл\"",
;;    :@number "26001011816320",
;;    :customer
;;    {:@crf "22859846",
;;     :bank
;;     {:@code "300023", :city "Київ", :#text "ПАТ \"УКРСОЦБАНК\""}}}},
;;  :purpose
;;  "оплата за телекомунiкацiйнi послуги по договору N 202536606 згiдно з мiсячним рахунком  вiд 30.04.2017 р. ПДВ - 83,33 грн.",}
;; {
;;  "Date": "2016-04-05",
;;  "DebitAccount": "f6c58998-557d-4eff-a31b-c9fcc2161e95",
;;  "Lines": [
;;            {
;;             "Account": "039e76cf-9361-48b6-9ab5-978ed094054d",
;;             "Amount": 9862.0,
;;             "Invoice": "3df05968-64a7-4bdb-b123-8f3cbad33e4d"}
;;            ,]
;;  "Payer": "РњР°Р»С‚СЋСЂРѕРї",
;;  "BankClearDate": "2016-04-05",
;;  "BankClearStatus": "Cleared"}
