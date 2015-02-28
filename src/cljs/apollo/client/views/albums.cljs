(ns apollo.client.views.albums
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.utils :as utils]
            [apollo.client.channels :as channels]
            [apollo.client.views.artists :as artists]
            [apollo.client.views.tracks :as tracks]
            [apollo.client.views.nav :as nav]
            [apollo.client.channels :as channels]
            [apollo.client.services :as services]
            [cljs.core.async :refer [<! put! chan]]))

(defn mk-album-url [artist album]
  (utils/format "#/artists/%s/albums/%s"
                (utils/encode artist)
                (utils/encode album)))

(defn mk-album-image [artist album]
  (utils/format "/api/artists/%s/albums/%s/image"
                (utils/encode artist)
                (utils/encode album)))

(defn mk-album-zip-url [artist album]
  (utils/format "/api/artists/%s/albums/%s/zip"
                (utils/encode artist)
                (utils/encode album)))

(defn album-item [{:keys [artist-ctx album]}]
  (om/component
   (html
    (let [the-artist (or artist-ctx (album "artist"))
          artist (if (utils/s-contains? the-artist ",")
                   (first (.split the-artist ","))
                   the-artist)
          album-name (album "album_canonical")
          album-url (mk-album-url artist album-name)
          album-year (album "year")
          album-image (mk-album-image artist album-name)
          album-label (if (nil? artist-ctx)
                        (utils/format "%s by %s" (album "album") artist)
                        (utils/format "%s" (album "album")))
          album-zip-url (mk-album-zip-url artist album-name)
          play-album (fn [e]
                       (go
                         (let [album-detail (<! (services/album-detail artist (album "album")))
                               tracks ((album-detail "album") "tracks")]
                           (put! channels/track-list [tracks 0]))))]
      [:li.no-select
       [:a
        [:img.band {:src album-image}] album-label album-year]
       [:div.album-actions
        [:i.fa.fa-play-circle {:on-click play-album}]
        [:a [:i.fa.fa-plus-circle.fa-lg]]
        [:a {:href album-url} [:i.fa.fa-search.fa-lg]]
        [:a.download {:href album-zip-url}
         [:i.fa.fa-download.fa-lg]]]
       ]))))

(defn album-list-partial [{:keys [artist albums]}]
  (om/component
   (html
    (let [album-heading (utils/format "%s Albums" (count albums))
          artist-first (first artist)
          back (nav/get-up-nav artist-first)
          back-link (utils/format "#/nav/%s" back)
          album-item-args (map (fn [artist album] {:artist-ctx artist :album album}) (repeat artist) albums)]
      [:div.artist-detail
       [:div.album-list
        [:h3.left
         [:a {:href back-link} [:i.fa.fa-angle-left.fa-fw] "Back"]]
        [:h3 album-heading]
        [:ul.clear (om/build-all album-item album-item-args)]]]))))


(defn album-detail [{:keys [artist album]}]
  (om/component
   (html
    (let [album-name (album "name")
          album-year (album "year")
          album-label (utils/format "%s" album-name)
          album-image (mk-album-image artist (album "album_canonical"))
          album-zip-url (mk-album-zip-url artist album-name)
          tracks (album "tracks")
          artist-url (artists/mk-artist-url artist)
          play-album (fn [e] (put! channels/track-list [tracks 0]))
          compilation? (album "compilation")]
      (if (and (not (nil? artist)) (not (nil? album)))
        [:div.album-detail
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
          (map (fn [track] (tracks/track-detail track compilation?)) tracks)]]
        [:div])))))
