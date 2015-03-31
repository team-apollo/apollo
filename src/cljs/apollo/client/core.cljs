(ns apollo.client.core
  (:require-macros [secretary.core :refer [defroute]]
                   [cljs.core.async.macros :refer [go]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.services :as services]
            [apollo.client.utils :as utils]
            [apollo.client.pages :as pages]
            [apollo.client.debug :as debug]
            [apollo.client.views.admin :as admin]
            [apollo.client.state :refer [app-state ref-post-filter]]
            [apollo.client.player :refer [audio-elem view-now-playing]]
            [apollo.client.keyboard :as keyboard]
            [apollo.client.events :as e]
            [apollo.client.state :as state]
            [cljs.core.async :refer [<! sub chan dropping-buffer timeout]])

  (:import goog.History))

(def nav-map {:nav-browse "#/"
              :nav-recent "#/recent"
              :nav-config "#/admin"})

(keyboard/register-shortcut :nav-browse "g b")
(keyboard/register-shortcut :nav-recent "g r")
(keyboard/register-shortcut :nav-config "g c")

(def shortcut-chan (chan (dropping-buffer 1)))
(sub e/event-bus :shortcut shortcut-chan)

(go
  (loop []
    (let [id (:message (:message (<! shortcut-chan)))
          route (id nav-map)]
      (when (not (nil? route))
        (set! (.-hash js/window.location) route)
        (secretary/dispatch! route)))
    (recur)))

(defn loading-page [data]
  (reify
    om/IRender
    (render [this]
      (html [:div "loading..."]))))

(defn not-found [data]
  (reify
    om/IRender
    (render [this]
      (html [:div "not found"]))))

(defn show-page [data]
  (reify
    om/IRender
    (render [this]
      (let [page (or (:active-page data) loading-page)]
        (html [:span
               (om/build audio-elem data) ;; always visible
               (om/build page data)])))))

(om/root show-page app-state
         {:target (. js/document (getElementById "app"))})


(defn reset-post-filter []
  (om/transact!
   (state/ref-post-filter)
   (fn [p]
     (let [result (assoc p :value nil)]
       result))))

(defroute home-path "/" []
  (go
    (swap! app-state assoc :artists (<! (services/artist-list-prefix "all")))
    (swap! app-state assoc :active-page pages/view-browse)
    (swap! app-state assoc :active-nav "all")
    (swap! app-state assoc :active-artist nil)
    (swap! app-state assoc :albums nil)
    (swap! app-state assoc :active-album nil)))

(defroute nav-path "/nav/:prefix" [prefix]
  (go
    (swap! app-state assoc :artists (<! (services/artist-list-prefix prefix)))
    (swap! app-state assoc :active-page pages/view-browse)
    (swap! app-state assoc :active-nav prefix)
    (swap! app-state assoc :active-artist nil)
    (swap! app-state assoc :albums nil)
    (swap! app-state assoc :active-album nil)))

(defroute artist-path "/artists/:artist" [artist]
  (go
    (let [response (<! (services/artist-detail artist))
          ;; info-response (<! (services/artist-info artist))
          ]
      (swap! app-state assoc :active-artist (response "artist"))
      ;; (swap! app-state assoc :artist-info (info-response "info"))
      (swap! app-state assoc :active-page pages/view-browse)
      (swap! app-state assoc :albums (response "albums"))
      (swap! app-state assoc :active-album nil))))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (go
    (let [response (<! (services/album-detail artist album))]
      (swap! app-state assoc :active-artist artist)
      (swap! app-state assoc :active-page pages/view-browse)
      (swap! app-state assoc :active-album (response "album")))))

(defroute recent "/recent" []
  (go
    (let [response (<! (services/recently-added))]
      (swap! app-state assoc :albums (response "albums"))
      (swap! app-state assoc :active-artist nil)
      (swap! app-state assoc :active-album nil)
      (swap! app-state assoc :active-page pages/view-recently-added))))

(defroute by-year "/by-year" {}
  (go
    (let [response (<! (services/by-year))]
      (swap! app-state assoc :albums (response "albums"))
      (swap! app-state assoc :active-artist nil)
      (swap! app-state assoc :active-album nil)
      (swap! app-state assoc :active-page pages/view-by-year))))
(defroute admin-path "/admin" []
  (go
    (swap! app-state assoc :mounts (<! (services/mounts)))
    (swap! app-state assoc :active-page admin/view-admin)))

(defroute debug-path "/debug" []
  (swap! app-state assoc :active-page debug/view-debug))

(defroute "*" []
  (.log js/console "route not found")
  (swap! app-state assoc :active-page not-found))

(defn main []
  (secretary/set-config! :prefix "#")
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))
                     (swap! app-state assoc :current-token (.-token event))
                     (go
                       (<! (timeout 100))
                       (reset-post-filter))
                     true))
    (.setEnabled history true)))

(main)
