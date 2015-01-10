(ns cljspazzer.client.pages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]))

(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))

(defn nav-item [x]
  [:li [:a {:href (utils/format "#/nav/%s" (utils/encode x))} x]])

(defn artist-item [x]
  [:li [:a {:href (utils/format "#/artists/%s" (utils/encode x))} x]])

(defn album-item [active-artist album]
  [:li
   [:a {:href (utils/format "#/artists/%s/albums/%s"
                            (utils/encode active-artist)
                            (utils/encode (album "album_canonical")))}
    (utils/format "%s - (%s)" (album "album_canonical") (album "year"))]
   [:a {:href (utils/format "/api/artists/%s/albums/%s/zip"
                            (utils/encode active-artist)
                            (utils/encode (album "album_canonical")))} "download(zip)"]
   ])

(defn track-detail [track]
  (let [t (track "track")
        artist (t "artist_canonical")
        album (t "album_canonical")]
    [:li
     [:a {:href (utils/format "/api/artists/%s/albums/%s/tracks/%s"
                              (utils/encode artist)
                              (utils/encode album)
                              (utils/encode (t "id")))}
      (utils/format "%s. %s" (t "track") (t "title"))]]))

(defn album-detail [artist album]
  (if (and (not (nil? artist)) (not (nil? album)))
   [:div 
   [:h1 artist]
   [:h2 (utils/format "%s - (%s)" (album "name") (album "year"))]
   [:ul.tracks
    (map track-detail (album "tracks"))]
   ])
  )

(defn browse-page [data]
  (let [active-artist (:active-artist data)]
    (html [:div.browse
           [:div.pure-g
            [:div.collection-nav.pure-u-1 [:ul (map nav-item nav-seq)]]]
           [:div.content.pure-g
            [:div.artist-list.pure-u-1-5
             [:ul (map artist-item (:artists data))]]
            [:div.artist-detail.pure-u-2-5
             [:h1 active-artist]
             [:ul (map (partial album-item active-artist) (:albums data))]]
            [:div.album-detail.pure-u-2-5
             (album-detail active-artist (:active-album data))]]])))

(defn view-browse [data]
  (om/component (browse-page data)))
