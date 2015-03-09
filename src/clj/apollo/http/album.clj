(ns apollo.http.album
  (:require [ring.util.response :refer [response, file-response header]]
            [apollo.db.schema :as s]
            [apollo.utils :as utils]
            [apollo.images :as images]
            [apollo.http.cache :as cache]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [pantomime.mime :refer [mime-type-of]])
  (:import
    [java.io FileOutputStream ByteArrayOutputStream ByteArrayInputStream]
    [java.util.zip ZipEntry ZipFile ZipOutputStream]))

(defn is-compilation? [tracks]
  (let [artists (map :artist_canonical tracks)
        first-artist (first artists)
        years (map :year tracks)
        first-year (first years)
        albums (map :album_canonical tracks)
        first-album (first albums)
        same-artist? (every? (fn [a] (= a first-artist)) artists)
        same-year? (every? (fn [y] (= y first-year)) years)
        same-album? (every? (fn [al] (= al first-album)) albums)
        tracks-grouped-by-artist (group-by :artist_canonical tracks)
        track-counts-by-artist (into {} (map (fn [[k v]] [k (count v)]) tracks-grouped-by-artist))]
    (if (and (not same-artist?)
             same-year?
             same-album?)
      (not-any? (fn [t-count] (> t-count 5)) (vals track-counts-by-artist))
      false)))

(defn just-artist [artist-id] (fn [t] (= (utils/canonicalize artist-id) (:artist_canonical t))))

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
  (let [db-result-raw (s/tracks-by-album s/the-db album-id)
        db-result (if (is-compilation? db-result-raw) db-result-raw (filter (just-artist artist-id) db-result-raw))
        artists (map :artist_canonical  db-result)
        albums (map :album_canonical db-result)
        paths (map :path db-result)
        result-file-name (format "%s - %s.zip"
                                 (utils/canonicalize artist-id)
                                 (utils/canonicalize album-id))
        zip-entries (into {} (map (fn [p] {(:path p)
                                           {:zip-entry (new ZipEntry (format "%s" (utils/track-file-name p)))
                                            :file (io/file (:path p))}}) db-result))
        zip-bytes (make-zip-file zip-entries)]
    {:body (new ByteArrayInputStream zip-bytes)
     :headers {"Content-Disposition" (format "attachment;filename=\"%s\"" result-file-name)
               "Content-Type" (mime-type-of zip-bytes)}}))

(defn images-for-tracks [tracks]
  (let [parents (into #{} (map (fn [t] (.getParentFile (io/file (:path t)))) tracks))
        result (filter utils/is-image? (flatten (map (fn [p] (seq (.listFiles p))) parents)))]
    result))

(defn first-album-image-from-google [artist album]
  (let [urls (map :url (images/goog-album-images artist album))
        cacher (fn [url] (cache/cache-image-response url artist album))]
    (log/info (format "attempting to get image from internet for %s %s" artist album))
    (first (drop-while nil? (map cacher urls)))))


(defn album-image [artist-id album-id]
  (let [tracks (filter (fn [t]
                         (= (utils/canonicalize artist-id) (:artist_canonical t)))
                       (s/tracks-by-album s/the-db album-id))
        img (or (first (images-for-tracks tracks))
                (images/image-from-cache artist-id album-id)
                (first-album-image-from-google artist-id album-id))]
    (header (file-response (.getAbsolutePath img)) "Content-Type" (mime-type-of img))))


(defn album-detail [artist-id album-id]
  (let [db-result (s/tracks-by-album s/the-db album-id)
        first-result (first db-result)
        {artist :artist artist_canonical :artist_canonical album :album album_canonical :album_canonical year :year} first-result
        compilation? (is-compilation? db-result)
        tracks (if compilation?
                 db-result
                 (filter (just-artist artist-id) db-result))]
    (if (> (count db-result) 0)
      (response {:album {:artist artist
                         :artist_canonical artist_canonical
                         :compilation compilation?
                         :name album
                         :album_canonical album_canonical
                         :year year
                         :tracks (map (fn [r] {:track r}) tracks)}})
      {:status 404})))


(defn recently-added []
  (let [db-result (s/get-albums-recently-added s/the-db (* 5 365))]
    (if (> (count db-result) 0)
      (response {:albums db-result})
      {:status 404})))
