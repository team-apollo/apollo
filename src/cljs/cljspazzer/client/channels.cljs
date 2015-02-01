(ns cljspazzer.client.channels
 (:require [cljs.core.async :refer [chan]]))

(def track-list (chan))
(def player-ctrl (chan))
(def stream-position (chan))
(def now-playing (chan))
