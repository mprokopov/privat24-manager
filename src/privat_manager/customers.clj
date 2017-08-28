(ns privat-manager.customers
  (:require
    [privat-manager.utils :as utils]
    [privat-manager.template :refer [x-panel date-form]]
    [privat-manager.privat.util :as privat.util]
    [privat-manager.privat.auth :as privat.auth]
    [privat-manager.manager.api :as manager.api]
    [clojure.string :as str]))

(defn index [{{{customers :customers} :db :as manager} :manager :as app-db}]
  (let [{{edrpou-uuid :customer-edrpou} :uuids} manager
        customer-edrpou (keyword edrpou-uuid)]
    [:div
     [:h1 "Покупатели"]
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
                   (let [[uuid {n :Name custom-fields :CustomFields}] customer
                         edrpou (get custom-fields customer-edrpou)
                         customer-url (str "/customers/" (name uuid))]
                     [:tr
                      [:td [:a {:href customer-url} n]] 
                      [:td edrpou]]))]])]
     [:div.col-md-6
      (x-panel "Контрагенты из выписки" (utils/parse-edrpou-statements app-db))]]))


(defn fetch! [app-db]
  (do
    (manager.api/get-index2! :customers app-db)
    (manager.api/populate-db! :customers app-db)
    {:flash "База покупателей успешно загружена"}))


(defn form [uuid {edrpou :edrpou}]
  [:form.form-horizontal {:method :POST}
   [:div.form-group
    [:div
     [:label.col-md-2.control-label "ЕДРПОУ"
      [:input#edrpou {:type :text :name "edrpou" :value edrpou}]]]]
   [:div.ln_solid]
   [:div.controls
    [:input.btn.btn-primary {:type :submit :value "Сохранить"}]]])


(defn single [customer-uuid {{{customers :customers} :db uuids :uuids} :manager}]
  (let [{n :Name :as customer} (get customers (keyword customer-uuid))
        edrpou (get-in customer [:CustomFields (keyword (:customer-edrpou uuids))])]
     [:div.col-md-6
      (x-panel (str "Покупатель " n)
               [:div
                (form customer-uuid {:edrpou edrpou})])]))


(defn update! [uuid {edrpou :edrpou} app-db]
  (let [{{{customer-edrpou-uuid :customer-edrpou} :uuids} :manager} @app-db
        edrpou-uuid-key (keyword customer-edrpou-uuid)
        update-map (assoc-in (manager.api/fetch-uuid-item! uuid app-db) [:CustomFields edrpou-uuid-key] edrpou)
        {status :status} (manager.api/api-update! uuid update-map app-db)]
     (if (= status 200)
      [:p "Updated"
       (privat-manager.utils/map-to-html-list update-map)]
      [:p "error occured"])))
      
