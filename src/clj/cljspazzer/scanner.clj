(ns cljspazzer.scanner
  (:require [clojure.java.io :as io]
            [claudio.id3 :as id3] ;; might want to ditch this, it's pretty limited
            [clojure.string :as s]
            [cljspazzer.db.schema :as db]
            [pantomime.mime :refer [mime-type-of]]
            [clojure.tools.logging :as log])
  (:import org.jaudiotagger.audio.AudioFileFilter))

(.setLevel (java.util.logging.Logger/getLogger "org.jaudiotagger")
           java.util.logging.Level/OFF)

(def file-filter (new org.jaudiotagger.audio.AudioFileFilter false))

(defn is-audio-file? [f]
  (.accept file-filter f))

(defn val-not-null? [kv]
  (not (= (last kv) "null")))

(defn hm-filter-null [hm]
  (into {} (filter val-not-null? hm)))

(defn canonicalize [s]
  (let [result (s/trim (s/lower-case s))]
    (if (and (> (count s) 4)
         (= "the " (subs result 0 4)))
      (s/trim (subs result 4))
      result)))

(defn get-info [f]
  (let [id3tags (try
                  (some identity [(id3/read-tag f) {}])
                  (catch Exception e
                    (log/error e (format "problems reading tags from %s" (.getAbsolutePath f)))
                    {}))
        id3tags-fixed (hm-filter-null id3tags)
        result (assoc id3tags-fixed
                      :path (.getAbsolutePath f)
                      :last_modified (.lastModified f)
                      :artist_canonical (canonicalize (:artist id3tags-fixed ""))
                      :album_canonical (canonicalize (:album id3tags-fixed ""))
                      :title_canonical (canonicalize (:title id3tags-fixed ""))
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
                                    (is-audio-file? f)
                                    (= (mime-type-of f) "audio/mpeg")))
         audio-files (filter active-predicate? files)]
     (pmap get-info audio-files)))
  ([d] (file-tag-seq d (fn [f] true))))


(defn process-dir! [d]
  (let [fseq (file-tag-seq d (mk-need-info))
        upsert (partial db/upsert-track! db/the-db)]
    (dorun (map  upsert fseq))))

(defn process-mounts! []
  (let [m (db/mount-points db/the-db)]
    (dorun (map process-dir! m))
    (dorun (db/prune-tracks! db/the-db))))



