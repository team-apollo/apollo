(ns apollo.http.artist
  (:require [ring.util.response :refer [response file-response header]]
            [apollo.db.schema :as s]
            [apollo.utils :as utils]
            [apollo.images :as images]
            [pantomime.mime :refer [mime-type-of]]
            [apollo.http.cache :as cache]
            [clojure.tools.logging :as log]))

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

(defn first-artist-image-from-google [artist]
  (let [urls (map :url (images/goog-artist-images artist))
        cacher (fn [url] (cache/cache-image-response url artist))]
    (log/info (format "attempting to get image from internet for %s" artist))
    (first (drop-while nil? (map cacher urls)))))

(defn artist-image [artist force-fetch]
  (let [img (or (images/image-from-cache artist)
                (if (not (nil? force-fetch))
                  (first-artist-image-from-google artist)))]
    (if (not (nil? img))
      (header (file-response (.getAbsolutePath img)) "Content-Type" (mime-type-of img))
      {:status 404})))


