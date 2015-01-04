(ns cljspazzer.client.core
  (:require-macros [secretary.core :refer [defroute]]
                   [cljs.core.async.macros :refer [go]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljspazzer.client.services :as services]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.pages :as pages]
            [cljs.core.async :refer [<!]])
  
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
  (go
    (.log js/console "home-path ")
    (swap! app-state assoc :artists (<! (services/artist-list)))
    (swap! app-state assoc :active-page pages/view-browse)))


(defroute artist-path "/artists/:artist" [artist]
  (go
    (.log js/console "artist-path " artist)
    (.log js/console (clj->js (<! (services/artist-detail artist))))))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (go
    (.log js/console "album-path " artist " " album)
    (.log js/console (clj->js (<! (services/album-detail artist album))))))


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
