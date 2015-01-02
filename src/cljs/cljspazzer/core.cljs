(ns cljspazzer.core.client
  (:require-macros [secretary.core :refer [defroute]]
                   [kioo.om :refer [defsnippet deftemplate]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [goog.string.format]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [kioo.om :refer [content set-attr do-> substitute listen]]
            [kioo.core :refer [handle-wrapper]])
  
  (:import goog.History))

(defn error-handler [response]
  (.log js/console "something bad happened " response))

(def app-state (atom {}))

(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)))))

(defn view-artist-list [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil
             (map (fn [e] (dom/li nil e)) (:artists data))))))

(defsnippet artist-item "templates/artists.html" [:li]
  [artist]
  {[:a] (do-> (content artist)
              (set-attr :href (gstring/format "#/artists/%s" artist)))})

(deftemplate artists-template "templates/artists.html"
  [artists]
  {[:.artist-list] (content (map artist-item artists))})

(defn view-artists [data] (om/component (artists-template (:artists data))))

(om/root view-artists app-state
         {:target (. js/document (getElementById "app"))})

(defroute home-path "/" []
  (.log js/console "home path")
  (ajax/GET "/api/artists" {:error-handler error-handler
                            :handler (fn [response]
                                       (let [not-blank? (fn [x] (not (= x "")))
                                             artists (filter  not-blank? (map (fn [a] (a "artist")) (response "artists")))]
                                         (swap! app-state assoc :artists artists)))}))

(defroute artist-path "/artists/:artist" [artist]
  (.log js/console "artist-path " artist)
  (ajax/GET (gstring/format "/api/artists/%s" artist)
            {:error-handler error-handler
             :handler (fn [response]
                        (.log js/console (str response)))}))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (.log js/console "album-path " artist " " album)
  (ajax/GET (gstring/format "/api/artists/%s/albums/%s" artist album)
            {:error-handler error-handler
             :handler (fn [response]
                        (.log js/console (str response)))}))

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
