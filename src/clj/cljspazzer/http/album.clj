(ns cljspazzer.http.album
  (:require [ring.util.response :refer [response, file-response header]]
            [cljspazzer.db.schema :as s]
            [cljspazzer.utils :as utils]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [pantomime.mime :refer [mime-type-of]])
  (:import
    [java.io FileOutputStream ByteArrayOutputStream ByteArrayInputStream]
    [java.util.zip ZipEntry ZipFile ZipOutputStream]))


(defn make-zip-file [entries]
  (let [result (new ByteArrayOutputStream)
        z (new ZipOutputStream result)]
    (with-open [out z]
      (doseq [entry entries] ;;destructure?
        (let [v (val entry)
              zip-entry (:zip-entry v)
              file (:file v)]
          (.putNextEntry out zip-entry)
          (io/copy file out)
          (.closeEntry out))))
    (.toByteArray result)))

(defn album-zip [artist-id album-id]
  (let [db-result (s/tracks-by-album s/the-db album-id)
        artists (map :artist_canonical  db-result)
        albums (map :album_canonical db-result)
        paths (map :path db-result)
        result-file-name (format "%s - %s.zip"
                                 (utils/canonicalize artist-id)
                                 (utils/canonicalize album-id))
        zip-entries (into {}
                           (map (fn [p] {(:path p)
                                         {:zip-entry (new ZipEntry (utils/track-file-name p))
                                         :file (io/file (:path p))}}) db-result))
        zip-bytes (make-zip-file zip-entries)
        ]
    {
     :body (new ByteArrayInputStream zip-bytes)
     :headers {"Content-Disposition" (format "attachment;filename=%s" result-file-name)
               "Content-Type" (mime-type-of zip-bytes)}
     }
    ))

(defn images-for-tracks [tracks]
  (let [parents (into #{} (map (fn [t] (.getParentFile (io/file (:path t)))) tracks))
        result (filter utils/is-image? (flatten (map (fn [p] (seq (.listFiles p))) parents)))]
    result))

(defn album-image [artist-id album-id]
  (let [tracks (s/tracks-by-album s/the-db album-id)
        img (first (images-for-tracks tracks))]
    (if (nil? img)
      {:status 404}
      {:body img :headers {"Content-Type" (mime-type-of img)}})))

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
