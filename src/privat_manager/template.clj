(ns privat-manager.template
  (:require [privat-manager.utils :as utils]
            [clj-time.format :as time.format]
            [clj-time.coerce :as time.coerce]
            [hiccup.core])
  (:import java.util.Locale))

(declare date-form sidebar-menu)

(defn template
  "шаблон этого HTML"
  [app-db flash body]
  (hiccup.core/html
   [:html
    [:head
     [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:href "/vendors/bootstrap/dist/css/bootstrap.min.css" :rel "stylesheet"}]
     [:link {:href "/vendors/bootstrap-daterangepicker/daterangepicker.css" :rel "stylesheet"}]
     [:link {:href "/vendors/pnotify/dist/pnotify.css" :rel "stylesheet"}]
     [:link {:href "/vendors/font-awesome/css/font-awesome.min.css" :rel "stylesheet"}]
     [:link {:href "/build/css/custom.min.css" :rel "stylesheet"}]
     [:link {:href "/styles.css" :rel "stylesheet"}]
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
        ;; (when flash
        ;;   [:div.row flash])
        ;; [:div.page-title
        ;;  [:div.title_left
        ;;   [:h3 "Страница"]] 
        ;;  [:div.title_right]]
        ;; [:div.clearfix]
        [:div.row body]]]]
     (when flash
       (let [type (-> flash keys first)
             message (get flash type)
             title (case type :success "Успех" :error "Ошибка" :else "Что-то случилось")]
         [:script "window.onload = function(){new PNotify({title: '" title "', text: '" message "', type: '" type "', styling: 'bootstrap3'});}"]))
     [:script {:src "/vendors/jquery/dist/jquery.min.js"}]
     [:script {:src "/vendors/pnotify/dist/pnotify.js"}]
     [:script {:src "/vendors/bootstrap/dist/js/bootstrap.js"}]
     [:script {:src "/vendors/moment/min/moment.min.js"}]
     [:script {:src "/vendors/bootstrap-daterangepicker/daterangepicker.js"}]
     [:script {:src "/build/js/custom.js"}]
     [:script {:src "/custom.js"}]]]))

(defn x-panel [title body]
  [:div.x_panel
   [:div.x_title
    [:h2 title]
    [:ul.nav.navbar-right.panel_toolbox
     [:li
      [:a.close-link [:i.fa.fa-close]]]]
    [:div.clearfix]]
   [:div.x_content body]])


(defn sidebar-menu [db]
  [:div#sidebar-menu.main_menu_side.hidden-print.main_menu
   [:div.menu_section
    [:h3 "Menu"]
    (let [{:keys [customers suppliers mstatements rests]} (get db :db)]
      [:ul.nav.side-menu
       [:li
        [:a [:i.fa.fa-home] "Privat24" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/statements"} (str "Выписки " (count mstatements))]]
         [:li [:a {:href "/rests"} (str "Остатки " (count rests))]]]]
       [:li
        [:a [:i.fa.fa-book] "Manager" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/customers"} (str "Клиенты " (count customers))]]
         [:li [:a {:href "/suppliers"} (str "Поставщики " (count suppliers))]]]]
       [:li
        [:a [:i.fa.fa-sliders] "Управление" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/settings"} "Настройки"]]]]])]])

(def custom-formatter (time.format/formatter "dd-MM-yyyy"))

(defn date-form []
  [:div.col-md-3
   [:form#date-select {:method :post}
    [:input {:name :stdate :type :hidden :value (time.format/unparse custom-formatter (clj-time.core/now))}]
    [:input {:name :endate :type :hidden :value (time.format/unparse custom-formatter (clj-time.core/now))}]
    [:div.controls
     [:div.input-prepend.input-group
      [:span.add-in.input-group-addon
       [:i.glyphicon.glyphicon-calendar.fa.fa-calendar]]
      [:input#testDate.form-control {:type :text :name :testDate :style "width: 180px;"}]
      [:span.input-group-btn
       [:button.btn.btn-primary {:type :button :onClick "document.getElementById('date-select').submit();"} "Загрузить из Privat24"]]]]]])


(defn paging-statements [id app-db]
  (let [statements (get-in app-db [:manager :db :mstatements])
        has-prev? (> id 0) 
        has-next? (< (+ id 1) (count statements))]
    [:div
     (when has-prev?
       [:a.btn.btn-default {:href (str "/statements/" (dec id))} "предыдущий"])
     (when has-next?
       [:a.btn.btn-default {:href (str "/statements/" (inc id))} "следующий"])]))
