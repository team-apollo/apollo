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
    (swap! app-state assoc :artists (<! (services/artist-list-prefix "all")))
    (swap! app-state assoc :active-page pages/view-browse)
    (swap! app-state assoc :active-nav "all")
    (swap! app-state assoc :active-artist nil)
    (swap! app-state assoc :albums nil)))

(defroute nav-path "/nav/:prefix" [prefix]
  (go
    (swap! app-state assoc :artists (<! (services/artist-list-prefix prefix)))
    (swap! app-state assoc :active-page pages/view-browse)
    (swap! app-state assoc :active-nav prefix)
    (swap! app-state assoc :active-artist nil)
    (swap! app-state assoc :albums nil)))

(defroute artist-path "/artists/:artist" [artist]
  (go
    (if (nil? (:artists @app-state))
      (swap! app-state assoc :artists (<! (services/artist-list-prefix (:active-nav @app-state "all")))))
    (let [response (<! (services/artist-detail artist))]
      (swap! app-state assoc :active-artist (response "artist"))
      (swap! app-state assoc :active-page pages/view-browse)
      (swap! app-state assoc :albums (response "albums")))))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (go
    (if (nil? (:artists @app-state))
      (swap! app-state assoc :artists (<! (services/artist-list-prefix (:active-nav @app-state "all")))))
    (let [response (<! (services/album-detail artist album))]
      (swap! app-state assoc :active-artist artist)
      (swap! app-state assoc :active-page pages/view-browse)
      (swap! app-state assoc :active-album (response "album")))))

(defroute admin-path "/admin" []
  (go
    (swap! app-state assoc :mounts (<! (services/mounts)))
    (swap! app-state assoc :active-page pages/view-admin)))

(defroute debug-path "/debug" []
  (swap! app-state assoc :debug "hi there")
  (swap! app-state assoc :active-page pages/view-debug))

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
