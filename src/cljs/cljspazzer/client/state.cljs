(ns cljspazzer.client.state
  (:require [om.core :as om :include-macros true]))

(def app-state (atom {:now-playing []
                      :current-playlist []
                      :a-value {}}))

(defn ref-now-playing []
  (om/ref-cursor (:now-playing (om/root-cursor app-state))))

(defn ref-current-playlist []
  (om/ref-cursor (:current-playlist (om/root-cursor app-state))))
