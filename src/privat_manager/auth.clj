(ns privat-manager.auth
  (:require [privat-manager.privat.auth :as privat.auth]
            [privat-manager.utils :as utils]
            [clj-time.coerce :as time.coerce]))


(defn login! [app-db]
  (do
    (privat.auth/auth app-db)
    (-> (privat.auth/auth-p24 app-db)
        (get-in [:privat :session])
        (update :expires #(time.coerce/from-long (* % 1000)))
        utils/map-to-html-list)))
