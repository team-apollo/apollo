(ns cljspazzer.images
  (:require [clj-http.client :as client]))

(def goog-image-qry-url "http://ajax.googleapis.com/ajax/services/search/images")

(defn goog-images [qry-phrase]
  (let [response (client/get goog-image-qry-url {:query-params {:q qry-phrase
                                                 :v "1.0"
                                                 :rsz "8"}
                                  :as :json})
        results (:results (:responseData (:body response)))
        ]
    (take 10 results))) ;; if it's not in the first 10, it likely is not relevant(arbitrary decision made by coder)

(defn image-response [response]
  (select-keys response [:url :width :height]))

(defn goog-artist-images [artist]
  (let [qry-phrase "%s band photo"
        results (goog-images (format qry-phrase artist))
        image-urls (map image-response results)]
    image-urls))


(defn goog-album-images [artist album]
  (let [qry-phrase "%s: %s cover art photo"
        results (goog-images (format qry-phrase artist album))
        image-urls (map image-response results)]
    image-urls))
