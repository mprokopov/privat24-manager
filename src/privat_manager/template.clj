(ns privat-manager.template)


(defn x-panel [title body]
  [:div.x_panel
   [:div.x_title
    [:h2 title]
    [:ul.nav.navbar-right.panel_toolbox
     [:li
      [:a.close-link [:i.fa.fa-close]]]]
    [:div.clearfix]]
   [:div.x_content body]])

(defn session-info [db]
  (let [{{roles :roles} :session bid :business-id} @db]
    [:div
     [:h2 "Бизнес: " bid]
     [:h4 "Роли: " (clojure.string/join "," roles)]
     [:a.btn.btn-default {:href "/auth/logout"} "Выйти"]]))


(defn sidebar-menu [db]
  [:div#sidebar-menu.main_menu_side.hidden-print.main_menu
   [:div.menu_section
    [:h3 "Menu"]
    (let [{:keys [customers suppliers statements]} (get @db :db)]
      [:ul.nav.side-menu
       [:li
        [:a [:i.fa.fa-home] "Privat24" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/statements"} (str "Выписки " (count statements))]]]]
       [:li
        [:a [:i.fa.fa-book] "Manager" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/customers"} (str "Клиенты " (count customers))]]
         [:li [:a {:href "/suppliers"} (str "Поставщики " (count suppliers))]]]]
       [:li
        [:a [:i.fa.fa-sliders] "Управление" [:span.fa.fa-chevron-down]]
        [:ul.nav.child_menu {:style "display: block;"}
         [:li [:a {:href "/accounts"} "Настройки"]]]]])]])
