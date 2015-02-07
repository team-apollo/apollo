(ns cljspazzer.client.state
  (:require [om.core :as om :include-macros true]))

(def app-state (atom {:player {:now-playing {"track" {}}}
                      :a-value {}}))

(defn ref-player []
  (om/ref-cursor (:player (om/root-cursor app-state))))
