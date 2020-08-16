(ns privat-manager.privat.parser-test
  (:require [privat-manager.privat.parser :as sut]
            [clojure.test :refer :all]
            [privat-manager.config :as config]))

(defn load-payment-purposes [f]
  (reset! config/payment-purposes 
         {
          "РКО" :operational-expences-bank
          "Выдача наличных средств" :transfer
          })
  (f))

(defn load-receipt-purposes [f]
  (reset! config/receipt-purposes
         {
          "Агентська виногорода" :agent-income
          })
  (f))

(use-fixtures :each load-payment-purposes load-receipt-purposes)

(deftest payment-purposes-test
  (testing "wrong payment purpose returns :supplier"
    (is (= :supplier
           (sut/payment-purpose "Вasdfasdfasdf"))))
  (is (= :operational-expences-bank
         (sut/payment-purpose "РКО")))
  (is (= :transfer
         (sut/payment-purpose "Выдача наличных средств"))))

(deftest receipt-purposes-test
  (is (= :customer
         (sut/receipt-purpose "asdfasdfasdf")))
  (is (= :agent-income
         (sut/receipt-purpose "Агентська виногорода"))))
