(ns privat-manager.core
  (:require [privat-manager.privat.auth :as privat.auth]
            [privat-manager.utils :as utils]
            [privat-manager.rests :as rests]
            [privat-manager.statement :as mstatement]
            [privat-manager.customers :as customers]
            [privat-manager.suppliers :as suppliers]
            [privat-manager.settings :as settings]
            [privat-manager.config :as config]
            [privat-manager.template :refer [x-panel sidebar-menu]]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.coercions :refer [as-int as-uuid]]
            [clj-time.coerce :as time.coerce]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [yada.yada :refer [listener resource as-resource] :as yada]
            [yada.bidi :as bidi]
            [hiccup.core]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.string :as str])
  (:gen-class))

(declare with-redirect)

(def app-db (atom {:business-id ""
                   :privat nil
                   :manager nil}))

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
            [:a {:href "/auth/login"} "Логин"]]]]]]
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
    (settings/load-account! account app-db)
    "/settings"))

(defn settings-index [app-db]
  (template (settings/index app-db)))

(defn fetch-statements! [app-db stdate endate]
  (with-redirect
    (mstatement/fetch! app-db stdate endate)
    "/statements"))


(defn statements-index [app-db]
  (template [:h1 "Выписки"] (mstatement/index @app-db)))

(defn single-statement [index]
  (template (mstatement/single index @app-db)))

(defn post-statement! [index]
  (template (mstatement/post! index @app-db)))

(defn single-customer [uuid app-db]
   (template (customers/single uuid @app-db)))

(defn single-supplier [uuid app-db]
  (template (suppliers/single uuid @app-db)))

(defn update-customer! [uuid m app-db]
  (template (customers/update! uuid m app-db)))

(defn update-supplier! [uuid m app-db]
  (template (suppliers/update! uuid m app-db)))

(defn login! [app-db]
 (with-redirect
  (do
    (privat.auth/auth app-db)
    (-> (privat.auth/auth-p24 app-db)
        (get-in [:privat :session])
        (update :expires #(time.coerce/from-long (* % 1000)))
        utils/map-to-html-list))
  "/settings"))

(defn with-redirect [f to]
  (do
    f
    (ring.util.response/redirect to))) 

(defroutes app 
  (GET "/" [] (with-redirect identity "/settings")) 
  (context "/customers" []
           (GET "/" [] (template (customers/index @app-db)))
           (POST "/" [] (with-redirect (customers/fetch! app-db) "/customers"))
           (GET "/:uuid" [uuid] (single-customer uuid app-db)) 
           (POST "/:uuid" [uuid edrpou] (update-customer! uuid {:edrpou edrpou} app-db))) 
  (context "/suppliers" []
           (GET "/" [] (template (suppliers/index @app-db)))
           (GET "/:uuid" [uuid] (single-supplier uuid app-db)) 
           (POST "/:uuid" [uuid edrpou] (update-supplier! uuid {:edrpou edrpou} app-db)) 
           (POST "/" [] (with-redirect (suppliers/fetch! app-db) "/suppliers")))
  (context "/rests" []
           (GET "/" [] (template [:h1 "Остатки"] (rests/index @app-db)))
           (POST "/" [stdate endate] (with-redirect (rests/fetch! app-db stdate endate) "/rests")))
  (context "/settings" []
           (GET "/" [] (settings-index app-db))
           (POST "/" [account] (load-account! account app-db))
           (GET "/load" [data] (with-redirect (config/load-cached-db (keyword data) app-db) "/settings"))
           (GET "/save" [data] (with-redirect (config/save-cached-db (keyword data) app-db) "/settings")))
  (context "/auth" []
           (POST "/login" [] (login! app-db))
           (GET "/logout" [] (with-redirect (privat.auth/logout! app-db) "/settings")))
  ;; (context "/api" [])
  (context "/statements" []
           (GET "/" [] (statements-index app-db))
           (POST "/" [stdate endate] (fetch-statements! app-db stdate endate))
           (POST "/:id" [id :<< as-int] (post-statement! id))
           (GET "/:id" [id :<< as-int] (single-statement id))))

(yada/handler
 {:methods {:get
            {:produces "text/html"
             :response "<h1>Hi</h1>"}}})

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

(def rests-index-resource
  (yada/resource
   {
    :id :rest/index
    :produces "text/html"
     :methods { :get { :response (template (rests/index @app-db))}}}))

(def statements-index-resource
  (yada/resource
   {
    :id :statements/index
    :produces "text/html"
    :methods { :get { :response (template (mstatement/index @app-db))}}}))

(defn load-s [ctx]
  (let [account (get-in ctx [:parameters :form :account])]
    (template
      (settings/load-account! account app-db))))

(def settings-resource
  (yada/resource
   {
    :id :settings/index
    :produces "text/html"
    :methods {:get {:response (template (settings/index app-db))}
              :post {
                     :consumes "application/x-www-form-urlencoded"
                     :parameters {:form {:account String}}
                     :response load-s}}}))

(def routes
  ["/"
   [
    ["db"
     [
      ["/privat" (yada/yada (get @app-db :privat))]
      ["/manager" (yada/yada (get-in @app-db [:manager]))]]]
    ["auth/login" (yada/resource {
                                  :produces "text/hml"
                                  :methods { :post { :response (login! app-db)}}})]
    ["rests" rests-index-resource]
    ["statements" statements-index-resource]
    ["settings" settings-resource]
    ["" (yada.resources.classpath-resource/new-classpath-resource "public/vendor/gentelella")]]])

((:close svr))

(def svr
  (yada/listener
   routes
   {:port 3000}))

(defn stop! []
  ((:close svr)))
;; (def svr
;;   (yada/listener
;;    ["/"
;;     [
;;      ["rest" rests-index-resource]
;;      ["hello" (as-resource "Hello World!")]
;;      ["test" (resource {:produces "text/plain"
;;                         :response "This is a test!"})]
;;      [true (as-resource nil)]]]
;;    {:port 3000}))


(defn -main [& args]
  (settings/load-account! (first args) app-db)
  (start-dev))
