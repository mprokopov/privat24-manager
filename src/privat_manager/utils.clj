(ns privat-manager.utils
  (:require [hiccup.core]
            [clj-time.format :as time.format]) 
  (:import java.util.Locale))

(def custom-formatter (time.format/formatter "YYYYMMdd'T'HH:mm:ss"))

(def custom-datetime-formatter (time.format/formatter "dd.MM.YYYY HH:mm:ss"))

(def custom-ru-formatter (time.format/formatter "dd.MM.yy в HH:mm"))

(def custom-date-formatter (time.format/formatter "dd.MM.yy"))

(defn my-format [fmt n & [locale]]
  (let [locale (if locale (Locale. locale)
                   (Locale/getDefault))]
    (String/format locale fmt (into-array Object [n]))))

(defn map-to-html-list
  "Clojure map to nested HTML list. Optional list-type and wrapper params taken as keywords hiccup understands, and optional sep parameter for the string to show key-value associations"
  ([m] (map-to-html-list m :ul :span " => "))
  ([m list-type] (map-to-html-list m list-type :span " => "))
  ([m list-type wrapper] (map-to-html-list m list-type wrapper " => "))
  ([m list-type wrapper sep]
   (hiccup.core/html
    [list-type
     (for [[k v] m]
       [:li
        [wrapper (str k sep
                      (if (map? v)
                        (map-to-html-list v list-type wrapper sep)
                        v))]])])))


(defn parse-edrpou-statements [{{{statements :mstatements} :db} :manager}]
  (if statements
    [:table.table
     [:thead
      [:tr
       [:th "Клиент"]
       [:th "ЕДРПОУ"]
       [:th "Поставщик"]
       [:th "ЕДРПОУ"]]]
     (for [statement statements] 
       [:tr
        [:td (get-in statement [:debit :name])]
        [:td (get-in statement [:debit :edrpou])]
        [:td (get-in statement [:credit :name])]
        [:td (get-in statement [:credit :edrpou])]])]
    [:p "Необходимо загрузить выписки"]))

(defn format-floats [m]
  (let [f (fn [acc mkey mval] (assoc acc mkey (if (float? mval) (format "%.2f" mval) mval)))]
    (reduce-kv f {} m)))
