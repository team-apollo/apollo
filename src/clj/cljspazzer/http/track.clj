(ns cljspazzer.http.track
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :refer [the-db track-by-artist-by-album]]
            [clojure.java.io :as io]
            [cljspazzer.utils :as utils]))

(defn track-detail [artist album id]
  (let [result (track-by-artist-by-album the-db artist album id)
        path (:path result)]
    (if (not (nil? path))
      (let [file-name (utils/track-file-name result)]
        {:headers {"Content-Disposition" (format "attachment;filename=%s" file-name)}
         :body (io/file path)})
      {:status 404})
    ))
