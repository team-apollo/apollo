(ns apollo.client.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.utils :as utils]
            [apollo.client.events :as events]
            [cljs.core.async :refer [<! put! chan unsub sub dropping-buffer]]))

(defn when-visible [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:scroll-chan (chan (dropping-buffer 1))
       :check-visible (fn []          
                         (let [is-visible (or (om/get-state owner :visible)
                                              (utils/in-view-port owner))]
                           (om/set-state! owner :visible is-visible)
                           (when is-visible (unsub events/event-bus :scroll (om/get-state owner :scroll-chan)))))})
    om/IWillMount
    (will-mount [_]
      (let [scroll-chan (om/get-state owner :scroll-chan)
            check (om/get-state owner :check-visible)]
        (sub events/event-bus :scroll scroll-chan)
        (go
          (loop []
            (let [e (<! scroll-chan)]
              (check))
            (recur)))))
    om/IDidMount
    (did-mount [_]
      ((om/get-state owner :check-visible)))
    om/IWillUnmount
    (will-unmount [_]
      (unsub events/event-bus :scroll (om/get-state owner :scroll-chan)))
    om/IRender
    (render [_]
      (html
       (if (not (om/get-state owner :visible))
         [:span]
         (let [component (:component data)
               component-data (:component-data data)]
           (om/build component component-data)))))))
