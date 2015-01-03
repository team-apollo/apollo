(ns cljspazzer.client.core
  (:require-macros [secretary.core :refer [defroute]]
                   [kioo.om :refer [defsnippet deftemplate]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [kioo.om :refer [content set-attr do-> substitute listen]]
            [kioo.core :refer [handle-wrapper]]
            [cljspazzer.client.services :as services]
            [cljspazzer.client.utils :as utils])
  
  (:import goog.History))

(def app-state (atom {}))

(defsnippet artist-item "templates/artists.html" [:.artist-item]
  [artist]
  {[:a] (do-> (content artist)
              (set-attr :href (utils/format "#/artists/%s" (utils/encode artist))))})

(deftemplate artists-template "templates/artists.html"
  [artists]
  {[:.artist-list] (content (map artist-item artists))})

(defn view-artists [data]
  (om/component (artists-template (:artists data))))

(om/root view-artists app-state
         {:target (. js/document (getElementById "app"))})

(defroute home-path "/" []
  (.log js/console "home-path ")
  (services/artist-list (fn [result] (swap! app-state assoc :artists result))))

(defroute artist-path "/artists/:artist" [artist]
  (.log js/console "artist-path " artist)
  (services/artist-detail artist (fn [response] (.log js/console (clj->js response)))))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (.log js/console "album-path " artist " " album)
  (services/album-detail artist album (fn [response] (.log js/console clj->js response))))


(defroute "*" []
  (.log js/console "route not found"))

(defn main []
  (secretary/set-config! :prefix "#")
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(main)
