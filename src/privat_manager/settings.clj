(ns privat-manager.settings
  (:require [privat-manager.config :as config]
            [privat-manager.privat.auth :as privat.auth]
            [privat-manager.template :refer [x-panel]]
            [privat-manager.utils :as utils]
            [clj-time.format :as time.format]
            [clj-time.coerce :as time.coerce])
  (:import java.util.Locale))

(defn privat-session-info [{privat-session :privat}]
  (let [{{roles :roles expires :expires :as session} :session bid :business-id} privat-session
        formatter (time.format/with-locale (time.format/formatter "HH:mm dd MMMM YYYY") (Locale. "ru"))
        expire (->> (* 1000 expires)
                    time.coerce/from-long
                    (time.format/unparse formatter))] 
    [:div
     [:h2 "Бизнес: " bid]
     [:h4 "Роли: " (clojure.string/join ", " roles)]
     [:p expire]
     [:p (utils/map-to-html-list session)]
     [:a.btn.btn-default {:href "/auth/logout"} "Выйти"]]))

(defn login-form [app-db]
  [:form {:action "/auth/login" :method :POST}
   [:h3 (str "Доступ к " (get-in app-db [:privat :login]))]
   [:input.btn.btn-primary {:type :submit :value "Пройти авторизацию Privat24"}]])

(defn send-otp-form [{{{otp-persons :message} :session} :privat :as app-db}]
  (let [link (fn [{id :id number :number}]
               [:li
                [:a {:href (format "/auth/login/otp?id=%s" id)} number]])]
    [:div
     (privat-session-info app-db)
     [:h3 "Отправить OTP"]
     [:ul
       (map link otp-persons)]]))

(defn check-otp-form [app-db]
  [:form.form-horizontal {:action "/auth/login/otp" :method :post}
   [:div.form-group
    [:label.control-label "OTP"
      [:input {:type :text :name :otp}]]
    [:div.ln_solid]
    [:div.controls
     [:input.btn {:type :submit :value "Отправить"}]]]])

(defn index [app-db]
  (let [f (fn [e] [:option {:value e :selected (= e (:business-id @app-db))} e])
        {roles :roles session :session} (:privat @app-db)
        manager-uri config/manager-endpoint]
     [:div
        [:h1 "Настройки"]
        [:div.col-md-4
         (x-panel
          "Конфигурация предприятия"
          [:form {:method :post}
           [:select.form-control {:name "account"}
            (map f config/config-set)]
           [:br]
           [:input.btn.btn-primary {:type :submit :value "Загрузить"}]])]
        [:div.col-md-4
         (x-panel "Управление Manager"
          [:div
           [:h2 "Manager API" ] [:p [:a {:href manager-uri} manager-uri]]
           [:h2 "Загрузить из кеша"]
           [:a.btn.btn-default {:href "/settings/load?data=customers"} "Покупателей"]
           [:a.btn.btn-default {:href "/settings/load?data=suppliers"} "Поставщиков"]
           [:div.divider]
           [:h2 "Сохранить кеш"]
           [:a.btn.btn-danger {:href "/settings/save?data=customers"} "Покупателей"]
           [:a.btn.btn-danger {:href "/settings/save?data=suppliers"} "Поставщиков"]])]
        [:div.col-md-4
         (x-panel "Приват24"
          (if (:privat @app-db)
            (if (privat.auth/logged? @app-db) 
              (if (privat.auth/authorized? @app-db)
                (privat-session-info @app-db)
                (if (privat.auth/otp-sent? @app-db)
                  (check-otp-form @app-db)
                  (send-otp-form @app-db)))
                ;[:h3 "OTP" (map utils/map-to-html-list (get-in @app-db [:privat :session :message]))])
              (login-form @app-db))
            "не загружены настройки"))]]))

(defn load-account! [account app-db]
  (when-let [conf (config/config-set account)]
    (do
      (config/load-settings! account app-db)
      (config/load-cached-db :customers app-db)
      (config/load-cached-db :suppliers app-db)
      {:flash "Настройки успешно загружены!"})))
