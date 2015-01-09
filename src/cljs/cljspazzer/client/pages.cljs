(ns cljspazzer.client.pages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]))

(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))

(defn nav-item [x]
  [:li [:a {:href (utils/format "#/nav/%s" (utils/encode x))} x]])

(defn artist-item [x]
  [:li [:a {:href (utils/format "#/artists/%s" (utils/encode x))} x]])

(defn album-item [x]
  [:li (utils/format "%s - (%s)" (x "album_canonical") (x "year"))])

(defn browse-page [data]
  (html [:div.browse
         [:div.pure-g
          [:div.collection-nav.pure-u-5-5 [:ul (map nav-item nav-seq)]]]
         [:div.content.pure-g
          [:div.artist-list.pure-u-1-5
           [:ul (map artist-item (:artists data))]]
          [:div.artist-detail.pure-u-3-5
           [:h1 (:active-artist data)]
           [:ul (map album-item (:albums data))]]]]))

(defn view-browse [data]
  (om/component (browse-page data)))
