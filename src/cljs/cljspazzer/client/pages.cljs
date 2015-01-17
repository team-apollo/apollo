(ns cljspazzer.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.services :as services]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary]))

(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))

(defn nav-item [x]
  [:li [:a {:href (utils/format "#/nav/%s" (utils/encode x))} x]])

(defn artist-item [x]
  [:li
   [:a {:href (utils/format "#/artists/%s" (utils/encode x))}
    [:img {:src "http://placehold.it/250x250.png"}] x]])


(defn album-item [active-artist album]
  [:li
   [:a {:href (utils/format "#/artists/%s/albums/%s"
                            (utils/encode active-artist)
                            (utils/encode (album "album_canonical")))}
    [:img {:src (utils/format "/api/artists/%s/albums/%s/image"
                              (utils/encode active-artist)
                              (utils/encode (album "album_canonical")))}]
    (utils/format "%s" (album "album_canonical"))]
   [:a.download {:href (utils/format "/api/artists/%s/albums/%s/zip"
                                     (utils/encode active-artist)
                                     (utils/encode (album "album_canonical")))}
    [:i.fa.fa-download.fa-lg]]
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
     [:img {:src
            (utils/format "/api/artists/%s/albums/%s/image"
                          (utils/encode artist)
                          (utils/encode (album "name")))}]
     [:ul.tracks
      (map track-detail (album "tracks"))]
     ])
  )

(defn browse-page [data]
  (let [active-artist (:active-artist data)
        artists (:artists data)
        artist-count (count artists)
        albums (:albums data)
        album-count (count albums)
        active-album (:active-album data)]
    (html
     [:div.browse
      [:div.pure-g
       [:div.collection-nav
        [:h3 "Artists"]
        [:ul (map nav-item nav-seq)]]]
      [:div.content.pure-g
       [:div.artist-list.pure-u-1
        [:h3 (utils/format "%s Artists" artist-count)]
        [:ul (map artist-item artists)]]
       [:div.artist-detail
        [:div.pure-u-4-5
         [:h3 (utils/format "%s Albums" album-count)]
         [:ul (map (partial album-item active-artist) albums)]]
        [:div.pure-u-1-5
         [:h3 active-artist]
         [:img {:src (utils/format "/api/artists/%s/image" active-artist)}]]]
       [:div.pure-u-1
        [:div.album-detail
         (album-detail active-artist active-album)]]]])))

(defn nav-partial []
  [:div.collection-nav
   [:h3 "Artists"]
   [:ul (map nav-item nav-seq)]])

(defn artist-list-partial [artists]
  (let [artist-heading (utils/format "%s Artists" (count artists))]
    [:div.artist-list
     [:h3 artist-heading]
     [:ul (map artist-item artists)]]))

(defn album-list-partial [active-artist albums]
  (let [album-heading (utils/format "%s Albums" (count albums))
        render-album (partial album-item active-artist)]
    [:div.album-list
     [:h3 album-heading]
     [:ul (map  render-album albums)]]))

(defn artist-detail-partial [active-artist]
  (let [artist-image (utils/format "/api/artists/%s/image" active-artist)]
    [:div
     [:h3 active-artist]
     [:img {:src artist-image}]]))

(defn browse-page-new [data]
  (let [active-artist (:active-artist data)
        artists (:artists data)
        artist-count (count artists)
        albums (:albums data)
        album-count (count albums)
        active-album (:active-album data)]
    (html
     [:div.browse
      (nav-partial)
      [:div.content.pure-g
       (cond
         (and (nil? active-artist) (nil? active-album))
         [:div.pure-u-1
          (artist-list-partial artists)]
         (and (not (nil? active-artist)) (nil? active-album))
         [:div.pure-u-1
          [:div.pure-g.artist-detail
           [:div.pure-u-4-5
            (album-list-partial active-artist albums)]
           [:div.pure-u-1-5
            (artist-detail-partial active-artist)]]]
         (not (nil? active-album))
         [:div.pure-u-1
          [:div.album-detail
           (album-detail active-artist active-album)]])]])))

(defn delete-mount [mount]
  (fn [e]
    (go
      (<! (services/delete-mount mount))
      (secretary/dispatch! "#/admin"))))

(defn mount-item [m]
  (let [path (m "mount")]
    [:li path [:a {
                   :on-click (fn [e] (delete-mount path) false)}
               "delete"]]))

(defn add-mount [owner]
  (let [v (.-value (om/get-node owner "new-mount"))]
    (go
      (<! (services/add-mount v))
      (secretary/dispatch! "#/admin"))))

(defn view-admin [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:mount-input-value nil})
    om/IRender
    (render [this]
      (let [mounts ((:mounts data) "mounts")]
        (html [:div.admin
               [:div.pure-g
                [:div.content.pure-u-1
                 [:ul (map mount-item mounts)]]
                [:div.new-mount.pure-u-1
                 [:input {:type "text"
                          :ref "new-mount"}]
                 [:a.button
                  {:href "#"
                   :on-click (fn [e]
                               (add-mount owner)
                               false)}
                  "create"]]]])))))

(defn view-browse [data]
  (om/component (browse-page-new data)))

(defn view-debug [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:debug (:debug data)})
    om/IRender
    (render [this]
      (html [:h1 (om/get-state owner :debug)]))))
