(ns cljspazzer.core
  (:require [ring.middleware.resource :as m]
            [compojure.core :refer :all]
            [compojure.response :refer [render]]
            [ring.middleware.json :as j]
            [ring.util.response :refer [response]]))
(defn track-detail [artist-id album-id track-id]
  (response {:track {:id 1
                     :num 1
                     :disc 1
                     :title "Catatonic"
                     :details_url "/api/artists/1/albums/1/tracks/1"}}))

(defn album-detail [artist-id album-id]
  (response {:album {:id 1
                     :name "Madonna"
                     :year 1998
                     :details_url "/api/artists/1/albums/1"
                     :tracks [{:track {:id 1
                                       :num 1
                                       :disc 1
                                       :title "Catatonic"
                                       :details_url "/api/artists/1/albums/1/tracks/1"}}]}}))

(defn artists-detail [id]
  (response {:artist {:id 1
                      :name "...And You Will Know Us by the Trail of Dead"
                      :albums [{:album {:id 1
                                        :name "Madonna"
                                        :year 1998
                                        :details_url "/api/artists/1/albums/1"
                                        :tracks [{:track {:id 1
                                                          :num 1
                                                          :disc 1
                                                          :title "Catatonic"
                                                          :details_url "/api/artists/1/albums/1/tracks/1"}}]}}]}}))

(defn artists-index [req]
  (response {:artists
             [{:artist {:id 1
                        :name "...And You Will Know Us by the Trail of Dead"
                        :details_url "/api/artists/1"}}]}))

(defroutes app-handler
  (GET "/api/artists/:artist-id/albums/:album-id/tracks/:track-id"
       [artist-id album-id track-id]
       (track-detail artist-id album-id track-id))
  (GET "/api/artists/:artist-id/albums/:album-id"
       [artist-id album-id]
       (album-detail artist-id album-id))
  (GET "/api/artists/:artist-id"
       [artist-id]
       (artists-detail artist-id))
  (GET "/api/artists"
       []
       artists-index)
  (GET "/"
       []
       {:status 302
        :headers {"Location" "/index.html"}}))

(def app
  (-> app-handler
      (j/wrap-json-response)
      (m/wrap-resource "public")))

