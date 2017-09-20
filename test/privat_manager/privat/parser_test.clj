(ns privat-manager.privat.parser-test
  (:require [privat-manager.privat.parser :as sut]
            [clojure.test :refer :all]))


(deftest payment-purposes-test
  (is (=
       (sut/payment-purpose "Вasdfasdfasdf")
       :supplier))
  (is (=
       (sut/payment-purpose "РКО")
       :operational-expences-bank))
  (is (=
       (sut/payment-purpose "Выдача наличных средств")
       :transfer)))

(deftest receipt-purposes-test
  (is (= (sut/receipt-purpose "asdfasdfasdf")
         :customer))
  (is (=
       (sut/receipt-purpose "Агентська виногорода")
       :agent-income)))
