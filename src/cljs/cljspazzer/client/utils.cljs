(ns cljspazzer.client.utils
  (:require [goog.string :as gstring]
            [goog.string.format]))

(def encode js/encodeURIComponent)
(def format gstring/format)
