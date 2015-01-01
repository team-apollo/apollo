(defproject cljspazzer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "undefined"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.3.1"]
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [org/jaudiotagger "2.0.3"]
                 [claudio "0.1.2"]
                 [com.novemberain/pantomime "2.3.0"]
                 [org.clojure/tools.logging "0.3.1"]]
  :source-paths ["src/clj"
                 "src/cljs"]
  :plugins [[lein-ring "0.8.13"]
            [lein-cljsbuild "1.0.4-SNAPSHOT"]]
  :hooks [leiningen.cljsbuild]
  :ring {:handler cljspazzer.core/app
         :port 5050}
  :cljsbuild {:builds
              [{:source-paths ["src/cljs"]
                :compiler
                {:output-to "resources/public/javascripts/main.js"
                 :optimizations :whitespace
                 :pretty-print true}}]})
