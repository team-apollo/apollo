(ns cljspazzer.http.track
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :refer [the-db track-by-artist-by-album]]
            [clojure.java.io :as io]
            [cljspazzer.utils :as utils]
            [pantomime.mime :refer [mime-type-of extension-for-name]]))

(defn get-extension [path]
  (let [mime-type (mime-type-of path)]
    (cond
    (= mime-type "audio/mpeg") ".mp3"
    :else (extension-for-name mime-type))))

(defn track-file-name [track]
  (let [{:keys [artist_canonical album_canonical title_canonical path]} track]
    (format "%s - %s - %s%s"
            artist_canonical
            album_canonical
            title_canonical
            (get-extension path))))

(defn track-detail [artist album id]
  (let [result (track-by-artist-by-album the-db artist album id)
        path (:path result)]
    (if (not (nil? path))
      (let [file-name (track-file-name result)]
        {:headers {"Content-Disposition" (format "attachment;filename=%s" file-name)}
         :body (io/file path)})
      {:status 404})
    ))
