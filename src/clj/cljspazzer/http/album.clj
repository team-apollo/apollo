(ns cljspazzer.http.album
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as s]))

(defn album-detail [artist-id album-id]
  (let [db-result (s/tracks-by-album s/the-db album-id)
        first-result (first db-result)
        {artist :artist_canonical album :album_canonical year :year} first-result]
    (if (> (count db-result) 0)
      (response {:album {:artist artist
                     :name album
                     :year year
                         :tracks (map (fn [r] {:track r}) db-result)}})
      {:status 404})))
