(ns privat-manager.utils
  (:require [hiccup.core])) 

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
