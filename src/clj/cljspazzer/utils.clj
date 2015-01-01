(ns cljspazzer.utils
  (:require [clojure.string :as s]))

(defn canonicalize [s]
  (let [result (s/trim (s/lower-case s))]
    (if (and (> (count s) 4)
         (= "the " (subs result 0 4)))
      (s/trim (subs result 4))
      result)))
