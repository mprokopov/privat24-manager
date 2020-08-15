(defproject privat-manager "2.2.0"
  :description "Import bank statements from Privat24 to Manager.io API"
  :url "http://github.com/mprokopov/privat24-manager"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                ;  [debug-middleware #=(eval (System/getenv "DEBUG_MIDDLEWARE_VERSION"))]
                 [cheshire "5.7.1"]
                 [clj-http "3.6.1"]
                 [clj-time "0.14.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [cljfmt "0.6.4"]
                 [org.clojure/test.check "0.9.0" :scope "test"]
                 ;[ring/ring-jetty-adapter "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.service "0.5.2"]
                 [io.pedestal/pedestal.jetty "0.5.2"]
                 [io.pedestal/pedestal.route "0.5.2"]
                 [io.pedestal/pedestal.interceptor "0.5.2"]
                 [ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/jul-to-slf4j "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]
                 [reloaded.repl "0.2.4"]
                 [org.clojure/core.async "0.4.474"]
                 [environ "1.1.0"]]
                 ;[io.pedestal/pedestal.log "0.5.2"]]

  :main ^:skip-aot privat-manager.core
  :target-path "target/%s"
  :ring {:handler privat-manager.core/handler}
  :resource-paths ["config", "resources", "resources/REBL-0.9.220.jar" ]
  :plugins [
            ;[lein-figwheel "0.5.8"]
            [lein-bower "0.5.1"]
            [lein-environ "1.1.0"]
            [lein-ring "0.9.7"]]
  :profiles {:uberjar {:aot :all
                       :uberjar-name "privat-manager-standalone.jar"
                       }
             :repl {:plugins [[cider/cider-nrepl "0.20.0"]]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [com.stuartsierra/component.repl "0.2.0"]]
                   ;; :env {:manager-endpoint "http://manager.it-premium.local:8088/api/"}
                   :env {:manager-endpoint "http://192.168.180.31:8080/api/"}
                   :main user
                   :source-paths ["dev"]}})
