(ns cljspazzer.client.views.tracks
  (:require [cljspazzer.client.utils :as utils]
            [cljspazzer.client.channels :as channels]
            [cljs.core.async :refer [<! put! chan]]))

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
        track-title (t "title")
        artist (t "artist")]
    (if with-artist?
      (utils/format "%s. %s by %s" track-num track-title artist)
      (utils/format "%s. %s" track-num track-title))))

  
(defn track-detail [track compilation?]
  (let [t (track "track")
        duration (t "duration")
        track-label (track-label track compilation?)]
    [:li
     [:a {:on-click (fn [e] (put! channels/track-list [track]))}
      track-label
      [:div.right (utils/format-duration duration)]]]))
