(ns cljspazzer.client.pages
  (:require-macros [kioo.om :refer [defsnippet deftemplate]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [kioo.om :refer [content set-attr do-> substitute listen]]
            [kioo.core :refer [handle-wrapper]]
            [cljspazzer.client.utils :as utils]))

(defsnippet artist-item "templates/browser.html" [:.artist-item]
  [artist]
  {[:a] (do-> (content artist)
              (set-attr :href (utils/format "#/artists/%s" (utils/encode artist))))})

(defsnippet index-item "templates/browser.html" [:.artist-index]
  [idx]
  {[:a] (do-> (content idx)
              (set-attr :href (utils/format "#/nav/%s" idx)))})
(defsnippet track-item "templates/browser.html" [:.track-item]
  [track]
  {[:.name] (content "goo")}
  )
(defsnippet album-item "templates/browser.html" [:.album-item]
  [album]
  {[:.album-heading] (content (:name album))
   [:.track-list] (content (map track-item (:tracks album)))
   })

(defsnippet album-list "templates/browser.html" [:.album-list]
  [artist albums]
  {[:.album-item] (do-> (content map album-item albums))
   [:.artist-heading] (content artist)})

(deftemplate browse-page "templates/browser.html" [data]
  {[:.artist-list] (content (map artist-item (:artists data)))
   [:.artist-nav] (content (map index-item "abcdefghijklmnopqrstuvwxyz"))
   [:.album-list] (content (album-list (:active-artist data)
                                (:albums data)))})

(defn view-browse [data]
  (om/component (browse-page data)))
