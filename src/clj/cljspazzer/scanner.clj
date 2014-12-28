(ns cljspazzer.scanner
  (:require [clojure.java.io :as io]
            [claudio.id3 :as id3] ;; might want to ditch this, it's pretty limited
            [clojure.string :as s]
            [cljspazzer.db.schema :as db])
  (:import org.jaudiotagger.audio.AudioFileFilter))

(def file-filter (new org.jaudiotagger.audio.AudioFileFilter false))

(defn is-audio-file? [f]
  (.accept file-filter f))

(defn val-not-null? [kv]
  (not (= (last kv) "null")))

(defn hm-filter-null [hm]
  (into {} (filter val-not-null? hm)))

(defn get-info [f]
  (let [id3tags (id3/read-tag f)
        id3tags-fixed (hm-filter-null id3tags)
        result (assoc id3tags-fixed
                      :path (.getAbsolutePath f)
                      :last_modified (.lastModified f)
                      :artist_canonical (s/trim (s/lower-case (:artist id3tags-fixed "")))
                      :album_canonical (s/trim (s/lower-case (:album id3tags-fixed "")))
                      :title_canonical (s/trim (s/lower-case (:title id3tags-fixed "")))
                      :disc_no (:disc-no id3tags-fixed))]
    (select-keys result
                 (db/column-names
                  (:tracks db/tables)))))

(defn file-tag-seq [d]
  (let [lmidx (db/last-modified-index db/the-db)
        need-info? (fn [f] (and (is-audio-file? f)
                                (or (nil? (lmidx (.getAbsolutePath f)))
                                    (not (= (.lastModified f) (lmidx (.getAbsolutePath f)))))))
        audio-files (filter need-info? (file-seq (io/file d)))]
    (pmap get-info audio-files)))
