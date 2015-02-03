(ns cljspazzer.client.debug
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn child-comp [data owner]
  (om/component
   (html [:div
          [:h2 "hello"]
          (om/get-state owner :x)])))

(defn view-debug [data owner]
  (om/component
   (html
    [:div
     [:h1 "hi"]
     (om/build child-comp data {:init-state {:x "xx"}})])))
