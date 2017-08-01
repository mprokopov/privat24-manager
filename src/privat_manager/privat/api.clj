(ns privat-manager.privat.api
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]))

(def api "https://link.privatbank.ua/api/")

(defn post [{:keys [uri body headers]}]
  (client/post (str api uri)
               {:content-type :json
                :accept :json
                :debug true
                :headers (when headers headers)
                :body (when body (cheshire/generate-string body))}))

(defn get2 [{:keys [uri session query-params]}]
  (client/get (str api uri)
              {:content-type :json
               :accept :json
               ;; :debug true
               ;; :as :json
               :query-params query-params
               :headers {"Authorization" (str "Token " (get-in session [:session :id]))
                         "Content-type" "application/json"}}))

(defn get-body [m]
  (let [{status :status body :body} (get2 m)]
    (case status
      200 (cheshire/parse-string body true)
      {:error status :message body})))

(defn get-statements [session stdate endate]
  (get-body {:uri "p24b/statements"
             :session session
             :query-params {"acc" (:bank-account-number session)
                            "showInf" true
                            "stdate" stdate     ;"01.05.2017"
                            "endate" endate}})) ;"10.05.2017"}}))

(defn get-rests [session stdate endate]
  (get-body {:uri "p24b/rests"
             :session session
             :query-params {"acc" (:bank-account-number session)
                            "showInf" true
                            "stdate" stdate ;"01.05.2017"
                            "endate" endate}}))
