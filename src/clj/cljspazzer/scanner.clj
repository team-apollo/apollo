(ns cljspazzer.scanner
  (:require [clojure.java.io :as io]
            [claudio.id3 :as id3] ;; might want to ditch this, it's pretty limited
            )
  (:import org.jaudiotagger.audio.AudioFileFilter))

(def file-filter (new org.jaudiotagger.audio.AudioFileFilter false))

(defn is-audio-file? [f]
  (.accept file-filter f))

(defn get-info [f]
  (let [id3tags (id3/read-tag f)
        id3tags-fixed (apply hash-map
                             (flatten
                              (filter (fn [x] (not (= (last x) "null")))
                                      (seq id3tags))))]
    (assoc id3tags-fixed
           :path (.getAbsolutePath f)
           :last-modified (.lastModified f))))

(defn file-tag-seq [d]
  (let [audio-files (filter is-audio-file? (file-seq (io/file d)))]
    (pmap get-info audio-files)))
