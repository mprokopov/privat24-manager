(ns privat-manager.server
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as http.body]
            [io.pedestal.http.ring-middlewares :as ring-mid]
            [privat-manager.settings :as settings]
            [privat-manager.rests :as rests]
            [privat-manager.suppliers :as suppliers]
            [privat-manager.customers :as customers]
            [privat-manager.template :as template]
            [privat-manager.auth :as auth]
            [privat-manager.statement :as statements]
            [privat-manager.privat.auth :as privat.auth]
            [ring.util.response :as response]
            [io.pedestal.http.route :as route]))

;; (defonce app-db (atom {:business-id ""
;;                        :privat nil
;;                        :manager nil}))

(def render-result
  {:name :render-result
   :leave (fn [context]
            (let [result (get context :result)
                  redirect (get context :redirect)]
              (if redirect
                (assoc context :response
                       (cond-> (response/redirect redirect)
                         (:flash result) (assoc :flash (:flash result))))
                (assoc context :response (response/content-type (response/response result) "text/html")))))})

(def wrap-template
  {:name :template
   :leave (fn [context]
            (let [db (get context :db)
                  flash (get-in context [:request :flash])] 
               (update context :result #(template/template db flash %))))})

(defn db [app-db]
  {:name :db
   :enter (fn [context]
            (assoc context :db app-db))})

(def settings
  {:name :settings-index
   :leave (fn [context]
            (let [db (get context :db)]
              (assoc context :result (settings/index db))))})


(def set-account
  {:name :load-settings
   :enter (fn [context]
            (let [account (get-in context [:request :form-params :account] "puzko")] 
              (assoc context :account account))) 
   :leave (fn [context]
            (let [account (get context :account)
                  db (get context :db)]
              (assoc context
                      :result (settings/load-account! account db)
                      :redirect (route/url-for :settings-index))))})

(def privat-auth-login
  {:name :login
   :enter (fn [context]
            (let [db (get context :db)]
              (assoc context
                     :result (auth/login! db)
                     :redirect (route/url-for :settings-index))))})

(def debug-request
  {:name :debug-request
   :enter (fn [context]
            (do
              (println (:request context))
              context))})
(def debug
  {:name :debugger
   :enter (fn [context]
            (do
              (println context)
              context))
   :leave (fn [context]
            (do
              (println context)
              context))})

(def statements
  {:name :statements-index
   :enter (fn [context]
            (let [db (:db context)]
              (assoc context :result (statements/index @db))))})

(def suppliers
  {:name :suppliers-index
   :enter (fn [context]
            (let [db (:db context)]
              (assoc context :result (suppliers/index @db))))})

(def customers
  {:name :customers-index
   :enter (fn [context]
            (let [db (:db context)]
              (assoc context :result (customers/index @db))))})

(def rests
  {:name :rests-index
   :enter (fn [context]
            (let [db (:db context)]
              (assoc context :result (rests/index @db))))})

(def get-dates
  {:name :get-dates
   :enter (fn [context]
            (let [{:keys [stdate endate]} (get-in context [:request :form-params])] ; {:stdate "1.08.2017" :endate "15.08.2017"})]
              (assoc context
                     :start-date stdate
                     :end-date endate)))})
(def rests-load
  {:name :rests-load
   :leave (fn [context]
            (let [{:keys [start-date end-date db]} context]
              (assoc context
                     :result (rests/fetch! db start-date end-date)
                     :redirect (route/url-for :rests-index))))})

(def statements-load
  {:name :statements-load
   :leave (fn [context]
            (let [{:keys [start-date end-date db]} context]
              (assoc context
                     :result (statements/fetch! db start-date end-date)
                     :redirect (route/url-for :statements-index))))})

(def load-cached
  {:name :load-cached-data
   :enter (fn [context]
            (let [data (get-in context [:request :query-params :data])
                  db (get context :db)
                  redirect-url (keyword (str data "-index"))]
              (assoc context
                     :result (privat-manager.config/load-cached-db data db)
                     :redirect (route/url-for redirect-url))))})

(def save-cached
  {:name :save-cached-data
   :enter (fn [context]
            (let [data (get-in context [:request :query-params :data])
                  db (get context :db)
                  redirect-url (keyword (str data "-index"))]
              (assoc context
                     :result (privat-manager.config/save-cached-db data db)
                     :redirect (route/url-for redirect-url))))})

(def load-customers
  {:name :load-customers
   :enter (fn [context]
            (let [db (get context :db)]
              (assoc context
                     :result (customers/fetch! db)
                     :redirect (route/url-for :customers-index))))})

(def load-suppliers
  {:name :load-suppliers
   :enter (fn [context]
            (let [db (get context :db)]
              (assoc context
                     :result (suppliers/fetch! db)
                     :redirect (route/url-for :suppliers-index))))})

(def statement
  {:name :statement
   :leave (fn [context]
            (let [statement-id (get-in context [:request :path-params :id])
                  id (Integer/parseInt statement-id)
                  db (get context :db)]
              (assoc context :result (statements/single id @db))))})

(def privat-send-otp
  {:name :privat-send-otp
   :enter (fn [context]
            (let [otp-person-id (get-in context [:request :query-params :id])
                  id (Integer/parseInt otp-person-id)
                  db (get context :db)]
              (assoc context
                     :result (privat.auth/send-otp2! id db))))})
                     ;:redirect (route/url-for :settings-index))))})
(def debug-result
  {:name :debug-result
   :enter (fn [context]
            (do
              (println "result is " (:result context) "\n")
              (println "query params" (:query-params context) "\n")
              context))})

(def privat-check-otp
  {:name :privat-check-otp
   :enter (fn [context]
            (let [otp (get-in context [:request :form-params :otp])
                  db (get context :db)]
              (assoc context
                     :result (privat.auth/check-otp2! otp db)
                     :redirect (route/url-for :settings-index))))})

(def statement->manager
  {:name :statement->manager
   :leave (fn [context]
            (let [db (get context :db)
                  statement-index (get-in context [:request :path-params :id])
                  id (Integer/parseInt statement-index)]
              (assoc context :result (statements/post! id @db))))})

(def privat-logout
  {:name :privat-logout
   :leave (fn [context]
            (let [db (get context :db)]
              (assoc context
                     :result (privat.auth/logout! db)
                     :redirect (route/url-for :settings-index))))})

(def get-uuid
  {:name :get-uuid
   :enter (fn [context]
            (let [uuid (get-in context [:request :path-params :uuid])]
              (assoc context :uuid uuid)))})

(def single-supplier
  {:name :single-supplier
   :leave (fn [context]
            (let [{:keys [db uuid]} context]
              (assoc context :result (suppliers/single uuid db))))})

(def single-customer
  {:name :single-customer
   :leave (fn [context]
            (let [{:keys [db uuid]} context]
              (assoc context :result (customers/single uuid db))))})

(def update-customer
  {:name :update-customer
   :leave (fn [context]
            (let [{:keys [db uuid]} context
                  edrpou (get-in context [:request :form-params :edrpou])]
              (assoc context
                     :result (customers/update! uuid {:edrpou edrpou} db)
                     :redirect (route/url-for :single-customer :params {:uuid uuid}))))})

(def update-supplier
  {:name :update-supplier
   :leave (fn [context]
            (let [{:keys [db uuid]} context
                  edrpou (get-in context [:request :form-params :edrpou])]
              (assoc context
                     :result (suppliers/update! uuid {:edrpou edrpou} db)
                     :redirect (route/url-for :single-supplier :params {:uuid uuid}))))})

;; (def common-routes [(http.body/body-params) render-result db])
;; (def template-routes (conj common-routes wrap-template)) 

(defn routes [db-atom]
  (let [common-routes [(http.body/body-params) ring-mid/cookies (ring-mid/session) (ring-mid/flash) render-result (db db-atom)]
        template-routes (conj common-routes wrap-template)]
   (route/expand-routes
    #{["/" :get {:enter #(assoc % :response (response/redirect (route/url-for :settings-index)))} :route-name :index]
      ["/settings" :get (conj template-routes settings)]
      ["/settings" :post (conj common-routes set-account)]
      ["/settings/load" :get (conj common-routes load-cached)]
      ["/settings/save" :get (conj common-routes save-cached)]
      ["/statements" :get (conj template-routes statements)]
      ["/statements/:id" :get (conj template-routes statement)]
      ["/statements/:id" :post (conj template-routes statement->manager)]
      ["/statements" :post (conj template-routes get-dates statements-load)]
      ["/rests" :get (conj template-routes rests)]
      ["/rests" :post (conj common-routes get-dates rests-load)]
      ["/suppliers" :get (conj template-routes suppliers)]
      ["/suppliers/:uuid" :get (conj template-routes get-uuid single-supplier)]
      ["/suppliers/:uuid" :post (conj common-routes get-uuid update-supplier)]
      ["/suppliers" :post (conj template-routes load-suppliers)]
      ["/customers" :get (conj template-routes customers)]
      ["/customers" :post (conj template-routes load-customers)]
      ["/customers/:uuid" :get (conj template-routes get-uuid single-customer)]
      ["/customers/:uuid" :post (conj common-routes get-uuid update-customer)]
      ["/auth/login" :post (conj template-routes privat-auth-login)] 
      ["/auth/logout" :get (conj common-routes privat-logout)]
      ["/auth/login/otp" :get (conj template-routes privat-send-otp)]
      ["/auth/login/otp" :post (conj common-routes privat-check-otp)]})))


;; (defn test?
;;   [service-map]
;;   (= :test (:env service-map)))

;; (defrecord Pedestal [service-map service]
;;   component/Lifecycle
;;   (start [this]
;;     (if service
;;       this
;;       (cond-> service-map
;;         true http/create-server
;;         (not (test? service-map)) http/start
;;         true ((partial assoc this :service)))))
;;  (stop [this]
;;     (when (and service (not (test? service-map)))
;;       (http/stop service))
;;     (assoc this :service nil)))

(defrecord Webserver [app-atom handler host port server env]
  component/Lifecycle
  (start [component]
    (println "Start web server")
    (assoc component :server
           (http/start
            (http/create-server {::http/routes (routes app-atom)
                                 ::http/type :jetty
                                 ::http/resource-path "/public/vendor/gentelella"
                                 ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}
                                 ::http/join? (not (= env :dev)) ;; false if development
                                 ::http/port port}))))
  (stop [component]
    (println "Stop web server")
    (http/stop server)
    (assoc component :server nil)))

(defn system [config-options]
  (let [{:keys [host port app-atom routes env]} config-options]
    (component/system-map
     :app (map->Webserver {:host host
                           :port port
                           :env env
                           :app-atom app-atom})))) 
