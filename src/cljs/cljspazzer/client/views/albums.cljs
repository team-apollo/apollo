(ns cljspazzer.client.views.albums
  (:require [cljspazzer.client.utils :as utils]
            [cljspazzer.client.channels :as channels]
            [cljspazzer.client.views.artists :as artists]
            [cljspazzer.client.views.tracks :as tracks]
            [cljspazzer.client.views.nav :as nav]
            [cljspazzer.client.channels :as channels]
            [cljs.core.async :refer [<! put! chan]]))

(defn mk-album-url [artist album]
  (utils/format "#/artists/%s/albums/%s"
                (utils/encode artist)
                (utils/encode (album "album_canonical"))))

(defn mk-album-image [artist album]
  (utils/format "/api/artists/%s/albums/%s/image"
                (utils/encode artist)
                (utils/encode (album "album_canonical"))))

(defn mk-album-zip-url [active-artist album]
  (utils/format "/api/artists/%s/albums/%s/zip"
                (utils/encode active-artist)
                (utils/encode (album "album_canonical"))))

(defn album-item [active-artist album]
  (let [album-url (mk-album-url active-artist album)
        album-image (mk-album-image active-artist album)
        album-label (utils/format "%s" (album "album_canonical"))
        album-zip-url (mk-album-zip-url active-artist album)]
    [:li
     [:a {:href album-url}
      [:img {:src album-image}]
      album-label]
     [:a.download {:href album-zip-url}
      [:i.fa.fa-download.fa-lg]]
     ]))

(defn album-list-partial [active-artist albums]
  (let [album-heading (utils/format "%s Albums" (count albums))
        render-album (partial album-item active-artist)
        nav-str (apply str nav/nav-seq)
        artist-first (first active-artist)
        back (if (utils/contains? nav-str artist-first)
               artist-first
               "#")
        back-link (utils/format "#/nav/%s" back)]
    [:div.album-list
     [:a {:href back-link} "browse"]
     [:h3 album-heading]
     [:ul (map render-album albums)]]))

(defn album-detail [artist album]
  (let [album-name (album "name")
        album-year (album "year")
        album-label (utils/format "%s - (%s)" album-name album-year)
        album-image (mk-album-image artist album)
        album-zip-url (mk-album-zip-url artist album)
        tracks (album "tracks")
        artist-url (artists/mk-artist-url artist)
        play-album (fn [e] (put! channels/track-list tracks))
        compilation? (album "compilation")]
    (if (and (not (nil? artist)) (not (nil? album)))
      [:div 
       [:a {:href artist-url} [:h1 artist]]
       [:h2 album-label]
       [:a.download {:href album-zip-url}
        [:i.fa.fa-download.fa-lg]]
       [:i.fa.fa-play-circle.fa-lg {:on-click play-album}]
       [:img {:src album-image}]
       [:ul.tracks
        (map (fn [track] (tracks/track-detail track compilation?)) tracks)]
       ]))
  )
