(ns privat-manager.auth
  (:require [privat-manager.privat.auth :as privat.auth]
            [privat-manager.utils :as utils]
            [clj-time.coerce :as time.coerce]))

;; TODO: отрабатывать ошибки 500 от сервера
(defn login! [app-db]
  (do
    (privat.auth/authenticate app-db)
    (privat.auth/authenticate-business-privat24 app-db)
    {:flash "Авторизован успешно"}))
        ;; (get-in [:privat :session])
        ;; (update :expires #(time.coerce/from-long (* % 1000)))
        ;;utils/map-to-html-list)))
