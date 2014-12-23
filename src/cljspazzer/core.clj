(ns cljspazzer.core
  (:require  [ring.adapter.jetty :as j])
  (:gen-class :main true))


(defn app-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain;=us-ascii"}
   :body (str request)})

(defn -main
  "Launch the web server app"
  [& args]
  (j/run-jetty app-handler {:port 3000}))
