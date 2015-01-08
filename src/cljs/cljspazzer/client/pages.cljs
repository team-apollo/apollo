(ns cljspazzer.client.pages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]))
(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))

(defn nav-item [x]
  (html [:li
         [:a {:href (utils/format "#/nav/%s" x)} x]]))

(defn artist-item [x]
  (html [:li [:a {:href (utils/format "#/artists/%s" x)} x]]))

(defn album-item [x]
  (html [:li (utils/format "%s - (%s)" (x "album_canonical") (x "year"))]))

(defn browse-page [data]
  (html [:div.browse
         [:div.collection-nav [:ul (map nav-item nav-seq)]]
         [:div.artist-list [:ul (map artist-item (:artists data))]]
         [:div.artist-detail [:ul (map album-item (:albums data))]]]
        ))

(defn view-browse [data]
  (om/component (browse-page data)))
