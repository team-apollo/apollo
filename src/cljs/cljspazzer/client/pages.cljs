(ns cljspazzer.client.pages
  (:require-macros [kioo.om :refer [defsnippet deftemplate]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [kioo.om :refer [content set-attr do-> substitute listen]]
            [kioo.core :refer [handle-wrapper]]
            [cljspazzer.client.utils :as utils]))

(defsnippet artist-item "templates/artists.html" [:.artist-item]
  [artist]
  {[:a] (do-> (content artist)
              (set-attr :href (utils/format "#/artists/%s" (utils/encode artist))))})

(deftemplate artists-template "templates/artists.html"
  [artists]
  {[:.artist-list] (content (map artist-item artists))})

(defn view-artists [data]
  (om/component (artists-template (:artists data))))
