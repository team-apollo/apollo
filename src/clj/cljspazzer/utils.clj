(ns cljspazzer.utils
  (:require [clojure.string :as s]
            [pantomime.mime :refer [mime-type-of extension-for-name]]))

(defn is-image? [f]
  (and (.isFile f)
       (= "image" (subs (mime-type-of f) 0 5))))

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

(defn canonicalize [s]
  (let [result (s/trim (s/lower-case s))]
    (if (and (> (count s) 4)
         (= "the " (subs result 0 4)))
      (s/trim (subs result 4))
      result)))
