(ns cljspazzer.core.client
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [goog.events :as events])
  (:import goog.History))


(defroute home-path "/" []
  (.log js/console "home path"))

(defroute artist-path "/artists/:artist" [artist]
  (.log js/console "artist-path " artist))

(defroute album-path "/artists/:artist/albums/:album" [artist album]
  (.log js/console "album-path " artist " " album))

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

