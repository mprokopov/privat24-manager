(ns privat-manager.template
  (:require [privat-manager.utils :as utils]
            [clj-time.format :as time.format]
            [clj-time.coerce :as time.coerce])
  (:import java.util.Locale))


(defn x-panel [title body]
  [:div.x_panel
   [:div.x_title
    [:h2 title]
    [:ul.nav.navbar-right.panel_toolbox
     [:li
      [:a.close-link [:i.fa.fa-close]]]]
    [:div.clearfix]]
   [:div.x_content body]])

(defn privat-session-info [privat-session]
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
         [:li [:a {:href "/accounts"} "Настройки"]]]]])]])

(defn date-form []
  [:div.col-md-3
   [:form#date-select {:method :post}
    [:input {:name :stdate :type :hidden}]
    [:input {:name :endate :type :hidden}]
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
