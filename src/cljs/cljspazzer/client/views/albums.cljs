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

(defn mk-album-zip-url [artist album]
  (utils/format "/api/artists/%s/albums/%s/zip"
                (utils/encode artist)
                (utils/encode (album "album_canonical"))))

(defn album-item [artist album]
  (let [album-url (mk-album-url artist album)
        album-image (mk-album-image artist album)
        album-label (utils/format "%s" (album "album"))
        album-zip-url (mk-album-zip-url artist album)]
    [:li
     [:a {:href album-url}
      [:img {:src album-image}]
      album-label]
     [:a.download {:href album-zip-url}
      [:i.fa.fa-download.fa-lg]]
     ]))

(defn album-list-partial [artist albums]
  (let [album-heading (utils/format "%s Albums" (count albums))
        render-album (partial album-item artist)
        nav-str (apply str nav/nav-seq)
        artist-first (first artist)
        back (if (utils/s-contains? nav-str artist-first)
               artist-first
               "#")
        back-link (utils/format "#/nav/%s" back)]
    [:div.album-list
     [:h3.left
      [:a {:href back-link} [:i.fa.fa-angle-left.fa-fw] "Back"]]
     [:h3.left album-heading]
     [:ul.clear (map render-album albums)]]))

(defn album-detail [artist album]
  (let [album-name (album "name")
        album-year (album "year")
        album-label (utils/format "%s" album-name)
        album-image (mk-album-image artist album)
        album-zip-url (mk-album-zip-url artist album)
        tracks (album "tracks")
        artist-url (artists/mk-artist-url artist)
        play-album (fn [e] (put! channels/track-list tracks))
        compilation? (album "compilation")]
    (if (and (not (nil? artist)) (not (nil? album)))
      [:div 
       [:h3
        [:a {:href artist-url} [:i.fa.fa-angle-left.fa-fw] "Back"]]
       [:div.info
        [:i.fa.fa-play-circle.fa-lg {:on-click play-album}]
        [:img {:src album-image}]
        [:h2 album-name]
        [:h3 album-year]
        [:a.download {:href album-zip-url}
          [:i.fa.fa-download.fa-fw] "Download Album"]]
       [:ul.tracks
        (map (fn [track] (tracks/track-detail track compilation?)) tracks)]
       ]))
  )
