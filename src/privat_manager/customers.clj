(ns privat-manager.customers
  (:require
    [privat-manager.utils :as utils]
    [privat-manager.template :refer [x-panel date-form]]
    [privat-manager.privat.util :as privat.util]
    [privat-manager.privat.auth :as privat.auth]
    [privat-manager.manager.api :as manager.api]
    [clojure.string :as str]
    [clj-time.format :as time.format])
  (:import java.util.Locale))


(defn index [{{{customers :customers} :db :as manager} :manager :as app-db}]
  (let [custom-edrpou (keyword (get-in manager [:uuids :customer-edrpou]))]
    [:div
     [:form {:method :POST}
      [:input.btn.btn-primary {:type :submit :value "Загрузить из Manager"}]
      [:a.btn.btn-default {:href "/settings/load?data=customers"} "Загрузить из кеша"]]
     [:div.col-md-6
      (x-panel "Покупатели из Manager"
               [:table.table
                [:thead
                 [:tr
                  [:th "Название"]
                  [:th "ЕДРПОУ"]]]
                [:tbody
                 (for [customer customers]
                   (let [{n :Name custom :CustomFields} (val customer)
                         edrpou (get custom custom-edrpou)]
                     [:tr
                      [:td [:a {:href (str "/customers/" (-> customer key name))} n]]
                      [:td edrpou]]))]])]
     [:div.col-md-6
      (x-panel "Контрагенты из выписки" (utils/parse-edrpou-statements app-db))]]))


(defn fetch-customers! [app-db]
  (do
    (manager.api/get-index2! :customers app-db)
    (manager.api/populate-db! :customers app-db)))

