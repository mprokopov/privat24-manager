(defproject privat-manager "1.0.1"
  :description "Import bank statements from Privat24 to Manager.io API"
  :url "http://github.com/mprokopov/privat24-manager"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.7.1"]
                 [clj-http "3.6.1"]
                 [clj-time "0.13.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.6.1"]
                 [ring/ring-defaults "0.3.0"]]
  :main ^:skip-aot privat-manager.core
  :target-path "target/%s"
  :ring {:handler privat-manager.core/handler}
  :plugins [
            ;[lein-figwheel "0.5.8"]
            [lein-bower "0.5.1"]
            [lein-ring "0.9.7"]]
  :profiles {:uberjar {:aot :all}})
