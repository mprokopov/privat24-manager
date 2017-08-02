(ns privat-manager.statement
  (:require
   [privat-manager.utils :as utils]
   [privat-manager.template :refer [x-panel date-form]]
   [privat-manager.privat.util :as privat.util]
   [privat-manager.privat.api :as privat.api]
   [privat-manager.privat.auth :as privat.auth]
   [clojure.string :as str]
   [clj-time.format :as time.format])
  (:import java.util.Locale))


(defn paging [id statements]
  (let [has-prev? (> id 0)
        has-next? (< (+ id 1) (count statements))]
    [:div
     (when has-prev?
       [:a.btn.btn-default {:href (str "/statements/" (dec id))} "предыдущий"])
     (when has-next?
       [:a.btn.btn-default {:href (str "/statements/" (inc id))} "следующий"])]))

(defn single [index {{{statements :mstatements} :db} :manager}]
  (let [statement (-> statements
                      (nth index))
        {:keys [credit date debit amount comment recognized purpose payee payer]} statement
        custom-formatter (time.format/with-locale (time.format/formatter "dd.MM.yy в HH:mm") (Locale. "ru"))
        custom-formatter2 (time.format/formatter "dd.MM.yy")
        mdate2 (time.format/unparse custom-formatter2 date)
        mdate (time.format/unparse custom-formatter date)
        label (fn [statement] [:span.label.label-primary (-> (select-keys statement [:payment :receipt :transfer]) first key)])]
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
                  (paging index statements)]])]))


(defn index [{{{statements :mstatements} :db} :manager :as app-db}]
       [:div
        [:h1 "Выписки"]
        (if (privat.auth/authorized? app-db)
          (date-form)
          [:div.alert.alert-danger "необходима авторизация для загрузки выписок"])
        ;; [:div.clearfix]
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
          (when statements
            [:table.table
              [:thead
                [:tr]
                [:th.col-md-1 "Дата"]
                [:th "Сумма"]
                [:th.col-md-2 "Тип"]
                [:th.col-md-3 "Контрагент"]
                [:th "Назначение"]]
              [:tbody
                (map f statements (iterate inc 0))]]))])

(defn post! [index {{{statements :mstatements} :db :as manager} :manager}]
  (let [statement (nth statements index)
        {status :status headers :headers} (privat.util/post2 statement manager)]
    [:div
     (if (= status 201)
       [:div
        [:h1 "Успешно создано!"]
        [:p (get headers "Location")]] ; /api/937914e4-5686-4eec-ba21-474b1c0f982e/5890c101-ade6-4d67-8793-f484dcf30308.json
                                        ;[:a {:href "http://manager.it-premium.local:8080/payment-view?Key=4bf84e58-013d-413b-88c0-99454c23b119&FileID=937914e4-5686-4eec-ba21-474b1c0f982e"} "посмотреть"]]
       [:h1 "Произошла ошибка при создании!"])
     (paging index statements)])) 

(defn fetch! [app-db stdate endate]
  (let [statements (privat.api/get-statements (:privat @app-db) stdate endate)]
    (if (:error statements)
      (assoc statements :flash (:message statements))
      (swap! app-db assoc-in [:manager :db :mstatements] (->> statements
                                                          (transduce (privat.util/privat->manager @app-db) conj)
                                                          (sort-by :date))))))
;; TODO
;; (defn fetch2! [app-db stdate endate]
;;   (do
;;     (swap! app-db assoc-in [:manager :db :statements] (privat.api/get-statements (:privat @app-db) stdate endate))
;;     (swap! app-db assoc-in [:manager :db :mstatements] (->> (privat.util/make-statements-list @app-db)
;;                                                             (map privat.util/transform->manager2)
;;                                                             (map #(privat.util/make-manager-statement % @app-db))
;;                                                             (sort-by :date)))))
