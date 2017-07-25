(ns privat-manager.suppliers
  (:require
   [privat-manager.utils :as utils]
   [privat-manager.template :refer [x-panel date-form]]
   [privat-manager.privat.util :as privat.util]
   [privat-manager.privat.auth :as privat.auth]
   [privat-manager.manager.api :as manager.api]
   [clojure.string :as str]))


(defn index [{{{suppliers :suppliers} :db :as manager} :manager :as app-db}]
  (let [{{edrpou-uuid :supplier-edrpou} :uuids} manager
        supplier-edrpou (keyword edrpou-uuid)]
    [:div
     [:form {:method :POST}
      [:input.btn.btn-primary {:type :submit :value "Загрузить из Manager"}]
      [:a.btn.btn-default {:href "/settings/load?data=suppliers"} "Загрузить из кеша"]]
     [:div.col-md-6
      (x-panel "Покупатели из Manager"
               [:table.table
                [:thead
                 [:tr
                  [:th "Название"]
                  [:th "ЕДРПОУ"]]]
                [:tbody
                 (for [supplier suppliers]
                   (let [[uuid {n :Name custom-fields :CustomFields}] supplier
                         edrpou (get custom-fields supplier-edrpou)
                         supplier-url (str "/suppliers/" (name uuid))]
                     [:tr
                      [:td [:a {:href supplier-url} n]] 
                      [:td edrpou]]))]])]
     [:div.col-md-6
      (x-panel "Контрагенты из выписки" (utils/parse-edrpou-statements app-db))]]))

(defn fetch! [app-db]
  (do
    (manager.api/get-index2! :suppliers app-db)
    (manager.api/populate-db! :suppliers app-db)))

(defn form [uuid {edrpou :edrpou}]
  [:form.form-horizontal {:method :POST}
   [:div.form-group
    [:div
     [:label.col-md-2.control-label "ЕДРПОУ"
      [:input#edrpou {:type :text :name "edrpou" :value edrpou}]]]]
   [:div.ln_solid]
   [:div.controls
    [:input.btn.btn-primary {:type :submit :value "Сохранить"}]]])


(defn single [supplier-uuid {{{suppliers :suppliers} :db uuids :uuids} :manager}]
  (let [{n :Name :as supplier} (get suppliers (keyword supplier-uuid))
        edrpou (get-in supplier [:CustomFields (keyword (:supplier-edrpou uuids))])]
    [:div.col-md-6
     (x-panel (str "Поставщик " n)
              [:div
               (form supplier-uuid {:edrpou edrpou})])]))

(defn update! [uuid {edrpou :edrpou} app-db]
  (let [{{{supplier-edrpou-uuid :supplier-edrpou} :uuids} :manager} @app-db
        edrpou-uuid-key (keyword supplier-edrpou-uuid)
        update-map (assoc-in (manager.api/fetch-uuid-item! uuid app-db) [:CustomFields edrpou-uuid-key] edrpou)
        {status :status} (manager.api/api-update! uuid update-map app-db)]
    (if (= status 200)
      [:p "Updated"
       (privat-manager.utils/map-to-html-list update-map)]
      [:p "Error occured"])))

