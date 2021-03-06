(ns apollo.client.views.tracks
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.utils :as utils]
            [apollo.client.channels :as channels]
            [cljs.core.async :refer [<! put! chan]]
            [apollo.client.state :as state]))

(defn mk-track-url
  ([artist album track-id]
   (utils/format "/api/artists/%s/albums/%s/tracks/%s"
                 (utils/encode artist)
                 (utils/encode album)
                 (utils/encode track-id)))
  ([track]
   (let [t (:track track)
         artist (:artist t)
         album (:album t)
         id (:id t)]
     (mk-track-url artist album id))))

(defn track-number [track]
  (let [t (:track track)
        track-num (:track t)]
      (utils/format "%s." track-num)))

(defn track-label [track with-artist?]
  (let [t (:track track)
        track-num (:track t)
        track-title (:title t)
        artist (:artist t)]
    (if with-artist?
      (utils/format "%s by %s" track-title artist)
      (utils/format "%s" track-title))))

(defn playlist-track [track]
  (let [t (:track track)
        track-title (:title t)]
      (utils/format "%s" track-title)))

(defn artist [track with-artist?]
  (let [t (:track track)
        artist (:artist t)]
    (if with-artist?
      (utils/format "%s" artist))))

(defn track-detail [{:keys [track compilation?]} owner]
  (reify
    om/IRender
    (render [this]
      (let [t (:track track)
            duration (:duration t)
            track-number (track-number track)
            track-label (track-label track compilation?)
            now-playing (or (first (om/observe owner (state/ref-now-playing))) {})
            np-t (or (:track now-playing) {"id" nil})
            is-active? (= (:id np-t) (:id t))]
        (html
         [:li {:class-name (if is-active? "track-row active" "track-row")}
          [:a {:on-double-click (fn [e]
                           (put! channels/track-list [[track] 0])
                           (.preventDefault e))}
           track-number
           track-label
           [:div.right (utils/format-duration duration)]]])))))
