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
   (let [t (track "track")
         artist (t "artist")
         album (t "album")
         id (t "id")]
     (mk-track-url artist album id))))

(defn track-label [track with-artist?]
  (let [t (track "track")
        track-num (t "track")
        track-title (t "title")]
    (if with-artist?
      (utils/format "%s" track-title)
      (utils/format "%s. %s" track-num track-title))))

(defn artist [track with-artist?]
  (let [t (track "track")
        artist (t "artist")]
    (if with-artist?
      (utils/format "%s" artist))))

(defn track-detail [{:keys [track compilation?]} owner]
  (reify
    om/IRender
    (render [this]
      (let [t (track "track")
            duration (t "duration")
            track-label (track-label track compilation?)
            now-playing (or (first (om/observe owner (state/ref-now-playing))) {})
            np-t (or (now-playing "track") {"id" nil})
            is-active? (= (np-t "id") (t "id"))]
        (html
         [:li {:class-name (if is-active? "track-row active" "track-row")}
          [:a {:on-click (fn [e]
                           (put! channels/track-list [[track] 0])
                           (.preventDefault e))}
           track-label
           [:div.right (utils/format-duration duration)]]])))))
