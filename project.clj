(def cljs-src "src/cljs")
(def clj-src "src/clj")
(def src-paths [cljs-src clj-src])
(def cljs-output-dir "resources/public/javascripts/apollo")
(def cljs-output-to (format "%s/main.js" cljs-output-dir))
(def handler 'apollo.core/app)
(def port 5050)
(def sass-src "resources/sass")
(def sass-output-dir "resources/public/css")

(defproject apollo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "undefined"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring-partial-content "1.0.0"]
                 [compojure "1.3.1"]
                 [org.clojure/clojurescript "0.0-2755"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [org/jaudiotagger "2.0.3"]
                 [claudio "0.1.2"]
                 [com.novemberain/pantomime "2.3.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [secretary "1.2.1"]
                 [cljs-ajax "0.3.3"]
                 [org.omcljs/om "0.8.8"]
                 [bk/ring-gzip "0.1.1"]
                 [sablono "0.3.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-http "1.0.1"]
                 [digest "1.4.4"]
                 [figwheel "0.2.3-SNAPSHOT"]]
  :source-paths ~src-paths
  :plugins [[lein-ring "0.8.13"]
            [lein-cljsbuild "1.0.4"]
            [lein-haml-sass "0.2.7-SNAPSHOT"]
            [lein-figwheel "0.2.3-SNAPSHOT"]]
  :hooks [leiningen.cljsbuild
          leiningen.sass]
  :ring {:handler ~handler
         :port ~port}
  :clean-targets ^{:protect false} [~cljs-output-dir]
  :cljsbuild {:builds
              [{:source-paths [~cljs-src]
                :compiler {:output-to ~cljs-output-to
                           :optimizations :none
                           :cache-analysis true
                           :pretty-print true
                           :pseudo-names true
                           :source-map true
                           :output-dir ~cljs-output-dir}}]}
   :sass {:src ~sass-src
         :output-directory ~sass-output-dir
         :output-extension "css"})
