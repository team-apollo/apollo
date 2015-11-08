(ns apollo.scanner
  (:require [apollo.db.schema :as db]
            [apollo.utils :refer [canonicalize]]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [clojure.java.jdbc :refer [with-db-transaction]]
            [clojure.string :as s]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
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
  [cn]
   (let [last-modified-index (db/last-modified-index cn)]
     (fn [f]
     (let [abs-path (.getAbsolutePath f)
           last-modified (.lastModified f)]
       (or (nil? (last-modified-index abs-path))
           (not (= last-modified (last-modified-index abs-path))))))))

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


(defn process-tracks! [cn scan-date tracks]
  (with-db-transaction [d cn]
    (let [upsert (partial db/upsert-track! d scan-date)
          work (map upsert tracks)]
      (dorun work))))

(defn process-dir! [cn d]
  (let [scan-date (c/to-long (t/now))
        fseq (file-tag-seq d (mk-need-info cn))
        work-load (partition-all 150 fseq)
        worker (map (partial process-tracks! cn scan-date) work-load)]
    (dorun worker)))

(defn process-mounts! [cn]
  (let [m (db/mount-points cn)]
    (dorun (db/prune-tracks! cn))
    (dorun (map (partial process-dir! cn) m))))
