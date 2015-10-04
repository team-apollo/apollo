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
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.1"]
                 [org/jaudiotagger "2.0.3"]
                 [claudio "0.1.3"]
                 [com.novemberain/pantomime "2.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.3.14"]
                 [org.omcljs/om "0.9.0"]
                 [bk/ring-gzip "0.1.1"]
                 [sablono "0.3.6"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-http "2.0.0"]
                 [digest "1.4.4"]
                 [figwheel "0.4.0"]
                 [environ "1.0.1"]
                 [enlive "1.1.6"]
                 [clj-time "0.11.0"]
                 [com.andrewmcveigh/cljs-time "0.3.13"]
                 [org.apache.commons/commons-compress "1.10"] ;;temporary
                 ;; [org.clojure/tools.reader "0.9.2"] ;;temporary

                 [korma "0.4.2"]]
  :source-paths ~src-paths
  :plugins [[lein-ring "0.8.13"]
            [lein-cljsbuild "1.0.4"]
            [lein-haml-sass "0.2.7-SNAPSHOT"]
            [lein-environ "1.0.0"]]
  :ring {:handler ~handler
         :port ~port
         :init apollo.core/initialize
         :nrepl {:start? true :port ~nrepl-port}}
  :clean-targets ^{:protect false} [~cljs-output-dir]
  :profiles {:dev {:repl-options {:init-ns apollo.core}
                   :plugins [[lein-figwheel "0.4.0"]]
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
