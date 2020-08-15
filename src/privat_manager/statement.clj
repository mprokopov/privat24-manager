(ns privat-manager.statement
  (:require
   [privat-manager.utils :as utils]
   [privat-manager.template :refer [x-panel date-form]]
   [privat-manager.utils :as utils]
   [privat-manager.privat.util :as privat.util]
   [privat-manager.privat.parser :as privat.parser]
   [privat-manager.privat.api :as privat.api]
   [privat-manager.privat.auth :as privat.auth]
   [privat-manager.manager.api :as manager.api]
   [clojure.string :as str]
   [clj-time.format :as time.format]
   [privat-manager.utils :as util])
  (:import java.util.Locale))

;; TODO: давать возможность выбрать тип платежа прямо из интерфейса

(defn create-manager-statement
  "parses initial bank statement and makes POST to Manager API
  db should contain {:login '' :password ''}"
  [statement manager-db]
  (let [category-key (manager.api/category-key statement)]
    (manager.api/create-item category-key (privat.util/render-manager-statement statement) manager-db))) 


(defn privat->manager-transducer [app-db] (comp (map privat.parser/parse-statement)
                                             (map privat.parser/assoc-transaction-type)
                                             (map #(manager.api/statement % app-db))))

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
        custom-formatter (time.format/with-locale utils/custom-ru-formatter (Locale. "ru"))
        mdate2 (time.format/unparse utils/custom-date-formatter date)
        mdate (time.format/unparse custom-formatter date)
        label (fn [statement] [:span.label.label-primary (-> (select-keys statement [:payment :receipt :transfer]) first key)])]
     [:div.col-md-10
      (x-panel [:div {:class (if recognized "statement_recognized" "statement_nonrecognized")} (when recognized [:i.fa.fa-check-square]) " банковская выписка от " mdate2] 
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
          [:div.alert.alert-danger "Необходим токен для загрузки выписок. Создайте автоклиент."])
        ;; [:div.clearfix]
        (let [f (fn [statement i]
                  (let [{:keys[receipt amount refp payment purpose debit credit date payee payer recognized comment]} statement]
                    [:tr
                     [:td
                      [:article.media.event
                       (let [dt (time.format/unparse (time.format/with-locale (time.format/formatter "dd-MMM") (Locale. "ru")) date)
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
        {status :status headers :headers} (create-manager-statement statement manager)]
    [:div
     (if (= status 201)
       [:div
        [:h1 "Успешно создано!"]
        (let [location (get headers "Location")
              uid (manager.api/parse-header-location location)
              link (manager.api/payment-view-link uid (manager :business-id))]
          [:div
           [:p
            "Создана запись " uid]
           [:a.btn.btn-success {:href link} "Открыть в Manager"]])] ; /api/937914e4-5686-4eec-ba21-474b1c0f982e/5890c101-ade6-4d67-8793-f484dcf30308.json
       [:h1 "Произошла ошибка при создании!"])
     (paging index statements)]))

(defn fetch! [app-db stdate endate]
  (let [statements (privat.api/get-statements (:privat @app-db) stdate endate)]
    (if (:error statements)
      (assoc statements :flash (:error statements))
      (try
        (swap! app-db assoc-in [:manager :db :mstatements] (->> (privat.api/unpack-statements statements)
                                                                (transduce (privat->manager-transducer @app-db) conj)
                                                                (sort-by :date)))
        { :flash { :success "Выписки успешно загружены"}}
        (catch AssertionError e { :flash { :error (.getMessage e)}})
        ))))
;; (fetch! dev/app-db "15-06-2020" "15-06-2020")
;; TODO
;; (defn fetch2! [app-db stdate endate]
;;   (do
;;     (swap! app-db assoc-in [:manager :db :statements] (privat.api/get-statements (:privat @app-db) stdate endate))
;;     (swap! app-db assoc-in [:manager :db :mstatements] (->> (privat.util/make-statements-list @app-db)
;;                                                             (map privat.util/transform->manager2)
;;                                                             (map #(privat.util/make-manager-statement % @app-db))
;;                                                             (sort-by :date)))))

(comment
  (fetch! dev/app-db "01.06.2020" "06.06.2020")
  )
