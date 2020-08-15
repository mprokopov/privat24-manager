(ns privat-manager.privat.util-test
  (:require 
            [privat-manager.manager.api :as manager.api]
            ))

(deftest test-header-location
  (is
   (=
    "f1a582f4-537b-4159-82ff-5d1d76e88918"
    (manager.api/parse-header-location 
     "/api/SVQtUHJlbWl1bQ/f1a582f4-537b-4159-82ff-5d1d76e88918.json"))))

(deftest test-paymant-view-link
  (is
   (= "http://192.168.180.31:8080/receipt-or-payment-view?Key=0a89cc07-dce0-4f49-b35f-865daee3c024&FileID=SVQtUHJlbWl1bQ"
      (manager.api/payment-view-link "0a89cc07-dce0-4f49-b35f-865daee3c024" "SVQtUHJlbWl1bQ"))))
