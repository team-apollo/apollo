(ns apollo.images
  (:require [clj-http.client :as client]
            [apollo.http.cache :as cache]
            [apollo.utils :as utils]
            [apollo.scanner :refer [get-tag-from-file]]
            [clojure.tools.logging :as log]))

(def goog-image-qry-url "http://ajax.googleapis.com/ajax/services/search/images")

;; these functions are potentially expensive and flaky, should only be
;; used as a last resort

(defn goog-images [qry-phrase]
  (log/info (format "querying google ... '%s'" qry-phrase))
  (let [response (client/get goog-image-qry-url {:query-params {:q qry-phrase
                                                 :v "1.0"
                                                 :rsz "8"}
                                  :as :json})
        results (:results (:responseData (:body response)))]
    (take 10 results)))

(defn image-response [response]
  (select-keys response [:url :width :height]))

(defn goog-artist-images [artist]
  (let [qry-phrase [ "photo of the band %s" "photo of %s performing"]
        results (goog-images
                 (apply str (interpose " or " (map (fn [q] (format q artist)) qry-phrase))))
        image-urls (map image-response results)]
    image-urls))

(defn goog-album-images [artist album]
  (let [qry-phrase "%s by %s cover art photo"
        results (goog-images (format qry-phrase album artist))
        image-urls (map image-response results)]
    image-urls))

(defn image-from-cache [& kys]
  (let [root (cache/cache-root)
        k (str (apply cache/make-key kys) ".")
        candidates (filter (fn [f] (utils/starts-with? (.getName f) k)) (seq (.listFiles root)))
        result (first (reverse (sort-by (fn [f] (.lastModified f)) candidates)))]
    (println "image from cache " result)
    result))

(defn get-artwork-from-file [f]
  (seq (.getArtworkList (get-tag-from-file f))))
