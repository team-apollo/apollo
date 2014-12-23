(ns cljspazzer.core
  (:require [ring.middleware.resource :as m]
            [compojure.core :refer :all]
            [compojure.response :refer [render]]))

(defroutes app-handler
  (GET "/" [] {:status 302
               :headers {"Location" "/index.html"}}))
(def app
  (m/wrap-resource app-handler "public"))

