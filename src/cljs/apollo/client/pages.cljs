(ns apollo.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.utils :as utils]
            [apollo.client.services :as services]
            [apollo.client.channels :as channels]
            [apollo.client.views.nav :as nav]
            [apollo.client.views.artists :as artists]
            [apollo.client.views.albums :as albums]
            [apollo.client.player :as player]
            [apollo.client.state :as state]
            [cljs.core.async :refer [<! put! chan]]))

(defn left-column [data owner]
  (reify
    om/IRender
    (render [this]
      (let [active-artist (:active-artist data)
            sub-view (first (om/observe owner (state/ref-subview)))
            set-subview (fn [k] (om/transact! (state/ref-subview) (fn [p] [k])))]
        (html
         [:div.left-column
          (artists/artist-detail-partial active-artist)
          [:ul
           [:li {:on-click (fn [e]
                             (set-subview :now-playing))} "now playing"]
           [:li {:on-click (fn [e]
                             (set-subview :playlists))} "playlists"]]
          (if (= sub-view :now-playing)
            (om/build player/view-now-playing data)
            (om/build player/view-playlists data))])))))

(defn view-browse [data owner]
  (reify
    om/IRender
    (render [this]
      (let [active-artist (:active-artist data)
            active-nav (:active-nav data)
            artists (:artists data)
            artist-count (count artists)
            albums (:albums data)
            album-count (count albums)
            active-album (:active-album data)]
        (html
         [:div.browse
          (om/build nav/main-nav data)
          (om/build left-column data)
          [:div.middle-column.pure-g
           [:div.pure-u-1
            [:div.content
             (nav/nav-partial (or (first active-artist) active-nav))
             (cond
               (and (nil? active-artist) (nil? active-album))
               (artists/artist-list-partial artists)
               (and (not (nil? active-artist)) (nil? active-album))
               (albums/album-list-partial active-artist albums)
               (not (nil? active-album))
               (albums/album-detail active-artist active-album))]]]])))))
