(ns apollo.utils
  (:require [clojure.string :as s]
            [pantomime.mime :refer [mime-type-of extension-for-name]]
            [digest]))

(defn starts-with? [s prefix]
  (if (> (count prefix) (count s))
    false
    (= prefix (subs s 0 (count prefix)))))

(defn is-image? [f]
  (and (.isFile f)
       (= "image" (subs (mime-type-of f) 0 5))))

(defn get-extension-for-mime [mime-type]
  (cond
    (= mime-type "audio/mpeg") ".mp3"
    :else (extension-for-name mime-type)))

(defn get-extension [path]
  (let [mime-type (mime-type-of path)]
    (get-extension-for-mime mime-type)))

(defn track-file-name [track]
  (let [{:keys [artist_canonical album_canonical title_canonical path track]} track]
    (format "%02d - %s - %s - %s%s"
            (or track 0)
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

(defn chk-sum-str [s]
  (digest/md5 (canonicalize s)))
