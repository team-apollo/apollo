(ns cljspazzer.http.admin
  (:require [ring.util.response :refer [response]]
            [cljspazzer.db.schema :as db]
            [cljspazzer.scanner :refer [process-mounts!]]
            [clojure.java.io :as io]))

(defn mounts []
  (let [result (db/mount-points db/the-db)]
    (response {:mounts (map (fn [m] {:mount m}) result)})))

(defn add-new-mount [new-mount]
  (let [dir (io/file new-mount)]
    (if (and (.exists dir)
             (.isDirectory dir))
      (let [result-id (db/insert-mount! db/the-db (.getAbsolutePath dir))]
        (response {:mount {:id result-id}}))
      (throw (Exception. (format "%s is not a directory or doesn't exist" new-mount))))))


(defn delete-mount [the-mount]
  (let [dir (io/file the-mount)
        row-count (db/delete-mount! db/the-db (.getAbsolutePath dir))]
    (if (> row-count 0)
      (response "ok")
      (response "nope"))))

(defn do-scan []
  (process-mounts!)
  (response "ok"))
