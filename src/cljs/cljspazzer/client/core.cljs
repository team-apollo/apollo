(ns cljspazzer.client.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljspazzer.client.services :as services]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.pages :as pages])
  
  (:import goog.History))

(def app-state (atom {}))

(defn loading-page [data]
  (reify
    om/IRender
    (render [this]
      (dom/div nil "loading..."))))


(defn show-page [data]
  (reify
    om/IRender
    (render [this]
      (let [page (or (:active-page data) loading-page)]
        (om/build page data)))))


(om/root show-page app-state
         {:target (. js/document (getElementById "app"))})


(defroute home-path "/" []
  (.log js/console "home-path ")
  (services/artist-list (fn [result]
                          (swap! app-state assoc :artists result)
                          (swap! app-state assoc :active-page pages/view-artists))))

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
