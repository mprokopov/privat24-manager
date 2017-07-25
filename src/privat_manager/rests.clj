(ns privat-manager.rests
  (:require
   [privat-manager.privat.util :as privat.util]
   [privat-manager.privat.api :as privat.api]
   [privat-manager.template :refer [date-form]]
   [clj-time.format :as time.format]))


(defn index [app-db]
  (let [rests (get-in @app-db [:manager :db :rests])]
    [:div
      (date-form)
      [:table.table
        [:tr
          [:th "Дата"]
          [:th "Ушло"]
          [:th "Пришло"]
          [:th "Вх. остаток"]
          [:th "Исх. остаток"]]
        (for [r rests
              :let [{:keys [date inrest outrest debit credit]} (privat.util/format-floats r)]]
          [:tr
            [:td (time.format/unparse (time.format/formatter "dd MMMM YYYY") date)]
            [:td debit]
            [:td credit]
            [:td inrest]
            [:td outrest]])]])) 

(defn fetch-rests! [app-db stdate endate]
  (swap! app-db assoc-in [:manager :db :rests]
         (->>
          (privat.api/get-rests (:privat @app-db) stdate endate)
          (mapv privat.util/parse-rest)
          (sort-by :date)
          reverse)))
