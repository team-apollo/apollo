(ns apollo.http.album
  (:require [ring.util.response :refer [response, file-response header]]
            [apollo.db.schema :as s]
            [apollo.utils :as utils]
            [apollo.images :as images]
            [apollo.http.cache :as cache]
            [apollo.musicbrainz :refer [get-release-artwork]]
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

(defn album-zip [{cn :db-connection
                  {:keys [artist-id album-id]} :params
                  :as request}]
  (let [db-result-raw (s/tracks-by-album cn album-id)
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
    (println "results for image files in tracks directories")
    result))

(defn first-album-image-from-google [artist album]
  (let [urls (map :url (images/goog-album-images artist album))
        cacher (fn [url] (cache/cache-image-response url artist album))]
    (log/info (format "attempting to get image from internet for %s %s" artist album))
    (first (drop-while nil? (map cacher urls)))))

(defn artwork-from-musicbrainz [artist album]
  (let [url (get-release-artwork artist album)]
    (if url
      (doall
       (log/info (format "attempting to get image from internet for %s %s" artist album))
       (cache/cache-image-response url artist album))
      (log/info (format "no image found for %s %s from musicbrainz" artist album)))))

(defn album-image [{cn :db-connection
                    {:keys[artist-id album-id]} :params}]
  (let [tracks (filter (fn [t]
                         (= (utils/canonicalize artist-id) (:artist_canonical t)))
                       (s/tracks-by-album cn album-id))
        files  (map :path tracks)
        the-files (map io/file files)
        artwork-candidate (first (flatten (map images/get-artwork-from-file the-files)))]
    (if (nil? artwork-candidate)
      (let [img-candidate (or (first (images-for-tracks tracks))
                              (images/image-from-cache artist-id album-id)
                              (artwork-from-musicbrainz artist-id album-id))]
        (if img-candidate
          (let [[img-response mime-type] [(file-response (.getAbsolutePath img-candidate))
                                          (mime-type-of img-candidate)]]
            (header img-response "Content-Type" mime-type))
          {:status 404}))
      (header {:body (new ByteArrayInputStream (.getBinaryData artwork-candidate))} "Content-Type" (.getMimeType artwork-candidate)))))


(defn album-detail [{cn :db-connection
                     {:keys [artist-id album-id]} :params
                     :as request}]
  (let [db-result (s/tracks-by-album cn album-id)
        first-result (first db-result)
        {artist :artist artist_canonical :artist_canonical album :album album_canonical :album_canonical year :year} first-result
        compilation? (is-compilation? db-result)
        tracks (if compilation?
                 db-result
                 (filter (just-artist artist-id) db-result))]
    (if (> (count db-result) 0)
      (response {:album {:artist artist
                         :artist_id artist_canonical
                         :compilation compilation?
                         :name album
                         :id album_canonical
                         :year year
                         :tracks (map (fn [r] {:track r}) tracks)}})
      {:status 404})))


(defn recently-added [{cn :db-connection}]
  (let [db-result (s/get-albums-recently-added cn (* 5 365))]
    (if (> (count db-result) 0)
      (response {:albums db-result})
      {:status 404})))

(defn albums-by-year [{cn :db-connection}]
  (let [db-result (s/get-albums-by-year cn)]
    (if (> (count db-result) 0)
      (response {:albums db-result})
      {:status 404})))
