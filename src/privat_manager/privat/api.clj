(ns privat-manager.privat.api
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]))

(def api "https://acp.privatbank.ua/api/proxy/")

(defn post [{:keys [uri body headers]}]
  (client/post (str api uri)
               {:content-type :json
                :accept :json
                :throw-exceptions false
                ;; :debug true
                :headers (when headers headers)
                :body (when body (cheshire/generate-string body))}))

(defn get [{:keys [uri session query-params]}]
  (client/get (str api uri)
              {;;:content-type :json
               :accept :json
               :throw-exceptions false
               ;; :debug true
               ;; :as :json
               :query-params query-params
               :headers {"Id" (:privat-id session)
                         "Token" (:privat-token session)
                         "Content-type" "application/json;charset=utf8"
                         }}))

(defn get-body [m]
  (let [{status :status body :body} (get m)]
    (case status
      200 (cheshire/parse-string body true)
      {:error status :message body})))

(defn unpack-statements [json]
  (get-in json [:StatementsResponse :statements]))

(defn get-statements [session stdate endate]
  (get-body {:uri "transactions"
             :session session
             :query-params {"acc" (:bank-account-number session)
                            ;; "showInf" true
                            "startDate" stdate     ;"01-05-2017"
                            "endDate" endate}})) ;"10-05-2017"

(defn get-rests [session stdate endate]
  (get-body {:uri "rest"
             :session session
             :query-params {"acc" (:bank-account-number session)
                            ;; "showInf" true
                            "startDate" stdate ;"01-05-2017"
                            "endDate" endate}}))

;; (->
;;  (get-statements (:privat @dev/app-db ) "01-08-2019" "05-08-2019")
;;  :StatementsResponse
;;  :statements
;;  first
;;  privat-manager.privat.parser/parse-statement2
;;  )
