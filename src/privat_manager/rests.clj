(ns privat-manager.rests
  (:require
   [privat-manager.privat.util :as privat.util]
   [privat-manager.privat.parser :as privat.parser]
   [privat-manager.utils :as util]
   [privat-manager.privat.api :as privat.api]
   [privat-manager.privat.auth :as privat.auth]
   [privat-manager.template :refer [date-form]]
   [clj-time.format :as time.format]))


(defn index [{{{rests :rests} :db} :manager :as app-db}]
    [:div
      [:h1 "Остатки"]
      (if (privat.auth/authorized? app-db)
        (date-form)
        [:div.alert.alert-danger "Необходимо иметь токен для загрузки остатков. Создайте автоклиент."])
      (when rests
        [:table.table.table-striped.table-hover
          [:tr
            [:th "Дата"]
            [:th "Ушло"]
            [:th "Пришло"]
            [:th "Вх. остаток"]
            [:th "Исх. остаток"]]
          (for [r rests
                :let [{:keys [date inrest outrest debit credit]} (util/format-floats r)]]
            [:tr
              [:td (time.format/unparse (time.format/formatter "dd MMMM YYYY") date)]
              [:td debit]
              [:td credit]
              [:td inrest]
              [:td outrest]])])]) 

(defn fetch! [app-db stdate endate]
  (try
    (swap! app-db assoc-in [:manager :db :rests]
           (->>
            (privat.api/get-rests (:privat @app-db) stdate endate)
            :balanceResponse
            (mapv privat.parser/privat-rest)
            (sort-by :date)
            reverse))

    { :flash { :success "Остатки успешно загружены"}}
    (catch AssertionError e {:flash { :error (.getMessage e)}})
    ))

;; (fetch! dev/app-db "01-08-2019" "02-08-2019")
;; (-> (privat.api/get-rests (:privat @dev/app-db) "01-08-2019" "02-08-2019")
;;      :balanceResponse
;;      first
;;      privat.parser/privat-rest)
