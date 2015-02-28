(ns apollo.client.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! put! chan pub sub dropping-buffer]]))

(def event-chan (chan (dropping-buffer 1)))
(def event-bus (pub event-chan (fn [e] (:topic e))))


(defn publish [chan topic message]
  (put! chan {:topic topic :message message}))


(.addEventListener js/window "scroll" (fn [e]
                                        (publish event-chan :scroll nil)))
