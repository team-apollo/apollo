(ns cljspazzer.http.artist
  (:require [ring.util.response :refer [response]]))

(defn artists-detail [id]
  (response {:artist
             {:id 1
              :name "...And You Will Know Us by the Trail of Dead"
              :albums [{:album
                        {:id 1
                         :name "Madonna"
                         :year 1998
                         :details_url "/api/artists/1/albums/1"
                         :tracks [{:track
                                   {:id 1
                                    :num 1
                                    :disc 1
                                    :title "Catatonic"
                                    :details_url "/api/artists/1/albums/1/tracks/1"}}]}}]}}))

(defn artists-index [req]
  (response {:artists
             [{:artist
               {:id 1
                :name "...And You Will Know Us by the Trail of Dead"
                :details_url "/api/artists/1"}}]}))
