(ns privat-manager.settings
  (:require [privat-manager.config :as config]
            [privat-manager.privat.auth :as privat.auth]
            [privat-manager.template :refer [x-panel]]
            [privat-manager.utils :as utils]
            [clj-time.format :as time.format]
            [clj-time.coerce :as time.coerce])
  (:import java.util.Locale))

(defn business-info [{privat :privat}]
   (let [{:keys [business-id privat-id privat-token]} privat]
     [:div
      [:h3 "Доступ для " business-id]
      [:p "client id " (:privat-id privat)]
      [:p "client token " (when  privat-token "существует")]]))

(defn index [app-db]
  (let [f (fn [e] [:option {:value e :selected (= e (:business-id @app-db))} e])
        {roles :roles session :session} (:privat @app-db)
        manager-uri config/manager-endpoint]
    [:div
     [:h1 "Настройки"]
     [:div.col-md-6
      (x-panel
       "Конфигурация предприятия"
       [:form {:method :post}
        [:select.form-control {:name "account"}
         (map f @config/config-set)]
        [:br]
        [:input.btn.btn-primary {:type :submit :value "Загрузить"}]])
      (x-panel "Приват24 API"
               (business-info @app-db))]
     [:div.col-md-6
      (x-panel [:h2 "Manager API" ]
               [:h4 [:a {:href manager-uri} manager-uri]]
               )
      (x-panel "Управление Manager"
               [:div
                [:h2 "Загрузить из кеша"]
                [:a.btn.btn-default {:href "/settings/load?data=customers"} "Покупателей"]
                [:a.btn.btn-default {:href "/settings/load?data=suppliers"} "Поставщиков"]
                [:div.divider]
                [:h2 "Сохранить кеш"]
                [:a.btn.btn-danger {:href "/settings/save?data=customers"} "Покупателей"]
                [:a.btn.btn-danger {:href "/settings/save?data=suppliers"} "Поставщиков"]])]
     ]))

(defn load-account! [account app-db]
  (when-let [conf (@config/config-set account)]
    (do
      (config/load-settings! account app-db)
      (config/load-cached-db :customers app-db)
      (config/load-cached-db :suppliers app-db)
      {:flash { :success "Настройки успешно загружены!"}})))
