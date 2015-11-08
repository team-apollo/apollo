(ns apollo.http.admin
  (:require [ring.util.response :refer [response]]
            [apollo.db.schema :as db]
            [apollo.scanner :refer [process-mounts!]]
            [clojure.java.io :as io]))

(defn mounts [{cn :db-connection}]
  (let [result (db/mount-points cn)]
    (response {:mounts (map (fn [m] {:mount m}) result)})))

(defn add-new-mount [{cn :db-connection
                      {:keys [new-mount]} :params}]
  (let [dir (io/file new-mount)]
    (if (and (.exists dir)
             (.isDirectory dir))
      (let [result-id (db/insert-mount! (.getAbsolutePath dir))]
        (response {:mount {:id result-id}}))
      (throw (Exception. (format "%s is not a directory or doesn't exist" new-mount))))))


(defn delete-mount [{cn :db-connection
                     {:keys [the-mount]} :params}]
  (let [dir (io/file the-mount)
        row-count (db/delete-mount! (.getAbsolutePath dir))]
    (if (> row-count 0)
      (response "ok")
      (response "nope"))))

(defn do-scan [{cn :db-connection}]
  (process-mounts! cn)
  (response "ok"))
