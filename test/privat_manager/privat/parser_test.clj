(ns privat-manager.privat.parser-test
  (:require [privat-manager.privat.parser :as sut]
            [clojure.test :refer :all]))


(deftest payment-purposes-test
  (is (= :supplier
         (sut/payment-purpose "Вasdfasdfasdf")))
  (is (= :operational-expences-bank
         (sut/payment-purpose "РКО")))
  (is (= :transfer
         (sut/payment-purpose "Выдача наличных средств"))))

(deftest receipt-purposes-test
  (is (= :customer
         (sut/receipt-purpose "asdfasdfasdf")))
  (is (= :agent-income
         (sut/receipt-purpose "Агентська виногорода"))))
