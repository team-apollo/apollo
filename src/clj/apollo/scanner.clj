(ns apollo.scanner
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [apollo.db.schema :as db]
            [apollo.utils :refer [canonicalize]]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.coerce :as c])
  (:import org.jaudiotagger.audio.AudioFileFilter
           org.jaudiotagger.audio.AudioFileIO))

(.setLevel (java.util.logging.Logger/getLogger "org.jaudiotagger")
           java.util.logging.Level/OFF)

(def file-filter (new org.jaudiotagger.audio.AudioFileFilter false))

(def field-keys (map #(.name %) (.getEnumConstants org.jaudiotagger.tag.FieldKey)))

(defn get-tag-from-file [f]
  (.getTag (org.jaudiotagger.audio.AudioFileIO/read f)))

(defn ->constant [k]
  (.get (.getField org.jaudiotagger.tag.FieldKey k) nil))

(defn keywordify [k]
  (keyword (string/replace (string/lower-case k) #"_" "-")))

(defn retrieve-field [tag k]
  (let [v (try
            (.getFirst tag (->constant k))
            (catch java.lang.NullPointerException e
              (log/info (format "%s is null" k))
              nil))]
    (when-not (empty? v)
      (vector (keywordify k) v))))

(defn read-tag [f]
  (when-let [tag (.getTag (org.jaudiotagger.audio.AudioFileIO/read f))]
    (into {} (remove nil? (map (partial retrieve-field tag) field-keys)))))

(defn is-audio-file? [f]
  (.accept file-filter f))

(defn val-not-null? [kv]
  (not (= (last kv) "null")))

(defn hm-filter-null [hm]
  (into {} (filter val-not-null? hm)))

(defn get-audio-file-duration [f]
  (try
    (-> (AudioFileIO.) ;; sometimes this reads the wrong duration, wondering if it's a thread safety thing, need to ask smart people
        (.readFile f)
        .getAudioHeader
        .getTrackLength)
    (catch Exception e
      (log/error e (format "problems getting track duration from %s" (.getAbsolutePath f)))
      nil)))

(defn get-info [f]
  (log/info (format "reading tags from %s" (.getAbsolutePath f)))
  (let [id3tags (some identity [(try
                                  (read-tag f)
                                  (catch  Exception e
                                    (log/error e (format "problems reading tags from %s" (.getAbsolutePath f)))
                                    {}))
                                {}])
        id3tags-fixed (hm-filter-null id3tags)
        result (assoc id3tags-fixed
                      :path (.getAbsolutePath f)
                      :last_modified (.lastModified f)
                      :artist_canonical (canonicalize (:artist id3tags-fixed ""))
                      :album_canonical (canonicalize (:album id3tags-fixed ""))
                      :title_canonical (canonicalize (:title id3tags-fixed ""))
                      :duration (get-audio-file-duration f)
                      :disc_no (:disc-no id3tags-fixed nil))]
    (select-keys result
                 (db/column-names
                  (:tracks db/tables)))))

(defn mk-need-info
  ([last-modified-index]
   (fn [f]
     (let [abs-path (.getAbsolutePath f)
           last-modified (.lastModified f)]
       (or (nil? (last-modified-index abs-path))
           (not (= last-modified (last-modified-index abs-path)))))))
  ([] (mk-need-info (db/last-modified-index db/the-db))))

(defn file-tag-seq
  ([d file-predicate?]
   (let [directory (io/file d)
         files (file-seq directory)
         active-predicate? (fn [f] (and
                                    (.exists f)
                                    (.isFile f)
                                    (file-predicate? f)
                                    (is-audio-file? f)))
         audio-files (filter active-predicate? files)]
     (pmap get-info audio-files)))
  ([d] (file-tag-seq d (fn [f] true))))


(defn process-dir! [d]
  (let [scan-date (c/to-long (t/now))
        fseq (file-tag-seq d (mk-need-info))
        upsert (partial db/upsert-track! scan-date)
        work-load (partition 100 (map upsert fseq))
        worker (map (fn [w] (transaction (dorun w))) work-load)]
    (dorun worker)))

(defn process-mounts! []
  (let [m (db/mount-points)]
    (dorun (db/prune-tracks!))
    (dorun (map process-dir! m))))
