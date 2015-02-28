(ns apollo.client.utils
  (:require [om.core :as om :include-macros true]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.dom :as dom]))

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

(defn s-contains? [s k]
  (not (= (count (filter (fn [t](= k t)) s)) 0)))


(defn in-view-port [owner]
  (let [rect (.getBoundingClientRect (om/get-node owner))
        vpSize (dom/getViewportSize js/window)
        view-port-height (.-height vpSize)
        view-port-width (.-width vpSize)]
    (and (>= (.-bottom rect) 0)
         (>= (.-right rect) 0)
         (<= (.-top rect) view-port-height)
         (<= (.-left rect) view-port-width))))
