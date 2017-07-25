(ns privat-manager.privat.auth
  (:require [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [privat-manager.privat.api :as api]))

(defn logout! [app-db]
  (let [{status :status} (api/post {:uri "auth/removeSession"
                                    :body {"sessionId" (get-in @app-db [:privat :session :id])}})]
    (when (= 200 status)
      (swap! app-db assoc-in [:privat :session] nil))))

(defn auth [app-db]
  (let [{app-id :app-id app-secret :app-secret} (get @app-db :privat)
        {body :body status :status} (api/post {:uri "auth/createSession"
                                               :body {"clientId" app-id
                                                      "clientSecret" app-secret}})] 
    (when (= status 200)
      (let [{id :id roles :roles expires :expiresIn} (cheshire/parse-string body true)]
        (swap! app-db assoc-in [:privat :session] {:id id
                                                   :roles roles
                                                   :expires expires})))))


(defn auth-p24
  "creds atom with :privat :session structure"
  [app-db]
  (if-let [id (get-in @app-db [:privat :session :id])]
    (let [{login :login password :password} (:privat @app-db)
          {body :body status :status} (api/post {:uri "p24BusinessAuth/createSession"
                                                 :body {"sessionId" id "login" login "password" password}})]
      (when (= status 200)
        (let [{:keys [clientId message roles expiresIn id]} (cheshire/parse-string body true)]
          (swap! app-db assoc-in [:privat :session] {:client-id clientId
                                                     :id id
                                                     :message message
                                                     :expires expiresIn
                                                     :roles roles}))))
    (print "auth required")))

(defn send-otp [creds]
  (let [id (get-in @creds [:session :id])
        otp-dev (get-in @creds [:otp-id])]
    (api/post {:uri "p24BusinessAuth/sendOtp"
               :body {"sessionId" id "otpDev" otp-dev}})))

(defn check-otp [creds otp]
  (let [id (get-in @creds [:session :id])
        {body :body status :status} (api/post {:uri "p24BusinessAuth/checkOtp" :body {"sessionId" id "otp" otp}})]
      (when (= status 200)
        (let [{:keys [clientId message roles expiresIn id]} (cheshire/parse-string body true)]
          (swap! creds assoc :session {:client-id clientId
                                       :id id
                                       :message message
                                       :expires expiresIn
                                       :roles roles})))))

(defn validate-session [session]
  (api/post {:uri "auth/validateSession"
             :body {"sessionId" (get-in @session [:session :id])}}))

(defn logged? [p-creds]
  (get-in @p-creds [:session :id]))

(defn authorized?
  "ROLE_P24_BUSINESS should be under [:session :roles] atom"
  [{{{roles :roles} :session} :privat}]
  (some #(= % "ROLE_P24_BUSINESS") roles)) 
