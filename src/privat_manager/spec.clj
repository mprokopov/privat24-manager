(ns privat-manager.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [privat-manager.privat.api :as privat.api]
            ))
;; ----- Statements ------
(s/def ::non-blank-string (s/and string? (complement empty?)))
(s/def ::AUT_CNTR_NAM ::non-blank-string)
(s/def ::AUT_CNTR_CRF ::non-blank-string)
(s/def ::AUT_CNTR_MFO ::non-blank-string)
(s/def ::AUT_CNTR_ACC ::non-blank-string)
(s/def ::AUT_MY_NAM ::non-blank-string)
(s/def ::AUT_MY_CRF ::non-blank-string)
(s/def ::AUT_MY_MFO ::non-blank-string)
(s/def ::AUT_MY_ACC ::non-blank-string)
(s/def ::BPL_OSND ::non-blank-string)
(s/def ::BPL_SUM ::non-blank-string)
(s/def ::TRANTYPE ::non-blank-string)
(s/def ::DATE_TIME_DAT_OD_TIM_P ::non-blank-string)
(s/def ::ID ::non-blank-string)

(s/def ::creditor (s/keys :req-un [::AUT_CNTR_NAM ::AUT_CNTR_CRF ::AUT_CNTR_MFO ::AUT_CNTR_ACC]))

(s/def ::debitor (s/keys :req-un [::AUT_MY_NAM ::AUT_MY_CRF ::AUT_MY_MFO ::AUT_MY_ACC]))

(s/def ::transaction-keys (s/merge ::creditor ::debitor (s/keys :req-un [::ID ::BPL_OSND ::TRANTYPE ::DATE_TIME_DAT_OD_TIM_P ::BPL_SUM])))

(s/def ::transaction (s/map-of keyword? ::transaction-keys))

(s/def ::statements (s/coll-of ::transaction))

(s/def ::StatementsResponse (s/keys :req-un [::statements]))

(s/def ::response (s/keys :req-un [::StatementsResponse]))

;; ----- Rests -------
(s/def ::string-digit (s/and string? #(re-matches #"\d+(\.\d+)?" %)))

(s/def ::turnoverCred ::string-digit)
(s/def ::turnoverDept ::string-digit)
(s/def ::balanceIn ::string-digit)
(s/def ::balanceOut ::string-digit)
(s/def ::balance-keys (s/keys :req-un [::turnoverCred ::turnoverDebt ::balanceIn ::balanceOut]))
(s/def ::balance-entry (s/map-of keyword? ::balance-keys))
(s/def ::balanceResponse (s/coll-of ::balance-entry))
(s/def ::rests (s/keys :req-un [::balanceResponse]))

(comment
  (def rests (read-string (slurp "rests.edn")))
  (s/valid? ::rests rests)

  (s/valid? ::string-digit "12323.23a")

  (s/explain-str ::rests rests)
)

(comment

(spit "transactions.edn" statements)
(def statements (privat.api/get-statements (:privat @dev/app-db) "01-06-2020" "06-06-2020"))

(def statements (read-string (slurp "transactions.edn")))

(s/valid? ::response statements)

(s/explain ::response statements)


(def ex1 {:JBKLK62O3CZTLI
{:ID "750824643",
  :TECHNICAL_TRANSACTION_ID "111111111_online",
  :BPL_DLR "XXXXXXXX",
  :AUT_MY_NAM "NAME",
  :DATE_TIME_DAT_OD_TIM_P "02.06.2020 10:34:00",
  :BPL_SUM_E "1050.00",
  :BPL_PR_PR "r",
  :AUT_CNTR_CRF "11111111",
  :AUT_CNTR_MFO "111119",
  :AUT_MY_CRF "3111511111",
  :BPL_REF "JBKLK65O3CZTLI",
  :BPL_DAT_KL "02.06.2020",
  :AUT_CNTR_ACC "UA111026890000021008051111111",
  :BPL_TIM_P "10:34",
  :AUT_MY_MFO_NAME "ПЕЧЕРСЬКА ФІЛІЯ АТ КБ \"ПРИВАТБАНК\"",
  :BPL_CCY "UAH",
  :AUT_CNTR_MFO_NAME "ВІННИЦЬКА ФІЛІЯ АТ КБ \"ПРИВАТБАНК\"",
  :BPL_DAT_OD "02.06.2020",
  :BPL_OSND
  "BLA BLA",
  :AUT_MY_ACC "UA011007110000026001111111116",
  :AUT_MY_MFO "100111",
  :BPL_REFN "P",
  :AUT_CNTR_NAM "",
  :BPL_FL_DC "C",
  :BPL_DOC_TYP "p",
  :BPL_SUM "1050.00",
  :TRANTYPE "C",
  :BPL_NUM_DOC "4243",
  :BPL_FL_REAL "r"}})

(s/valid? ::transaction ex1)

(s/explain-str ::transaction ex1)

(s/explain-data ::transaction ex1)

(s/explain ::transaction ex1)

(privat-manager.privat.parser/privat-statement-debit (val (first ex1)))

(gen/sample (s/gen ::transaction))

(s/exercise ::transaction 10)
)
