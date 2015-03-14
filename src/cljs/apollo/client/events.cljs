(ns apollo.client.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! put! chan pub sub sliding-buffer close! timeout]]
            [apollo.client.utils :as utils]
            [goog.events :as events]))
 
(defn throttle [c ms]
  (let [c' (chan)]
    (go
      (while true
        (>! c' (<! c))
        (<! (timeout ms))))
    c'))

(def event-chan (chan (sliding-buffer 1)))
(def event-bus (pub event-chan (fn [e] (:topic e))))

(defn publish [chan topic message]
  (put! chan {:topic topic :message message}))

(events/listen js/window "scroll" (fn [e] (publish event-chan :scroll nil)))
(events/listen js/window "keyup" (fn [e] (publish event-chan :keypress {:event e}))) ;; probably redundant with keyboard.cljs

(def scroll-chan (chan (sliding-buffer 1)))
(sub event-bus :scroll scroll-chan)

(go-loop []
  (let [e (<! scroll-chan)]
    (when (utils/scroll-bottom?)
      (publish event-chan :at-bottom nil)))
  (recur))



