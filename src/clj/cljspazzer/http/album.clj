(ns cljspazzer.http.album
  (:require [ring.util.response :refer [response]]))

(defn album-detail [artist-id album-id]
  (response {:album
             {:id 1
              :name "Madonna"
              :year 1998
              :details_url "/api/artists/1/albums/1"
              :tracks [{:track
                        {:id 1
                         :num 1
                         :disc 1
                         :title "Catatonic"
                         :details_url "/api/artists/1/albums/1/tracks/1"}}]}}))
