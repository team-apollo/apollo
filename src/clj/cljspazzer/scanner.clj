(ns cljspazzer.scanner
  (:require [clojure.java.io :as io]
            [claudio.id3 :as id3] ;; might want to ditch this, it's pretty limited
            )
  (:import org.jaudiotagger.audio.AudioFileFilter))

(def file-filter (new org.jaudiotagger.audio.AudioFileFilter false))

(defn is-audio-file? [f]
  (.accept file-filter f))

(defn val-not-null? [kv]
  (not (= (last kv) "null")))

(defn hm-filter-null [hm]
  (apply hash-map
         (flatten
          (filter val-not-null?
                  (seq hm)))))

(defn get-info [f]
  (let [id3tags (id3/read-tag f)
        id3tags-fixed (hm-filter-null id3tags)]
    (assoc id3tags-fixed
           :path (.getAbsolutePath f)
           :last-modified (.lastModified f))))

(defn file-tag-seq [d]
  (let [audio-files (filter is-audio-file? (file-seq (io/file d)))]
    (pmap get-info audio-files)))
