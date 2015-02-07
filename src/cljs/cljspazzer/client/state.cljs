(ns cljspazzer.client.state
  (:require [om.core :as om :include-macros true]))

(def app-state (atom {:now-playing []
                      :player {:current-playlist []}
                      :a-value {}}))

(defn ref-now-playing []
  (om/ref-cursor (:now-playing (om/root-cursor app-state))))


(defn ref-player []
  (om/ref-cursor (:player (om/root-cursor app-state))))
