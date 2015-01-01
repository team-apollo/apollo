(ns cljspazzer.http.artist
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as s]
            [cljspazzer.utils :as utils]))

(defn artists-detail [id]
  (let [result (s/album-list-by-artist s/the-db id)]
    (if (> (count result) 1)
      (response {:artist (utils/canonicalize id)
                 :albums result})
      {:status 404})))

(defn artists-index [req]
  (response {:artists (s/artist-list s/the-db)}))
