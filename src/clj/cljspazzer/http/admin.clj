(ns cljspazzer.http.admin
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as db]))

(defn mounts []
  (let [result (db/mount-points db/the-db)]
    (response {:mounts (map (fn [m] {:mount m}) result)})))
