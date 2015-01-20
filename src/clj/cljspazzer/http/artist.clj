(ns cljspazzer.http.artist
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as s]
            [cljspazzer.utils :as utils]
            [cljspazzer.images :as images]
            [pantomime.mime :refer [mime-type-of]]
            [cljspazzer.http.cache :as cache]
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

(defn artist-image [artist force-fetch]
  (let [cache-image (images/image-from-cache artist)]
    (if (nil? cache-image)
      (if (not (nil? force-fetch))
        (do
        (log/info (format "attempting to get image from internet for %s" artist))
        (let [urls (map :url (images/goog-artist-images artist))
            cacher (fn [url]
                     (cache/cache-image-response url artist))
              goog-image (first (drop-while nil? (map cacher urls)))]
          (if (not (nil? goog-image))
            {:body goog-image :headers {"Content-Type" (mime-type-of goog-image)}}
            {:status 404})))
        {:status 404})
      {:body cache-image :headers {"Content-Type" (mime-type-of cache-image)}})))


