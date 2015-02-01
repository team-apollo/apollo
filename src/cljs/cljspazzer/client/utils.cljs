(ns cljspazzer.client.utils
  (:require [goog.string :as gstring]
            [goog.string.format]))

(def encode js/encodeURIComponent)
(def format gstring/format)

(defn format-duration [d]
  (let [hours (quot d 3600)
        r (rem d 3600)
        minutes (quot r 60)
        seconds (rem r 60)]
    (if (> hours 0)
      (format "%i:%02i:%02i" hours minutes seconds)
      (format "%i:%02i" minutes seconds))))
