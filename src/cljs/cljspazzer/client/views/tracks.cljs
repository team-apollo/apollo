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
         artist (t "artist_canonical")
         album (t "album_canonical")
         id (t "id")]
     (mk-track-url artist album id))))

(defn track-detail [track compilation?]
  (let [t (track "track")
        track-num (t "track")
        track-title (t "title")
        track-id (t "id")
        artist (t "artist_canonical")
        album (t "album_canonical")
        duration (t "duration")
        track-url (mk-track-url artist album track-id)
        track-label (if compilation?
                      (utils/format "%s. %s by %s" track-num track-title artist)
                      (utils/format "%s. %s" track-num track-title))]
    [:li
     [:a {:on-click (fn [e] (put! channels/track-list [track]))}
      track-label
      [:div.right (utils/format-duration duration)]]]))
