(ns cljspazzer.client.audio
  (:require [cljspazzer.client.channels :as channels]
            [cljs.core.async :refer [<! put! chan]]))

(def audio-node (js/Audio.))

(aset audio-node "onended" (fn [e] (put! channels/player-ctrl :next)))
(aset audio-node "ontimeupdate" (fn[e] (put! channels/stream-position :changed)))
