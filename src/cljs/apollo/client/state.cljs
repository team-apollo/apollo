(ns apollo.client.state
  (:require [om.core :as om :include-macros true]))

(def app-state (atom {:now-playing []
                      :player {:current-playlist []}
                      :sub-view-mode [:now-playing]}))

(defn ref-now-playing []
  (om/ref-cursor (:now-playing (om/root-cursor app-state))))


(defn ref-player []
  (om/ref-cursor (:player (om/root-cursor app-state))))

(defn ref-subview []
  (om/ref-cursor (:sub-view-mode (om/root-cursor app-state))))
