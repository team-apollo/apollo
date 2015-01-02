(ns cljspazzer.core.client
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [goog.string.format])
  (:import goog.History))

(defn error-handler [response]
  (.log js/console "something bad happened " response))

(defroute home-path "/" []
  (.log js/console "home path")
  (ajax/GET "/api/artists" {:error-handler error-handler
                            :handler (fn [response]
                                       (.log js/console (str response)))}))

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
