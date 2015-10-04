(ns apollo.http.track
  (:require [ring.util.response :refer [response file-response header]]
            [apollo.db.schema :refer [track-by-artist-by-album]]
            [clojure.java.io :as io]
            [apollo.utils :as utils]
            [pantomime.mime :refer [mime-type-of]]
            [clojure.tools.logging :as log]))

(defn track-detail [artist album id]
  (let [result (track-by-artist-by-album artist album id)
        path (.getAbsolutePath (io/file (:path result)))]
    (if (not (nil? path))
      (let [file-name (utils/track-file-name result)
            _ (log/info file-name)]
        (header
         (header (file-response path)
                 "Content-Disposition" (format "attachment;filename=\"%s\"" file-name))
         "Content-Type" (mime-type-of path)))
      {:status 404})))
