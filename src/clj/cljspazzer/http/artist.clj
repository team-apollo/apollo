(ns cljspazzer.http.artist
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as s]
            [cljspazzer.utils :as utils]
            [cljspazzer.images :as images]))

(defn artists-detail [id]
  (let [result (s/album-list-by-artist s/the-db id)]
    (if (> (count result) 0)
      (response {:artist (utils/canonicalize id)
                 :albums result})
      {:status 404})))

(defn artists-index [req]
  (response {:artists (s/artist-list s/the-db)}))

(defn artist-search [prefix]
  (response {:artists (s/artist-search s/the-db prefix)}))


(defn artist-image [artist]
  (let [goog-result (:url (last (sort-by :width (filter (fn [r] (<= (Integer/parseInt (:width r)) 1024)) (images/goog-artist-images artist)))))]
    {:status 302
     :headers {"Location" goog-result}}))
