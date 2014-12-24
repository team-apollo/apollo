(ns cljspazzer.http.track
  (:require [ring.util.response :refer [response]]))

(defn track-detail [artist-id album-id track-id]
  (response {:track
             {:id 1
              :num 1
              :disc 1
              :title "Catatonic"
              :details_url "/api/artists/1/albums/1/tracks/1"}}))
