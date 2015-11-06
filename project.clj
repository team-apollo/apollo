(def cljs-src "src/cljs")
(def clj-src "src/clj")
(def src-paths [cljs-src clj-src])
(def cljs-output-dir "resources/public/javascripts/apollo")
(def cljs-output-to (format "%s/main.js" cljs-output-dir))
(def handler 'apollo.core/app)
(def port 5050)
(def nrepl-port 9998)
(def sass-src "resources/sass")
(def sass-output-dir "resources/public/css")

(defproject apollo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "undefined"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring-partial-content "1.0.0"]
                 [compojure "1.4.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [org/jaudiotagger "2.0.3"]
                 [claudio "0.1.3"]
                 [com.novemberain/pantomime "2.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.5.1"]
                 [org.omcljs/om "0.9.0"]
                 [bk/ring-gzip "0.1.1"]
                 [sablono "0.4.0"]
                 [org.clojure/core.async "0.2.371"]
                 [clj-http "2.0.0"]
                 [digest "1.4.4"]
                 [figwheel "0.5.0-SNAPSHOT"]
                 [environ "1.0.1"]
                 [enlive "1.1.6"]
                 [clj-time "0.11.0"]
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [org.apache.commons/commons-compress "1.10"] ;;temporary
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [log4j/log4j "1.2.17"]
                 [korma "0.4.2"]
                 [honeysql "0.6.2"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]]
  :source-paths ~src-paths
  :plugins [[lein-ring "0.8.13"]
            [lein-cljsbuild "1.1.1-SNAPSHOT"]
            [lein-haml-sass "0.2.7-SNAPSHOT"]
            [lein-environ "1.0.0"]]
  :ring {:handler ~handler
         :port ~port
         :init apollo.core/initialize
         :nrepl {:start? true :port ~nrepl-port}}
  :clean-targets ^{:protect false} [~cljs-output-dir]
  :profiles {:dev {;; :repl-options {:init-ns apollo.core}
                   :plugins [[lein-figwheel "0.5.0-SNAPSHOT"]]
                   :hooks [leiningen.cljsbuild
                           leiningen.sass]
                   :env {:is-dev true}
                   :figwheel {:http-server-root "public"
                              :server-port ~port
                              :css-dirs [~sass-output-dir]
                              :ring-handler ~handler
                              :nrepl-port ~nrepl-port}
                   :cljsbuild {:builds
                               {:app {:source-paths [~cljs-src "env/dev/cljs"]
                                      :figwheel true
                                      :compiler {:output-to ~cljs-output-to
                                                 :optimizations :none
                                                 :cache-analysis true
                                                 :pretty-print true
                                                 :pseudo-names true
                                                 :source-map true
                                                 :output-dir ~cljs-output-dir}}}}}
             :uberjar {:hooks [leiningen.cljsbuild
                               leiningen.sass]
                       :aot :all
                       :cljsbuild {:builds
                                   [{:source-paths [~cljs-src]
                                     :compiler {:output-to ~cljs-output-to
                                                :optimizations :advanced
                                                :cache-analysis true
                                                :pretty-print false
                                                :pseudo-names false}}]}}}  
  :sass {:src ~sass-src
         :output-directory ~sass-output-dir
         :output-extension "css"})
