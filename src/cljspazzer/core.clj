(ns cljspazzer.core
  (:require  [ring.adapter.jetty9 :as j]
             [ring.middleware.resource :as m]
             [compojure.core :refer :all]
             [compojure.route :as route]
             [compojure.response :refer [render]]
             [clojure.java.io :as io])
  (:gen-class :main true))

(defroutes app
  (GET "/" [] {:status 302
               :headers {"Location" "/index.html"}}))

(defn -main
  "Launch the web server app"
  [& args]
  (j/run-jetty (m/wrap-resource app "public") {:port 5050}))
