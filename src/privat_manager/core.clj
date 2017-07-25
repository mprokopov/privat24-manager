(ns privat-manager.core
  (:require [cheshire.core :as cheshire]
            [privat-manager.privat.api :as privat.api]
            [privat-manager.privat.auth :as privat.auth]
            [privat-manager.privat.util :as privat.util]
            [privat-manager.utils :as utils]
            [privat-manager.template :refer [x-panel privat-session-info sidebar-menu date-form]]
            [privat-manager.manager.api :as manager.api]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.coercions :refer [as-int as-uuid]]
            [clj-time.core :as time]
            [clj-time.coerce :as time.coerce]
            [privat-manager.config :as config]
            [clojure.walk :as walk]
            [lentes.core :as lentes]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [hiccup.core]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-time.format :as time.format]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.string :as str])
  (:import java.util.Locale)
  (:gen-class))

(declare with-redirect)


(def app-db (atom {:business-id ""
                   :privat nil
                   :manager nil}))

;; (def privat (lentes/derive (lentes/key :privat) app-db))
(def manager (lentes/derive (lentes/key :manager) app-db))




(defn template
  "шаблон этого HTML"
  [& args]
  (hiccup.core/html
   [:html
    [:head
     [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:href "/vendors/bootstrap/dist/css/bootstrap.min.css" :rel "stylesheet"}]
     [:link {:href "/vendors/bootstrap-daterangepicker/daterangepicker.css" :rel "stylesheet"}]
     [:link {:href "/vendors/font-awesome/css/font-awesome.min.css" :rel "stylesheet"}]
     [:link {:href "/build/css/custom.min.css" :rel "stylesheet"}]
     [:link {:href "/vendors/google-code-prettify/bin/prettify.min.css" :rel "stylesheet"}]
 
     [:title "Admin"]]
    [:body.nav-md
     [:div.container.body
      [:div.main_container
       [:div.col-md-3.left_col
        [:div.left_col.scroll-view
         [:div.navbar.nav_title
          [:a.site_title "Manager import"]]
         [:div.clearfix]
         [:div.profile.clearfix
          [:div.profile_pic [:img.img-circle.profile_img {:src "/production/images/img.jpg"}]]
          [:div.profile_info
           [:span "Предприятие"]
           [:h2 (:business-id @app-db)]]]
         [:br]
         (sidebar-menu (:manager @app-db))]]
       [:div.top_nav
        [:div.nav_menu
         [:nav
          [:div.nav.toggle
           [:a#menu_toggle [:i.fa.fa-bars]]]
          [:ul.nav.navbar-nav.navbar-right
           [:li
            [:a {:href "/login"} "Логин"]]]]]]
       [:div.right_col {:role "main" :style "min-height:914px;"}
        ;; [:div.page-title
        ;;  [:div.title_left
        ;;   [:h3 "Страница"]] 
        ;;  [:div.title_right]]
        ;; [:div.clearfix]
        [:div.row args]]]]
     [:script {:src "/vendors/jquery/dist/jquery.min.js"}]
     [:script {:src "/vendors/bootstrap/dist/js/bootstrap.js"}]
     [:script {:src "/vendors/moment/min/moment.min.js"}]
     [:script {:src "/vendors/bootstrap-daterangepicker/daterangepicker.js"}]
     [:script {:src "/build/js/custom.js"}]
     [:script {:src "/custom.js"}]]]))


(defn load-account! [account app-db]
  (with-redirect
   ;; (utils/map-to-html-list
    (do
      (config/load-settings! account app-db)
      (config/load-cached-db :customers app-db)
      (config/load-cached-db :suppliers app-db))
    "/accounts"))


(defn login-form [app-db]
  [:form {:action "/login" :method :POST}
   [:h3 (str "Доступ к " (get-in app-db [:privat :login]))]
   [:input.btn.btn-primary {:type :submit :value "Пройти авторизацию Privat24"}]])


(defn settings [app-db]
  (let [accounts #{"puzko" "itservice" "super-truper"}
        f (fn [e] [:option {:value e :selected (when (= e (:business-id @app-db)) true)} e])
        {roles :roles} (:privat @app-db)]
    (template
     [:div
        [:div.col-md-3
         (x-panel
          "Конфигурация предприятия"
          [:form {:method :post}
           [:select.form-control {:name "account"}
            (map f accounts)]
           [:br]
           [:input.btn.btn-primary {:type :submit :value "Загрузить"}]])]
        [:div.col-md-3
         (x-panel "Управление Manager"
          [:div
           [:h2 "Загрузить из кеша"]
           [:a.btn.btn-default {:href "/settings/load?data=customers"} "Покупателей"]
           [:a.btn.btn-default {:href "/settings/load?data=suppliers"} "Поставщиков"]
           [:div.divider]
           [:h2 "Сохранить кеш"]
           [:a.btn.btn-danger {:href "/settings/save?data=customers"} "Покупателей"]
           [:a.btn.btn-danger {:href "/settings/save?data=suppliers"} "Поставщиков"]])]
        [:div.col-md-3
         (x-panel "Приват24"
          (if-let [privat (:privat @app-db)]
            (if (privat.auth/authorized? privat) 
              (privat-session-info privat) 
              (login-form @app-db))
            "не загружены настройки"))]])))


(defn parse-edrpou-statements [app-db]
  (let [statements (get-in app-db [:manager :db :mstatements])]
    (if statements
     [:table.table
      [:thead
       [:tr
        [:th "Клиент"]
        [:th "ЕДРПОУ"]
        [:th "Поставщик"]
        [:th "ЕДРПОУ"]]]
      (for [statement statements] 
        [:tr
         [:td (get-in statement [:debit :name])]
         [:td (get-in statement [:debit :edrpou])]
         [:td (get-in statement [:credit :name])]
         [:td (get-in statement [:credit :edrpou])]])]
     [:p "Необходимо загрузить выписки"])))


(defn customers-index [app-db]
  (let [customers (get-in @app-db [:manager :db :customers])
        custom-edrpou (keyword (get-in @app-db [:manager :uuids :customer-edrpou]))]
        ;; custom-edrpou (keyword (:customer-edrpou @uuids))]
   (template
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
     (parse-edrpou-statements @app-db)])))


(defn suppliers-index [app-db]
  (let [suppliers (get-in @app-db [:manager :db :suppliers])
        edrpou-uuid (keyword (get-in @app-db [:manager :uuids :supplier-edrpou]))]
    (template
     [:form {:method :POST}
      [:input.btn.btn-primary {:type :submit :value "Загрузить из Manager"}]
      [:a.btn.btn-default {:href "/settings/load?data=suppliers"} "Загрузить из кеша"]]
     [:div.col-md-6
      (x-panel "Поставщики из Manager"
        [:table.table
         [:thead]
         [:tbody
          (for [supplier suppliers]
            (let [{n :Name :as m} (val supplier)
                  uuid (key supplier)]
              [:tr
               [:td [:a {:href (str "/suppliers/" (name uuid))} n]]
               [:td (get-in m [:CustomFields edrpou-uuid])]]))]])]
     [:div.col-md-6
      (parse-edrpou-statements @app-db)])))


 

(defn fetch-statements! [app-db stdate endate]
  (do
   (swap! app-db assoc-in [:manager :db :statements] (privat.api/get-statements (:privat @app-db) stdate endate))
   (swap! app-db assoc-in [:manager :db :mstatements] (->> (privat.util/make-statements-list (:manager @app-db))
                                                           (map privat.util/transform->manager2)
                                                           (map #(privat.util/make-manager-statement % manager))
                                                           (sort-by :date)))
   (ring.util.response/redirect "/statements")))


(defn statements-index [app-db]
  (let [statements (get-in @app-db [:manager :db :mstatements])]
    (template
       [:h1 "Выписки"]
       [:div.row
        (if (privat.auth/authorized? (:privat @app-db))
          (date-form)
          [:div.col-md-6.alert.alert-danger "необходима авторизация для загрузки выписок"])
        [:div.clearfix]
        (let [f (fn [statement i]
                  (let [{:keys[receipt amount refp payment purpose debit credit date payee payer recognized comment]} statement]
                    [:tr
                     [:td
                      [:article.media.event
                       (let [dt (time.format/unparse (time.format/formatter "dd-MMM") date)
                             [day month] (str/split dt #"\-")]
                        [:a.date.pull-left {:href (str "/statements/" i)}
                          [:p.month month]
                          [:p.day day]])]]
                     [:td amount]
                     [:td comment]
                     [:td (when recognized [:i.fa.fa-check-square]) " " payee payer]
                     [:td purpose]]))]
          (x-panel (str "Выписки " (count statements))
                   [:table.table
                    [:thead
                     [:tr
                      [:th.col-md-1 "Дата"]
                      [:th "Сумма"]
                      [:th.col-md-2 "Тип"]
                      [:th.col-md-3 "Контрагент"]
                      [:th "Назначение"]]]
                    [:tbody
                     (map f statements (iterate inc 0))]]))])))

(defn paging-statements [id]
   (let [statements (get-in @app-db [:manager :db :mstatements])
         has-prev? (> id 0) 
         has-next? (< (+ id 1) (count statements))]
    [:div
      (when has-prev?
        [:a.btn.btn-default {:href (str "/statements/" (dec id))} "предыдущий"])
      (when has-next?
        [:a.btn.btn-default {:href (str "/statements/" (inc id))} "следующий"])]))

(defn single-statement [index]
  (let [statement (-> (get-in @app-db [:manager :db :mstatements])
                      (nth index))
        {:keys [credit date debit amount comment recognized purpose payee payer]} statement
        custom-formatter (time.format/with-locale (time.format/formatter "dd.MM.yy в HH:mm") (Locale. "ru"))
        custom-formatter2 (time.format/formatter "dd.MM.yy")
        mdate2 (time.format/unparse custom-formatter2 date)
        mdate (time.format/unparse custom-formatter date)
        label (fn [statement] [:span.label.label-primary (-> (select-keys statement [:payment :receipt :transfer]) first key)])]
    (template
     [:div.col-md-10
      (x-panel [:div (when recognized [:i.fa.fa-check-square]) " банковская выписка от " mdate2] 
               [:div
                [:div.jumbotron
                 [:h1 payee payer " " comment " " amount]
                 [:h1 mdate] 
                 [:p (label statement) " " purpose]
                 [:div.controls
                   [:form {:method :POST}
                     [:input.btn.btn-danger {:type "submit" :value "Отправить в Manager"}]]]]

                [:div.col-md-6
                  [:h3 "Кредитор"]
                  (utils/map-to-html-list credit)]
                [:div.col-md-6
                  [:h3 "Дебитор"]
                  (utils/map-to-html-list debit)]
                [:div.clearfix
                 (utils/map-to-html-list (privat.util/render-manager-statement statement))]
                [:div.divider]
                [:div.controls.row
                  (paging-statements index)]])])))

;; (defn post-statement [id]
;;   (let [statement (nth (get-in @app-db [:manager :db :statements]) id)
;;         ;bid (get @db :business-id)
;;         {status :status headers :headers} (privat.util/post statement manager)]
;;     (template
;;      (if (= status 201)
;;        [:div
;;         [:h1 "Успешно создано!"]
;;         [:p (get headers "Location")]]
;;         ;[:a {:href "http://manager.it-premium.local:8080/payment-view?Key=4bf84e58-013d-413b-88c0-99454c23b119&FileID=937914e4-5686-4eec-ba21-474b1c0f982e"} "посмотреть"]]
;;       [:h1 "Произошла ошибка при создании!"])
;;      (paging-statements id))))

(defn post-statement! [id]
  (let [statement (nth (get-in @app-db [:manager :db :mstatements]) id)
        {status :status headers :headers} (privat.util/post2 statement manager)]
    (template
     (if (= status 201)
       [:div
        [:h1 "Успешно создано!"]
        [:p (get headers "Location")]]
                                        ;[:a {:href "http://manager.it-premium.local:8080/payment-view?Key=4bf84e58-013d-413b-88c0-99454c23b119&FileID=937914e4-5686-4eec-ba21-474b1c0f982e"} "посмотреть"]]
       [:h1 "Произошла ошибка при создании!"])
     (paging-statements id))))

(defn fetch-suppliers! [manager-db]
  (do
    (manager.api/get-index2! :suppliers manager-db)
    (manager.api/populate-db! :suppliers manager-db)
    (ring.util.response/redirect "/suppliers")))

(defn fetch-customers! [manager-db]
  (do
    (manager.api/get-index2! :customers manager-db)
    (manager.api/populate-db! :customers manager-db)
    (ring.util.response/redirect "/customers")))

(defn form-customer [uuid m]
  [:form.form-horizontal {:method :POST}
   [:div.form-group
    [:label.col-md-2.control-label {:for :edrpou} "ЕДРПОУ"]
    [:div.col-md-3
     [:input#edrpou {:type :text :name "edrpou" :value (:edrpou m)}]]]
   [:div.ln_solid]
   [:div.controls
    [:input.btn.btn-primary {:type :submit :value "Сохранить"}]]])
  
(defn form-supplier [uuid m]
  [:form {:method :POST}
   [:label.col-md-2.control-label {:for :edrpou} "ЕДРПОУ"]
   [:input#edrpou {:type :text :name "edrpou" :value (:edrpou m)}]
   [:div.ln_solid]
   [:div.controls
    [:input.btn.btn-primary {:type :submit :value "Сохранить"}]]])


(defn single-customer [uuid]
  (let [cust (get-in @manager [:db :customers (keyword uuid)])
        n (:Name cust)
        edrpou (get-in cust [:CustomFields (keyword (:customer-edrpou (get @manager :uuids)))])]
   (template
    [:div.col-md-4
     (x-panel (str "Покупатель " n)
      [:div
       (form-customer uuid {:edrpou edrpou})])])))


(defn single-supplier [uuid]
  (let [cust (get-in @manager [:db :suppliers (keyword uuid)])
        n (:Name cust)
        edrpou (get-in cust [:CustomFields (keyword (:supplier-edrpou (get @manager :uuids)))])]
    (template
     [:div.col-md-4
      (x-panel n (form-supplier uuid {:edrpou edrpou}))])))

(defn update-customer! [uuid m manager-db]
  (let [cust (keyword (get-in @manager-db [:uuids :customer-edrpou]))
        edrpou (:edrpou m)
        update-map (assoc-in (manager.api/fetch-uuid-item! uuid manager-db) [:CustomFields cust] edrpou)]
   (template
    [:p "Updated"
     (manager.api/api-update! uuid update-map manager-db)])))

(defn update-supplier! [uuid m]
  (let [cust (keyword (get-in @manager [:uuids :supplier-edrpou]))
        edrpou (:edrpou m)
        update-map (assoc-in (manager.api/fetch-uuid-item! uuid manager) [:CustomFields cust] edrpou)]
    (template
     [:p "Updated"
      (manager.api/api-update! uuid update-map manager)])))

(defn login! [app-db]
 (with-redirect
  (do
    (privat.auth/auth app-db)
    (-> (privat.auth/auth-p24 app-db)
        (get-in [:privat :session])
        (update :expires #(time.coerce/from-long (* % 1000)))
        utils/map-to-html-list))
  "/accounts"))

(defn rests-index [app-db]
  (let [rests (get-in @app-db [:manager :db :rests])]
    (template [:h1 "Остатки "]
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
                     [:td outrest]])]]))) 

(defn fetch-rests! [app-db stdate endate]
  (swap! app-db assoc-in [:manager :db :rests]
    (->>
     (privat.api/get-rests (:privat @app-db) stdate endate)
     (mapv privat.util/parse-rest)
     (sort-by :date)
     reverse)))

(defn with-redirect [f to]
  (do
    f
    (ring.util.response/redirect to))) 

(defroutes app 
  (GET "/" [] (settings app-db))
  (context "/customers" []
           (GET "/" [] (customers-index app-db))
           (POST "/" [] (fetch-customers! manager))
           (GET "/:uuid" [uuid] (single-customer uuid)) 
           (POST "/:uuid" [uuid edrpou] (update-customer! uuid {:edrpou edrpou} manager))) 
  (context "/suppliers" []
           (GET "/" [] (suppliers-index app-db))
           (GET "/:uuid" [uuid] (single-supplier uuid)) 
           (POST "/:uuid" [uuid edrpou] (update-supplier! uuid {:edrpou edrpou})) 
           (POST "/" [] (fetch-suppliers! manager)))
  (context "/rests" []
           (GET "/" [] (rests-index app-db))
           (POST "/" [stdate endate] (with-redirect (fetch-rests! app-db stdate endate) "/rests")))
  (context "/settings" []
   (GET "/load" [data] (with-redirect (config/load-cached-db (keyword data) app-db) "/accounts"))
   (GET "/save" [data] (with-redirect (config/save-cached-db (keyword data) app-db) "/accounts")))
  (GET "/accounts" [] (settings app-db))
  (POST "/accounts" [account] (load-account! account app-db))
  (POST "/login" [] (login! app-db))
  (GET "/auth/logout" [] (with-redirect (privat.auth/logout! app-db) "/accounts"))
  (context "/api" [])
  (context "/statements" []
           (GET "/" [] (statements-index app-db))
           (POST "/" [stdate endate] (fetch-statements! app-db stdate endate))
           (POST "/:id" [id :<< as-int] (post-statement! id))
           (GET "/:id" [id :<< as-int] (single-statement id))))


(def handler
  (-> app
      (wrap-defaults (update-in site-defaults [:security] dissoc :anti-forgery))
      (wrap-resource "public/vendor/gentelella")
      (wrap-resource "public")
      (wrap-params)
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server (atom nil))

 
(defn start-dev []
  (reset! server (run-jetty #'handler {:port 8080 :join? false})))


(defn stop-dev []
  (.stop @server))


(defn restart []
  (stop-dev)
  (start-dev))

(defn -main [& args]
  (when-let [conf (first args)]
     (do
      (config/load-settings! conf app-db)
      (config/load-cached-db :customers app-db)
      (config/load-cached-db :suppliers app-db)))
      
  (start-dev))
